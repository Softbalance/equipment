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

    enum class FFD_VERSION { V_1_0_0, V_1_0_5, V_1_1_0 }

    private val context: Context = context.applicationContext

    private val driver: IFptr by lazy { prepareDriver() }

    var ffdVersion = FFD_VERSION.V_1_0_0
        get() {
            driver.put_RegisterNumber(54)
            driver.GetRegister()
            return when (driver._DeviceFfdVersion) {
                100 -> FFD_VERSION.V_1_0_0
                105 -> FFD_VERSION.V_1_0_5
                110 -> FFD_VERSION.V_1_1_0
                else -> FFD_VERSION.V_1_0_0
            }
        }

    var driverStatus = DriverStatus.NOT_INITIALIZED

    private var lastInfo = ""

    companion object {
        private const val RESULT_OK = 0

        private const val TAX_INDEX = 201
        private const val TAX_FIRST = 1
        private const val TAX_LAST = 6

        private const val SERIAL_REGISTER_INDEX = 22

        private const val PRINT_STRING_TRAIT = "trait"
        private const val PRINT_STRING_DASH = "dash"

        private const val TRAIT_TEMPLATE = "=================================================="
        private const val DASH_TEMPLATE = "--------------------------------------------------"
    }

    override fun execute(tasks: List<Task>,
                         finishAfterExecute: Boolean): Single<EquipmentResponse> {
        return Single.fromCallable { executeTasksInternal(tasks, finishAfterExecute) }
            .subscribeOn(Schedulers.io())
    }

    override fun finish() {
        if (driverStatus == DriverStatus.INITIALIZED) {
            driver.put_DeviceEnabled(false)
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
            info = context.getString(
                R.string.equipment_error_code,
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

    private fun executeTasksInternal(tasks: List<Task>,
                                     finishAfterExecute: Boolean): EquipmentResponse {
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
            TaskType.OPEN_CHECK_SELL -> openCheckSell(task)
            TaskType.PAYMENT -> payment(task)
            TaskType.OPEN_CHECK_RETURN -> openCheckReturn()
            TaskType.RETURN -> refund(task)
            TaskType.CASH_INCOME -> cashOperation(IFptr.CASH_INCOME, task)
            TaskType.CASH_OUTCOME -> cashOperation(IFptr.CASH_OUTCOME, task)
            TaskType.CLIENT_CONTACT -> setClientContact(task.data)
            TaskType.REPORT -> report(task)
            TaskType.SYNC_TIME -> syncTime()
            TaskType.PRINT_HEADER, TaskType.PRINT_FOOTER -> printHeader()
            TaskType.CUT -> cut()
            TaskType.PRINT_SLIP -> printSlip(task)
            else -> {
                Log.e(
                    Atol::class.java.simpleName,
                    context.getString(R.string.equipment_lib_operation_not_supported, task.type))
                return true
            }
        }
    }

    private fun registration(task: Task): Boolean {
        return prepareItemRegistration(task) && driver.Registration().isOK() // TODO Registration -> EndItem
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

        task.param.department?.let {
            driver.put_Department(it)
        }

        if (ffdVersion == FFD_VERSION.V_1_0_5) {
            task.param.itemType?.let {
                driver.put_PositionType(it) // Предмет расчета
            }
            task.param.paymentMode?.let {
                driver.put_PositionPaymentType(it) // Способ расчета
            }
            driver.put_TaxMode(0) // set automatic mode
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
        return if (setMode(IFptr.MODE_REGISTRATION)) {
            driver.CancelCheck()
            true
        } else {
            false
        }
    }

    private fun setClientContact(clientContact: String): Boolean =
        setStringFiscalProperty(1008, clientContact)

    private fun setStringFiscalProperty(code: Int, value: String): Boolean {
        driver.put_FiscalPropertyNumber(code)
        driver.put_FiscalPropertyType(IFptr.FISCAL_PROPERTY_TYPE_STRING)
        driver.put_FiscalPropertyValue(value)
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

    private fun openCheckSell(task: Task): Boolean {
        val result =
            prepareRegistration() && openCheck(IFptr.CHEQUE_TYPE_SELL) && initCheckParams(task)
        if (!result) closeCheck()
        return result
    }

    private fun initCheckParams(task: Task): Boolean {
        task.param.clientContact?.let {
            if (!setClientContact(it)) {
                lastInfo = context.getString(R.string.incorrect_client_contact)
                return false
            }
        }
        task.param.cashierINN?.let {
            if (!setStringFiscalProperty(1203, it)) {
                lastInfo = context.getString(R.string.incorrect_cashier_inn)
                return false
            }
        }
        val cashier: String =
            (task.param.cashierName?.trim() ?: "") + " " + (task.param.cashierPosition?.trim()
                    ?: "")
        if (cashier.isNotBlank()) {
            if (!setStringFiscalProperty(1021, cashier)) {
                lastInfo = context.getString(R.string.incorrect_cashier_name)
                return false
            }
        }
        /*
        FIXME for unknown reasons this operation leads to error «Смена открыта - операция невозможна»
        driver developers [mailto:a.belikov@atol.ru] didn't say anything useful

        task.param.paymentPlace?.let {
            if (!setStringFiscalProperty(1187, it)){
                lastInfo = context.getString(R.string.incorrect_payment_place)
                return false
            }
        }*/
        return true
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
        driver.put_TextWrap(if (params.wrap == true) IFptr.WRAP_WORD else IFptr.WRAP_NONE)

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
                Log.e(
                    Atol::class.java.simpleName,
                    "The report operation ${task.param.reportType} isn't supported")
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

        val taxes = (TAX_FIRST..TAX_LAST)
            .map { index ->
                Tax().apply {
                    id = index.toLong()
                    driver.put_CaptionPurpose(index + TAX_INDEX)
                    driver.GetCaption()
                    title = driver._Caption
                }
            }

        if (finishAfterExecute) {
            finish()
        }

        return taxes
    }

    override fun getSerial(finishAfterExecute: Boolean): Single<SerialResponse> {
        return Single.fromCallable { getSerialInternal(finishAfterExecute) }
            .subscribeOn(Schedulers.io())
    }

    private fun getSerialInternal(finishAfterExecute: Boolean): SerialResponse {
        var currentSerial = ""
        driver.run {

            if (driver.put_DeviceEnabled(true).isFail()) {
                return SerialResponse().handlingError() as SerialResponse
            }

            put_RegisterNumber(SERIAL_REGISTER_INDEX)
            GetRegister()
            currentSerial = _SerialNumber
        }
        if (finishAfterExecute) {
            finish()
        }

        return if (currentSerial.isNotBlank()) {
            SerialResponse().apply {
                resultCode = ResponseCode.SUCCESS
                serial = currentSerial
            }
        } else {
            SerialResponse().handlingError() as SerialResponse
        }
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

        cancelCheck()
        val openShiftResponse =
            if (setMode(IFptr.MODE_REGISTRATION) && driver.OpenSession().isOK()) {
                OpenShiftResponse().successResult()
            } else {
                OpenShiftResponse().handlingError()
            }

        if (finishAfterExecute) {
            finish()
        }

        return openShiftResponse as OpenShiftResponse
    }

    fun getInternalDriver(): IFptr = driver

    fun printSlip(task: Task) = (driver.put_Caption(task.data).isOK()
            && driver.put_TextWrap(IFptr.WRAP_LINE).isOK()
            && driver.put_Alignment(IFptr.ALIGNMENT_LEFT).isOK()
            && driver.PrintString().isOK()
            && driver.put_Mode(IFptr.MODE_REPORT_NO_CLEAR).isOK()
            && driver.SetMode().isOK()
            && driver.PrintFooter().isOK())
}