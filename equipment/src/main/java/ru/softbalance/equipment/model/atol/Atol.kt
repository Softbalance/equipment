package ru.softbalance.equipment.model.atol

import android.content.Context
import android.util.Log
import com.atol.drivers.fptr.Fptr
import com.atol.drivers.fptr.IFptr
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.*
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers
import java.math.BigDecimal
import java.util.*

class Atol(context: Context, val settings: String) : EcrDriver {

    private val context: Context = context.applicationContext

    private val driver: IFptr by lazy { prepareDriver() }

    private var driverStatus = DriverStatus.NOT_INITIALIZED

    private var lastInfo = ""

    companion object {
        private const val RESULT_OK = 0

        private const val TAX_INDEX = 201
        private const val TAX_FIRST = 1
        private const val TAX_LAST = 5

        private const val SERIAL_REGISTER_INDEX = 22

        private const val PRINT_STRING_TRAIT = "trait"
        private const val PRINT_STRING_DASH = "dash"

        private const val TRAIT_TEMPLATE = "=================================================="
        private const val DASH_TEMPLATE = "--------------------------------------------------"
    }

    override fun execute(tasks: List<Task>, finishAfterExecute: Boolean): Single<EquipmentResponse> {
        return Single.fromCallable { executeTasksInternal(tasks, finishAfterExecute) }
                .subscribeOn(Schedulers.io())
    }

    override fun finish() {
        if (driverStatus == DriverStatus.INITIALIZED) {
            driverStatus = DriverStatus.FINISHED
            try {
                driver.destroy()
            } catch (e: Exception) {
                Log.e(Atol::class.java.simpleName, null, e)
            }
        }
    }

    private fun Int.isOK() = this == RESULT_OK
    private fun Int.isFail() = this != RESULT_OK

    @Throws(IllegalArgumentException::class)
    private fun prepareDriver(): IFptr {
        val driver = Fptr()
        driver.create(context)

        if (settings.isNotEmpty() && driver.put_DeviceSettings(settings).isFail()) {
            val initFailure = context.getString(R.string.equipment_init_failure)
            val incorrectSettings = context.getString(R.string.equipment_incorrect_settings)
            throw IllegalArgumentException("$initFailure : $incorrectSettings. ${getInfo()}")
        }

        driverStatus = DriverStatus.INITIALIZED

        return driver
    }

    private fun getInfo(): String {
        val info: String
        if (lastInfo.isNotEmpty()) {
            info = lastInfo
            lastInfo = ""
        } else {
            info = context.getString(R.string.equipment_error_code,
                    driver._ResultCode,
                    driver._ResultDescription)
        }

        return info
    }

    private fun BaseResponse.initFailure(): BaseResponse = this.apply {
        resultCode = ResponseCode.LOGICAL_ERROR
        resultInfo = context.getString(R.string.equipment_init_failure)
    }

    private fun BaseResponse.handlingError(): BaseResponse = this.apply {
        resultCode = ResponseCode.HANDLING_ERROR
        resultInfo = getInfo()
    }

    private fun BaseResponse.successResult(): BaseResponse = this.apply {
        resultCode = ResponseCode.SUCCESS
        resultInfo = getInfo()
    }

    private fun executeTasksInternal(tasks: List<Task>, finishAfterExecute: Boolean): EquipmentResponse {
        if (driverStatus == DriverStatus.FINISHED) {
            return EquipmentResponse().initFailure() as EquipmentResponse
        }

        if (driver.put_DeviceEnabled(true).isFail()) {
            return EquipmentResponse().handlingError() as EquipmentResponse
        }

        tasks.forEach {
            if (!executeTask(it)) {
                return EquipmentResponse().apply {
                    resultCode = ResponseCode.HANDLING_ERROR
                    resultInfo = getInfo() + " (task " + it.type + ")"
                }
            }
        }

        driver.put_DeviceEnabled(false)

        val result = EquipmentResponse().successResult() as EquipmentResponse

        if (finishAfterExecute) {
            finish()
        }
        return result
    }

    private fun executeTask(task: Task): Boolean {
        return when (task.type.toLowerCase()) {
            TaskType.STRING -> printString(task)
            TaskType.REGISTRATION -> registration(task)
            TaskType.CLOSE_CHECK -> closeCheck()
            TaskType.CANCEL_CHECK -> cancelCheck()
            TaskType.OPEN_CHECK_SELL -> openCheckSell()
            TaskType.PAYMENT -> payment(task)
            TaskType.OPEN_CHECK_RETURN -> openCheckReturn()
            TaskType.RETURN -> refund(task)
            TaskType.CASH_INCOME -> cashOperation(IFptr.CASH_INCOME, task)
            TaskType.CASH_OUTCOME -> cashOperation(IFptr.CASH_OUTCOME, task)
            TaskType.CLIENT_CONTACT -> setClientContact(task)
            TaskType.REPORT -> report(task)
            TaskType.SYNC_TIME -> syncTime()
            TaskType.PRINT_HEADER, TaskType.PRINT_FOOTER -> printHeader()
            TaskType.CUT -> cut()
            else -> {
                Log.e(Atol::class.java.simpleName,
                        context.getString(R.string.equipment_lib_operation_not_supported, task.type))
                return true
            }
        }
    }

    private fun registration(task: Task): Boolean {
        return prepareItemRegistration(task) && driver.Registration().isOK()
    }

    private fun refund(task: Task): Boolean {
        return prepareItemRegistration(task) && driver.Return().isOK()
    }

    private fun prepareItemRegistration(task: Task): Boolean {
        if (task.data.isEmpty()) {
            lastInfo = context.getString(R.string.equipment_incorrect_product_name)
            return false
        } else {
            driver.put_Name(task.data)
        }

        driver.put_TextWrap(IFptr.WRAP_WORD)

        val price = task.param.price
        if (price == null || price <= BigDecimal.ZERO) {
            lastInfo = context.getString(R.string.equipment_incorrect_product_price)
            return false
        } else {
            driver.put_Price(price.toDouble())
        }

        val quantity = task.param.quantity
        if (quantity == null || quantity <= BigDecimal.ZERO) {
            lastInfo = context.getString(R.string.equipment_incorrect_product_quantity)
            return false
        } else {
            driver.put_Quantity(quantity.toDouble())
        }

        val sum = task.param.sum?.toDouble() ?: (driver._Quantity * driver._Price)
        driver.put_PositionSum(sum)

        task.param.tax?.let {
            driver.put_TaxNumber(it)
        }

        return true
    }

    private fun payment(task: Task): Boolean {
        val sum = task.param.sum ?: BigDecimal.ZERO
        if (sum > BigDecimal.ZERO) {
            driver.put_Summ(sum.toDouble())
        } else {
            lastInfo = context.getString(R.string.equipment_payment_incorrect)
            return false
        }

        val typeClose = task.param.typeClose
        if (typeClose == null) {
            lastInfo = context.getString(R.string.equipment_type_close_incorrect)
            return false
        } else {
            driver.put_TypeClose(typeClose)
        }

        return driver.Payment().isOK()
    }

    private fun closeCheck(): Boolean {
        return driver._CheckState == IFptr.CHEQUE_STATE_CLOSED || driver.CloseCheck().isOK()
    }

    private fun cancelCheck(): Boolean {
        val isSetModeOK = setMode(IFptr.MODE_REGISTRATION)
        driver.CancelCheck()
        return isSetModeOK
    }

    private fun setClientContact(task: Task): Boolean {
        driver.put_FiscalPropertyNumber(1008)
        driver.put_FiscalPropertyType(IFptr.FISCAL_PROPERTY_TYPE_STRING)
        driver.put_FiscalPropertyValue(task.data)
        return driver.WriteFiscalProperty().isOK()
    }

    private fun cashOperation(cashType: Int, task: Task): Boolean {
        var isOK = false

        if (prepareRegistration()) {
            val amount = task.param.sum ?: BigDecimal.ZERO
            if (amount > BigDecimal.ZERO) {
                driver.put_Summ(amount.toDouble())
                when (cashType) {
                    IFptr.CASH_INCOME -> isOK = driver.CashIncome().isOK()
                    IFptr.CASH_OUTCOME -> isOK = driver.CashOutcome().isOK()
                }
            }
        }

        return isOK
    }

    private fun openCheckSell(): Boolean {
        return prepareRegistration() && openCheck(IFptr.CHEQUE_TYPE_SELL)
    }

    private fun openCheckReturn(): Boolean {
        return prepareRegistration() && openCheck(IFptr.CHEQUE_TYPE_RETURN)
    }

    private fun openCheck(chequeState: Int): Boolean {
        driver.put_CheckType(chequeState)
        return driver.OpenCheck().isOK()
    }

    private fun printString(task: Task): Boolean {
        val data = task.data.trim()

        if (data.contains(PRINT_STRING_TRAIT, true)) {
            return printSimpleString(buildSymbolicPrintTask(TRAIT_TEMPLATE))
        } else if (data.contains(PRINT_STRING_DASH, true)) {
            return printSimpleString(buildSymbolicPrintTask(DASH_TEMPLATE))
        }

        return printSimpleString(task)
    }

    private fun buildSymbolicPrintTask(symbols: String): Task = Task().apply { data = symbols }

    private fun printSimpleString(task: Task): Boolean {
        val params = task.param
        driver.put_TextWrap(if (params.wrap ?: false) IFptr.WRAP_WORD else IFptr.WRAP_NONE)

        if (driver._TextWrap == IFptr.WRAP_NONE && task.data.length > driver._CharLineLength) {
            driver.put_Caption(task.data.substring(0, driver._CharLineLength))
        } else {
            driver.put_Caption(task.data)
        }

        driver.put_Alignment(convertAlign(params.alignment ?: Alignment.LEFT))
        driver.put_FontBold(params.bold ?: false)
        driver.put_FontItalic(params.italic ?: false)

        return driver.PrintString().isOK()
    }

    private fun convertAlign(@Alignment alignment: String): Int {
        when (alignment) {
            Alignment.CENTER -> return IFptr.ALIGNMENT_CENTER
            Alignment.RIGHT -> return IFptr.ALIGNMENT_RIGHT
            else -> return IFptr.ALIGNMENT_LEFT
        }
    }

    private fun prepareRegistration(): Boolean {
        cancelCheck()
        return setMode(IFptr.MODE_REGISTRATION) && openShift()
    }

    private fun openShift(): Boolean {
        // it is hacky cause driver has bug when checking session status, so just force open the shift
        driver.OpenSession()
        return true
    }

    private fun setMode(mode: Int): Boolean {
        driver.put_UserPassword(driver._UserPassword)
        driver.put_Mode(mode)
        return driver.SetMode().isOK()
    }

    private fun report(task: Task): Boolean {
        cancelCheck()

        val mode: Int = if (task.param.reportType == ReportType.REPORT_Z) {
            IFptr.MODE_REPORT_CLEAR
        } else {
            IFptr.MODE_REPORT_NO_CLEAR
        }

        if (!setMode(mode)) {
            return false
        }

        val reportType: Int = when (task.param.reportType) {
            ReportType.REPORT_Z -> IFptr.REPORT_Z
            ReportType.REPORT_X -> IFptr.REPORT_X
            ReportType.REPORT_DEPARTMENT -> IFptr.REPORT_DEPARTMENTS
            ReportType.REPORT_CASHIERS -> IFptr.REPORT_CASHIERS
            ReportType.REPORT_HOURS -> IFptr.REPORT_HOURS
            else -> {
                Log.e(Atol::class.java.simpleName, "The report operation ${task.param.reportType} isn't supported")
                return false
            }
        }

        driver.put_ReportType(reportType)
        return driver.Report().isOK()
    }

    private fun cut(): Boolean {
        driver.PartialCut().isOK() || driver.FullCut().isOK()
        return true
    }

    private fun syncTime(): Boolean {
        cancelCheck()

        return with(Date()) {
            driver.put_Date(this).isOK()
                    && driver.put_Time(this).isOK()
                    && driver.SetDate().isOK()
                    && driver.SetTime().isOK()
        }
    }

    private fun printHeader(): Boolean {
        return driver.PrintHeader().isOK()
    }

    private fun printFooter(): Boolean {
        return setMode(IFptr.MODE_REPORT_NO_CLEAR) && driver.PrintFooter().isOK()
    }

    fun getDefaultSettings(): String {
        return driver._DeviceSettings
    }

    fun getTaxes(finishAfterExecute: Boolean): Observable<List<Tax>> {
        return Observable.fromCallable { getTaxesInternal(finishAfterExecute) }
                .subscribeOn(Schedulers.io())
    }

    private fun getTaxesInternal(finishAfterExecute: Boolean): List<Tax> {
        if (driverStatus == DriverStatus.FINISHED) {
            throw RuntimeException(context.getString(R.string.equipment_init_failure))
        }

        if (driver.put_DeviceEnabled(true).isFail()) {
            throw RuntimeException(getInfo())
        }

        if (!setMode(IFptr.MODE_PROGRAMMING)) {
            throw RuntimeException(getInfo())
        }

        val taxes = TAX_FIRST.rangeTo(TAX_LAST)
                .map { index ->
                    Tax().apply {
                        id = index.toLong()
                        driver.put_CaptionPurpose(index + TAX_INDEX)
                        driver.GetCaption()
                        title = driver._Caption
                    }
                }

        driver.put_DeviceEnabled(false)

        if (finishAfterExecute) {
            finish()
        }

        return taxes
    }

    override fun getSerial(finishAfterExecute: Boolean): Single<String> {
        return Single.fromCallable { getSerialInternal(finishAfterExecute) }
                .subscribeOn(Schedulers.io())
    }

    private fun getSerialInternal(finishAfterExecute: Boolean): String {
        var serial: String = ""
        driver.run {
            put_DeviceEnabled(true)
            put_RegisterNumber(SERIAL_REGISTER_INDEX)
            GetRegister()
            serial = _SerialNumber
            put_DeviceEnabled(false)
        }
        if (finishAfterExecute) {
            finish()
        }
        return serial
    }

    override fun getSessionState(finishAfterExecute: Boolean): Single<SessionStateResponse> {
        return Single.fromCallable { getSessionStateInternal(finishAfterExecute) }
                .subscribeOn(Schedulers.io())
    }

    private fun getSessionStateInternal(finishAfterExecute: Boolean): SessionStateResponse {
        if (driverStatus == DriverStatus.FINISHED) {
            return SessionStateResponse().initFailure() as SessionStateResponse
        }

        if (driver.put_DeviceEnabled(true).isFail()) {
            return SessionStateResponse().handlingError() as SessionStateResponse
        }

        // it is required for get all information from registers
        // code belong won't work without calling get_InfoLine method
        driver._InfoLine

        val result = SessionStateResponse().apply {
            frSessionState = FrSessionState().apply {
                shiftOpen = driver._SessionOpened
                shiftNumber = driver._Session
                paperExists = driver._CheckPaperPresent
            }
            resultCode = ResponseCode.SUCCESS
            resultInfo = getInfo()
        }

        driver.put_DeviceEnabled(false)

        if (finishAfterExecute) {
            finish()
        }
        return result
    }

    override fun openShift(finishAfterExecute: Boolean): Single<OpenShiftResponse> {
        return Single.fromCallable { openShiftInternal(finishAfterExecute) }
                .subscribeOn(Schedulers.io())
    }

    private fun openShiftInternal(finishAfterExecute: Boolean): OpenShiftResponse {
        if (driverStatus == DriverStatus.FINISHED) {
            return OpenShiftResponse().initFailure() as OpenShiftResponse
        }

        if (driver.put_DeviceEnabled(true).isFail()) {
            return OpenShiftResponse().handlingError() as OpenShiftResponse
        }

        prepareRegistration()

        driver.put_DeviceEnabled(false)

        val openShiftResponse = OpenShiftResponse().successResult() as OpenShiftResponse

        if (finishAfterExecute) {
            finish()
        }

        return openShiftResponse
    }
}