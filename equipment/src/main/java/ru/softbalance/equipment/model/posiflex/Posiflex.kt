package ru.softbalance.equipment.model.posiflex

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.*
import ru.softbalance.equipment.model.atol.Atol
import ru.softbalance.equipment.model.exception.ExecuteException
import ru.softbalance.equipment.model.mapping.jackson.mapper
import ru.softbalance.equipment.model.posiflex.ports.Port
import ru.softbalance.equipment.model.posiflex.ports.TcpPort
import ru.softbalance.equipment.model.posiflex.ports.UsbPort
import rx.Completable
import rx.Single
import rx.schedulers.Schedulers
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class Posiflex(
    private val context: Context,
    private val port: Port,
    private val settings: Settings) : EcrDriver {

    companion object {
        private const val CONNECTION_TIME_OUT_MILLIS = 3000L

        const val POSIFLEX_VENDOR_ID = 0x0d3a
        const val ATOL_VENDOR_ID = 4070

        private const val CODE_PAGE_1251_ATOL = 6
        private const val CODE_PAGE_1251_POSIFLEX = 28

        private const val PRINT_STRING_TRAIT = "trait"
        private const val PRINT_STRING_DASH = "dash"

        private const val TRAIT_TEMPLATE = "============================="
        private const val DASH_TEMPLATE = "------------------------------"

        val VENDORS = arrayOf(POSIFLEX_VENDOR_ID, ATOL_VENDOR_ID)

        fun init(context: Context, settings: String): EcrDriver? {
            return init(context, extractSettings(settings))
        }

        fun init(context: Context, settings: Settings): EcrDriver? {
            val port = when (settings.connectionType) {
                DeviceConnectionType.NETWORK -> TcpPort(settings.host, settings.port)
                DeviceConnectionType.USB -> {
                    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                    val usbDevice = usbManager.deviceList.values
                        .firstOrNull { it.vendorId in VENDORS && it.productId == settings.productId }
                            ?: return null

                    settings.codePage = when (usbDevice.vendorId) {
                        ATOL_VENDOR_ID -> CODE_PAGE_1251_ATOL
                        else -> CODE_PAGE_1251_POSIFLEX
                    }

                    UsbPort(context, usbDevice)
                }
            }

            return Posiflex(context.applicationContext, port, settings)
        }

        fun extractSettings(settings: String): Settings {
            return try {
                mapper.readValue(settings, Settings::class.java)
            } catch (e: Exception) {
                Settings()
            }
        }

        fun packSettings(settings: Settings): String {
            return mapper.writeValueAsString(settings)
        }
    }

    private var isInitialized = false

    private val beginCommand = createByteArray(
        0x1B, 0x74, settings.codePage, // Установка кодировки принтера
        0x1B, 0x52, 0x00  // Установка набора символов (USA)
    )

    private val endCommand = createByteArray(
        0x1D,
        0x61,
        0x00 // Отключение автоматической отправки статуса (по документации - также чистит буффер принтера)
    )

    override fun execute(
        tasks: List<Task>,
        finishAfterExecute: Boolean): Single<EquipmentResponse> {
        return prepare()
            .subscribeOn(Schedulers.io())
            .timeout(CONNECTION_TIME_OUT_MILLIS, TimeUnit.MILLISECONDS)
            .andThen(executeTasks(tasks))
            .andThen(Completable.fromCallable {
                if (finishAfterExecute) {
                    finish()
                }
            })
            .toSingle {
                val response = EquipmentResponse()
                response.resultCode = ResponseCode.SUCCESS
                response
            }
            .onErrorReturn { exception ->
                val response = EquipmentResponse()
                response.resultCode = ResponseCode.HANDLING_ERROR
                response.resultInfo = buildMessageByException(exception)
                response
            }
    }

    private fun executeTasks(tasks: List<Task>) = Completable.fromCallable {
        port.write(beginCommand)
        for (task in tasks) {
            try {
                executeTask(task)
            } catch (e: Exception) {
                throw ExecuteException(
                    "Failed to execute task ${task.type}. ${buildMessageByException(
                        e)}")
            }
        }
        port.write(endCommand)
    }

    private fun prepare() = Completable.fromCallable {
        if (!isInitialized) {
            port.open()
        }
    }

    private fun executeTask(task: Task) {
        when (task.type.toLowerCase()) {
            TaskType.STRING -> printString(task)
            TaskType.CUT -> cut()
            TaskType.PRINT_FOOTER, TaskType.PRINT_HEADER -> printOffset()
            else -> {
                Log.e(
                    Atol::class.java.simpleName,
                    context.getString(R.string.equipment_lib_operation_not_supported, task.type))
            }
        }
    }

    private fun printOffset() {
        (1..settings.offsetHeaderBottom)
            .forEach {
                val offsetTask = Task()
                offsetTask.data = " "
                printStringInternal(offsetTask)
            }
    }

    private fun printString(task: Task) {
        val data = task.data.trim()

        if (data.contains(PRINT_STRING_TRAIT, true)) {
            task.data = TRAIT_TEMPLATE
            task.param.alignment = Alignment.CENTER
        } else if (data.contains(PRINT_STRING_DASH, true)) {
            task.data = DASH_TEMPLATE
            task.param.alignment = Alignment.CENTER
        }

        printStringInternal(task)
    }

    private fun cut() {
        writeCommands(createByteArray(0x1D, 0x56, 0x01), 200)
    }

    private fun printStringInternal(task: Task) {
        var font = 0

        if (task.param.bold == true) {
            font = font or 0x08
        }

        if (task.param.doubleHeight == true) {
            font = font or 0x20
        }

        if (task.param.underline == true) {
            font = font or 0x80
        }

        val config = createByteArray(
            0x1B,
            0x21,
            font, // Быстрая настройка шрифта (работает для 2 основных шрифтов, которые в коде идут как FONT_SMALL и FONT_NORMAL)
            0x1B,
            0x61,
            convertAlign(task.param.alignment ?: Alignment.LEFT)
        )

        // На деле такой кодировки в системе может не оказаться (встречал такие образы Android),
        // так что возможно нужно написать свой перекодировщик
        val textBytes = task.data.toByteArray(Charset.forName("cp1251"))

        // В коде ДТО это значится как "ultra-mega heuriscs: assumes that average printer will print 36 chars
        // in a row"... Пока что ни разу не подводило.
        val timeOut = 20 * (1 + task.data.length / 12)

        writeCommands(config + textBytes + byteArrayOf(0x0A), timeOut.toLong())
    }

    private fun writeCommands(commands: ByteArray, timeout: Long) {
        port.write(commands)
        try {
            Thread.sleep(timeout)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun buildMessageByException(e: Throwable): String {
        return if (e is UnknownHostException || e is ConnectException || e is NoRouteToHostException)
            context.getString(R.string.equipment_error_host_connection_failure)
        else if (e is SocketTimeoutException || e is TimeoutException) context.getString(R.string.equipment_error_time_out)
        else if (e is IOException) context.getString(
            R.string.equipment_error_io_exception,
            e.toString())
        else if (e is ExecuteException) {
            e.message ?: e.toString()
        } else {
            e.cause?.let { buildMessageByException(it) } ?: e.toString()
        }
    }

    override fun getSerial(finishAfterExecute: Boolean): Single<SerialResponse> {
        return Single.just(SerialResponse())
    }

    override fun getSessionState(finishAfterExecute: Boolean): Single<SessionStateResponse> {
        val accessException =
            IllegalAccessException(context.getString(R.string.equipment_error_method_not_supported))
        return Single.error<SessionStateResponse>(accessException)
    }

    override fun openShift(finishAfterExecute: Boolean): Single<OpenShiftResponse> {
        val accessException =
            IllegalAccessException(context.getString(R.string.equipment_error_method_not_supported))
        return Single.error<OpenShiftResponse>(accessException)
    }

    override fun getOfdStatus(finishAfterExecute: Boolean): Single<OfdStatusResponse> {
        val accessException =
            IllegalAccessException(context.getString(R.string.equipment_error_method_not_supported))
        return Single.error<OfdStatusResponse>(accessException)
    }

    override fun finish() {
        isInitialized = false
        ignoreException { port.close() }
    }

    private fun ignoreException(function: () -> Unit) {
        try {
            function.invoke()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun createByteArray(vararg bytes: Int): ByteArray {
        val res = ByteArray(bytes.size)

        for (i in res.indices)
            res[i] = (bytes[i] and 0xFF).toByte()

        return res
    }

    private fun convertAlign(@Alignment alignment: String): Int {
        return when (alignment) {
            Alignment.CENTER -> 0x01
            Alignment.RIGHT -> 0x02
            else -> 0x00
        }
    }
}