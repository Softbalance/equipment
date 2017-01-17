package ru.softbalance.equipment.presenter


import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.mapping.jackson.JacksonConfigurator
import ru.softbalance.equipment.model.printserver.PrintServer
import ru.softbalance.equipment.model.printserver.api.PrintServerApi
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceDriver
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceModel
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceType
import ru.softbalance.equipment.model.printserver.api.model.SettingsValues
import ru.softbalance.equipment.model.printserver.api.response.settings.*
import ru.softbalance.equipment.view.fragment.PrintServerFragment

class PrintServerPresenter(var url: String, var port: Int) : Presenter<PrintServerFragment> () {

    private var isDeviceRequest: Boolean = false
    private var isModelRequest: Boolean = false
    private var isSettingsRequest: Boolean = false
    private var isZipSettingsRequest: Boolean = false
    private var isPrintRequest: Boolean = false
    var connectedSuccessful: Boolean = false
    var printSuccessful: Boolean = false

    var settings: String = ""

    var deviceTypes: MutableList<PrintDeviceType>? = null
    var deviceType : PrintDeviceType? = null
    var models: MutableList<PrintDeviceModel>? = null
    private var model: PrintDeviceModel? = null
    var drivers: MutableList<PrintDeviceDriver>? = null
    var driver: PrintDeviceDriver? = null
    private var deviceSettings: SettingsResponse? = null
    var zipSettings: String? = null

    private var printer: PrintServer? = null

    private var deviceRequest: Disposable? = null
    private var modelRequest: Disposable? = null
    private var settingsRequest: Disposable? = null
    private var zipSettingsRequest: Disposable? = null

    private var printAvailable:Boolean = false
        get() = connectedSuccessful && zipSettings != null

    var api : PrintServerApi? = null

    private fun buildApi (url : String, port : Int) {
        if (api != null && this.url == url && this.port == port) return

        api = Retrofit.Builder()
                .baseUrl(getPrintServerUrl(url, port))
                .client(OkHttpClient.Builder()
                        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                        .build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(JacksonConfigurator.build()))
                .build()
                .create(PrintServerApi::class.java)
    }

    override fun bindView(view: PrintServerFragment){
        super.bindView(view)

        if (isDeviceRequest) {
            view()?.showLoading(context?.getString(R.string.test_print) ?: "")
        } else {
            view()?.hideLoading()
        }

        restoreUiState()
    }

    fun restoreUiState(){
        view()?.let {
            it.showConnectionState(connectedSuccessful)
            if (driver != null) it.showDriver(driver!!)
            if (model != null) it.showModel(model!!)
            if (deviceType != null) it.showType(deviceType!!)
            if (deviceSettings != null) it.buildSettingsUI(settingsList())
            it.showPrintAvailable(printAvailable)
            it.showPrintState(printSuccessful)
        }
    }

    private fun settingsList() : MutableList<SettingsPresenter<*, *>>{
        val settingsPresenters = mutableListOf<SettingsPresenter<*, *>>()
        deviceSettings?.let {
            settingsPresenters.addAll(it.boolSettings)
            settingsPresenters.addAll(it.stringSettings)
            settingsPresenters.addAll(it.listSettings)
        }

        return settingsPresenters
    }

    override fun unbindView(view: PrintServerFragment) {
        view()?.hideLoading()

        super.unbindView(view)
    }

    override fun onFinish() {
        deviceRequest?.dispose()
    }

    fun getDevices(url: String, port: Int) {

        val baseUrl: String = getPrintServerUrl(url, port)

        if (!isDeviceRequest && context != null) {

            if(HttpUrl.parse(baseUrl) == null) {
                view()?.showError(context?.getString(R.string.wrong_url_format) ?: "")
                return
            }

            buildApi(url, port)

            isDeviceRequest = true

            view()?.showLoading(context?.getString(R.string.connect_in_progress) ?: "")

            deviceRequest = api!!.getDeviceTypes()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isDeviceRequest = false
                        view()?.hideLoading()
                        view()?.showConnectionState(connectedSuccessful)
                    }
                    .subscribe({
                        connectedSuccessful = it.isSuccess()
                        this.url = url
                        this.port = port
                        deviceTypes = it.deviceTypes as MutableList
                        view()?.showConfirm(it.resultInfo)
                    }, {
                        connectedSuccessful = false
                        view()?.showError(it.toString())
                    })
        }
    }

    fun getModelsAndDrivers(deviceTypeId : Int) {

        if (!isModelRequest && context != null && api != null) {

            isModelRequest = true

            view()?.showLoading(context?.getString(R.string.connect_in_progress) ?: "")

            modelRequest = api!!.getModels(deviceTypeId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isModelRequest = false
                        view()?.hideLoading()
                    }
                    .subscribe({
                        models = it.models as MutableList
                        drivers = it.drivers as MutableList
                        view()?.showConfirm(it.resultInfo)
                    }, {
                        view()?.showError(it.toString())
                    })
        }
    }

    private fun requestSettings(curDriverId: String) {

        if (!isSettingsRequest && context != null && api != null) {

            isSettingsRequest = true

            view()?.showLoading(context?.getString(R.string.connect_in_progress) ?: "")

            settingsRequest = api!!.getDeviceSettings(curDriverId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isSettingsRequest = false
                        view()?.hideLoading()
                        view()?.showPrintAvailable(printAvailable)
                    }
                    .subscribe({
                        deviceSettings = it
                        view()?.buildSettingsUI(settingsList())
                        view()?.showConfirm(it.resultInfo)
                    }, {
                        view()?.showError(it.toString())
                    })
        }
    }

    fun saveSettings() {
        if (!isZipSettingsRequest && context != null && api != null && deviceSettings != null) {

            isZipSettingsRequest = true

            val settingsValues = SettingsValues().apply {
                driverId = driver?.id ?: ""
                modelId = model?.id ?: ""
                typeId = deviceType?.id ?: 0
                boolValues = deviceSettings!!.boolSettings
                stringValues = deviceSettings!!.stringSettings
                listValues = deviceSettings!!.listSettings
            }

            view()?.showLoading(context?.getString(R.string.connect_in_progress) ?: "")

            zipSettingsRequest = api!!.compressSettings(settingsValues)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isZipSettingsRequest = false
                        view()?.hideLoading()
                        view()?.showPrintAvailable(printAvailable)
                    }
                    .subscribe({
                        zipSettings = it.compressedSettings
                        view()?.showConfirm(it.resultInfo)
                    }, {
                        view()?.showError(it.toString())
                    })
        }
    }

    fun testPrint() {
        if(api != null && !isPrintRequest && !zipSettings.isNullOrEmpty()) {
            printer = PrintServer(api!!, zipSettings ?: "")
            isPrintRequest = true
            printer!!.execute(listOf(Task(context!!.getString(R.string.test_print))))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isPrintRequest = false
                        view()?.hideLoading()
                        view()?.showPrintAvailable(printAvailable)
                    }
                    .subscribe({
                        printSuccessful = it.isSuccess()
                        view()?.showConfirm(it.resultInfo)
                        view()?.showPrintState(printSuccessful)
                    }, {
                        view()?.showError(it.toString())
                    })
        }
    }

    fun getPrintServerUrl(url: String, port: Int): String = "http://$url:$port"

    fun selectDeviceType(deviceType: PrintDeviceType) {
        this.deviceType = deviceType
        view()?.showType(deviceType)
        getModelsAndDrivers(deviceType.id)
    }

    fun selectModel(model: PrintDeviceModel) {
        this.model = model
        view()?.showModel(model)
    }

    fun selectDriver(driver: PrintDeviceDriver) {
        this.driver = driver
        view()?.showDriver(driver)
        requestSettings(driver.id)
    }

    fun saveSettingValue(vp: SettingsPresenter<*, *>) {
        deviceSettings?.let {
            when(vp){
                is BooleanSettingsPresenter -> it.boolSettings
                        .filter { set -> vp.id == set.id }
                        .forEach { set -> set.value = vp.value }
                is StringSettingsPresenter -> it.stringSettings
                        .filter { set -> vp.id == set.id }
                        .forEach { set -> set.value = vp.value }
                is ListSettingsPresenter -> it.listSettings
                        .filter { set -> vp.id == set.id }
                        .forEach { set -> set.value = vp.value }
            }
        }
    }
}
