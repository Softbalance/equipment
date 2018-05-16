package ru.softbalance.equipment.model.shtrih

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import libcore.io.Libcore
import ru.shtrih_m.fr_drv_ng.classic_interface.Classic
import ru.shtrih_m.fr_drv_ng.classic_interface.ClassicImpl
import ru.softbalance.equipment.LINE_SEPARATOR
import ru.softbalance.equipment.ONE_HUNDRED
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
import java.math.BigDecimal
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
        const val ECR_MODE_SESSION_EXPIRED = 3

        const val CHECK_TYPE_SELL = 0
        const val CHECK_TYPE_ADD = 1
        const val CHECK_TYPE_RETURN = 2

        private const val CONNECTION_TIME_OUT_MILLIS = 5000L
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
            .doOnUnsubscribe { if (finishAfterExecute) finish() }
            .toSingle {
                val response = EquipmentResponse()
                response.resultCode = ResponseCode.SUCCESS
                response
            }
            .onErrorReturn { exception ->
                if (finishAfterExecute) finish()
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
        val url = "tcp://${settings.host}:${settings.port}?timeout=$CONNECTION_TIME_OUT_MILLIS&protocol=v1"
        classic.Set_ConnectionURI(url)
        classic.Connect().checkOrThrow()
        classic.Set_Password(30)
    }

    private fun executeTask(task: Task) {
        val lineLength = getLineLength()
        when (task.type.toLowerCase()) {
            TaskType.STRING -> printString(task, lineLength)
            TaskType.REGISTRATION -> registration(task)
            TaskType.CLOSE_CHECK -> closeCheck()
            TaskType.CANCEL_CHECK -> cancelCheck()
            TaskType.OPEN_CHECK_SELL -> openCheckSell(task)
            TaskType.PAYMENT -> setSum(task)
            TaskType.OPEN_CHECK_RETURN -> openCheckReturn(task)
            TaskType.RETURN -> setSum(task)
            TaskType.CASH_INCOME -> cashOperation(task, { classic.CashIncome() })
            TaskType.CASH_OUTCOME -> cashOperation(task, { classic.CashOutcome() })
            TaskType.CLIENT_CONTACT -> setClientContact(task)
            TaskType.REPORT -> report(task)
            TaskType.CUT -> cut()
            TaskType.PRINT_FOOTER, TaskType.PRINT_HEADER -> finishDocument()
            else -> {
                Log.e(
                    Atol::class.java.simpleName,
                    context.getString(R.string.equipment_lib_operation_not_supported, task.type))
            }
        }
    }

    private fun setSum(task: Task) {
        val totalCompat = task.param.sum?.toShtrihLong() ?: 0L
        when (task.param.typeClose) {
            2 -> classic.Set_Summ2(totalCompat)
            3 -> classic.Set_Summ3(totalCompat)
            4 -> classic.Set_Summ4(totalCompat)
            5 -> classic.Set_Summ5(totalCompat)
            6 -> classic.Set_Summ6(totalCompat)
            7 -> classic.Set_Summ7(totalCompat)
            8 -> classic.Set_Summ8(totalCompat)
            9 -> classic.Set_Summ9(totalCompat)
            10 -> classic.Set_Summ10(totalCompat)
            else -> classic.Set_Summ1(totalCompat)
        }
    }

    private fun registration(task: Task) {
        if (classic.Get_CheckType() == CHECK_TYPE_SELL || classic.Get_CheckType() == CHECK_TYPE_ADD) {
            classic.Set_CheckType(CHECK_TYPE_ADD)
        } else {
            classic.Set_CheckType(CHECK_TYPE_RETURN)
        }
        classic.Set_Tax1(task.param.tax ?: 0)
        classic.Set_Quantity(task.param.quantity?.toDouble() ?: 0.0)
        classic.Set_Price(task.param.price?.toShtrihLong() ?: 0L)
        classic.Set_StringForPrinting(task.data)
        task.param.paymentMode?.let { classic.Set_PaymentTypeSign(it) }
        task.param.itemType?.let { classic.Set_PaymentItemSign(it) }

        classic.FNOperation().checkOrThrow()
        clearPrintString()
    }

    private fun openCheckReturn(task: Task) {
        checkSessionOrThrow()
        classic.OpenSession()
        classic.Set_CheckType(CHECK_TYPE_RETURN)
        classic.OpenCheck().checkOrThrow()
        initCheckParams(task)
    }

    private fun report(task: Task) {
        if (task.param.reportType == ReportType.REPORT_Z) {
            reportZ()
        } else {
            reportX()
        }
    }

    private fun cashOperation(task: Task, cashFunction: () -> Int) {
        cancelCheck()
        checkSessionOrThrow()
        classic.Set_Summ1(task.param.sum?.toShtrihLong() ?: 0L)
        cashFunction().checkOrThrow()
    }

    private fun clearPrintString() {
        classic.Set_StringForPrinting("")
    }

    private fun openCheckSell(task: Task) {
        checkSessionOrThrow()
        classic.OpenSession()
        classic.Set_CheckType(CHECK_TYPE_SELL)
        classic.OpenCheck().checkOrThrow()
        initCheckParams(task)
    }

    private fun initCheckParams(task: Task) {
        setClientContact(task)
    }

    private fun setClientContact(task: Task) {
        task.param.clientContact?.let { clientContact ->
            classic.Set_EmailAddress(clientContact)
            classic.Set_CustomerEmail(clientContact)
            classic.FNSendCustomerEmail()
        }
    }

    private fun closeCheck() {
        classic.CloseCheck().checkOrThrow()
    }

    private fun cancelCheck() {
        classic.CancelCheck()
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
        clearPrintString()
    }

    private fun printStringInternal(task: Task, lineLength: Int) {
        wrapText(task, lineLength)
            .map { text -> applyAlignment(text, task.param.alignment, lineLength) }
            .forEach {
                classic.Set_StringForPrinting(it)
                classic.PrintString().checkOrThrow()
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

    private fun buildMessageByException(e: Throwable): String = when (e) {
        is UnknownHostException,
        is ConnectException,
        is NoRouteToHostException -> context.getString(R.string.equipment_error_host_connection_failure)

        is SocketTimeoutException,
        is TimeoutException -> context.getString(R.string.equipment_error_time_out)

        is IOException -> context.getString(R.string.equipment_error_io_exception, e.toString())

        is ExecuteException -> e.message ?: e.toString()

        else -> e.cause?.let { buildMessageByException(it) } ?: e.message ?: e.toString()
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

    private fun isSessionActive() = classic.Get_ECRMode() != ECR_MODE_SESSION_EXPIRED

    private fun checkSessionOrThrow() {
        if (!isSessionActive())
            throw RuntimeException(context.getString(R.string.equipment_lib_session_expired))
    }

    private fun reportZ() = classic.PrintReportWithCleaning().checkOrThrow()

    private fun reportX() = classic.PrintReportWithoutCleaning().checkOrThrow()

    private fun BigDecimal.toShtrihLong() = this.multiply(ONE_HUNDRED).toLong()

}