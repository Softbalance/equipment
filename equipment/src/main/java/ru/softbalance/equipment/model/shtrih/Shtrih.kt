package ru.softbalance.equipment.model.shtrih

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import libcore.io.Libcore
import ru.shtrih_m.fr_drv_ng.classic_interface.Classic
import ru.shtrih_m.fr_drv_ng.classic_interface.ClassicImpl
import ru.softbalance.equipment.LINE_SEPARATOR
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.*
import ru.softbalance.equipment.model.atol.Atol
import ru.softbalance.equipment.model.exception.ExecuteException
import ru.softbalance.equipment.model.mapping.jackson.mapper
import ru.softbalance.equipment.wrap
import rx.Completable
import rx.Single
import rx.schedulers.Schedulers
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class Shtrih(
    private val context: Context,
    private val settings: Settings,
    private val classic: Classic) : EcrDriver {

    companion object {
        private const val CONNECTION_TIME_OUT_MILLIS = 2000L
        private const val DEFAULT_LINE_LENGTH = 31

        private const val PRINT_STRING_TRAIT = "trait"
        private const val PRINT_STRING_DASH = "dash"

        private const val TRAIT_TEMPLATE = "============================="
        private const val DASH_TEMPLATE = "------------------------------"

        fun init(context: Context, settings: String): EcrDriver? {
            return init(context, extractSettings(settings))
        }

        fun init(context: Context, settings: Settings): EcrDriver? {
            try {
                Libcore.os.setenv("FR_DRV_DEBUG_CONSOLE", "1", true)//выводим лог в logcat, а не в файл.
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return Shtrih(context.applicationContext, settings, ClassicImpl())
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

    override fun execute(tasks: List<Task>, finishAfterExecute: Boolean): Single<EquipmentResponse> {
        return prepare()
            .subscribeOn(Schedulers.io())
            .timeout(CONNECTION_TIME_OUT_MILLIS, TimeUnit.MILLISECONDS)
            .andThen(executeTasks(tasks))
            .andThen(Completable.fromCallable {
                if (finishAfterExecute) finish()
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
        for (task in tasks) {
            try {
                executeTask(task)
            } catch (e: Exception) {
                throw ExecuteException(
                    "Failed to execute task ${task.type}. ${buildMessageByException(e)}")
            }
        }
    }

    private fun prepare() = Completable.fromCallable {
        val url = "tcp://${settings.host}:${settings.port}?timeout=2000&protocol=v1"
        classic.Set_ConnectionURI(url)
        classic.Connect().checkOrThrow()
        classic.Set_Password(30)
    }

    private fun executeTask(task: Task) {
        val lineLength = getLineLength()
        when (task.type.toLowerCase()) {
            TaskType.STRING -> printString(task, lineLength)
            TaskType.CUT -> cut()
            TaskType.PRINT_FOOTER, TaskType.PRINT_HEADER -> finishDocument()
            else -> {
                Log.e(
                    Atol::class.java.simpleName,
                    context.getString(R.string.equipment_lib_operation_not_supported, task.type))
            }
        }
    }

    private fun getLineLength(): Int {
        classic.Set_FontType(1)
        classic.GetFontMetrics()
        val charWidth = classic.Get_CharWidth()
        return if (charWidth > 0) {
            classic.Get_PrintWidth() / classic.Get_CharWidth()
        } else {
            DEFAULT_LINE_LENGTH
        }
    }

    private fun cut() {
        classic.Set_CutType(false) // true for partial cut
        classic.CutCheck()
    }

    private fun finishDocument() {
        classic.FinishDocument()
    }

    private fun printString(task: Task, lineLength: Int) {
        val data = task.data.trim()

        if (data.contains(PRINT_STRING_TRAIT, true)) {
            task.data = TRAIT_TEMPLATE
            task.param.alignment = Alignment.CENTER
        } else if (data.contains(PRINT_STRING_DASH, true)) {
            task.data = DASH_TEMPLATE
            task.param.alignment = Alignment.CENTER
        }

        printStringInternal(task, lineLength)
    }

    private fun printStringInternal(task: Task, lineLength: Int) {
        wrapText(task, lineLength)
            .map { text -> applyAlignment(text, task.param.alignment, lineLength) }
            .all {
                classic.Set_StringForPrinting(it)
                classic.PrintString().check()
            }
    }

    @SuppressLint("SwitchIntDef")
    protected fun wrapText(command: Task, lineLength: Int): Array<String> {
        val result: String

        when (command.param.wrap) {
            true -> result = wrap(command.data, lineLength, null, false)
            else -> result = command.data
        }

        return result
            .split(LINE_SEPARATOR.toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
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
        classic.Disconnect()
    }

    protected fun applyAlignment(text: String,
                                 @Alignment alignment: String?,
                                 lineLength: Int): String {
        val textLength = text.length

        if (textLength > lineLength) return text

        return when (alignment) {
            Alignment.CENTER -> text.padStart(textLength + (lineLength - textLength) / 2)
            Alignment.RIGHT -> text.padStart(lineLength)
            else -> text
        }
    }

    private fun Int.check() = this == 0

    private fun Int.checkOrThrow() {
        if (!this.check()) throw RuntimeException(getResultStatus())
    }

    private fun getResultStatus() =
        "${classic.Get_ResultCode()}: ${classic.Get_ResultCodeDescription()}"
}