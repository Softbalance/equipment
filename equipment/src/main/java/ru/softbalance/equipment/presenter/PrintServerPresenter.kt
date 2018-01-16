package ru.softbalance.equipment.presenter


import android.content.Context
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.softbalance.equipment.R
import ru.softbalance.equipment.isActive
import ru.softbalance.equipment.model.EquipmentResponse
import ru.softbalance.equipment.model.ExceptionHandler
import ru.softbalance.equipment.model.Task
import ru.softbalance.equipment.model.TaskType
import ru.softbalance.equipment.model.mapping.jackson.JacksonConfigurator
import ru.softbalance.equipment.model.printserver.PrintServer
import ru.softbalance.equipment.model.printserver.api.PrintServerApi
import ru.softbalance.equipment.model.printserver.api.model.*
import ru.softbalance.equipment.model.printserver.api.response.settings.*
import ru.softbalance.equipment.toHttpUrl
import ru.softbalance.equipment.view.fragment.PrintServerFragment
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class PrintServerPresenter(context: Context,
                           var url: String,
                           var port: Int,
                           var zipSettings: String? = null) : Presenter<PrintServerFragment>(context) {

    private val exceptionHandler = ExceptionHandler(context)

    var connectedSuccessful: Boolean = false
    var printSuccessful: Boolean = false

    var settings: String = ""

    var deviceTypes = emptyList<PrintDeviceType>()

    var deviceType: PrintDeviceType? = null
    var models = emptyList<PrintDeviceModel>()
    private var model: PrintDeviceModel? = null
    var drivers = emptyList<PrintDeviceDriver>()
    var driver: PrintDeviceDriver? = null
    private var deviceSettings: SettingsResponse? = null

    private var restoreSettingsRequest: Subscription? = null
    private var deviceRequest: Subscription? = null
    private var modelRequest: Subscription? = null
    private var settingsRequest: Subscription? = null
    private var zipSettingsRequest: Subscription? = null
    private var printRequest: Subscription? = null

    private fun isPrintAvailable(): Boolean {
        return connectedSuccessful && zipSettings != null
    }

    val api: PrintServerApi
        get() {
            return Retrofit.Builder()
                    .baseUrl(url.toHttpUrl(port))
                    .client(OkHttpClient.Builder()
                            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                            .build())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(JacksonConverterFactory.create(JacksonConfigurator.build()))
                    .build()
                    .create(PrintServerApi::class.java)
        }

    override fun bindView(view: PrintServerFragment) {
        super.bindView(view)

        if (restoreSettingsRequest.isActive()
                || deviceRequest.isActive()
                || modelRequest.isActive()
                || settingsRequest.isActive()
                || zipSettingsRequest.isActive()) {
            view.showLoading(context.getString(R.string.connect_in_progress))
        } else if (printRequest.isActive()) {
            view.showLoading(context.getString(R.string.test_print))
        } else {
            view.hideLoading()
        }

        restoreUiState()
    }

    fun restoreUiState() {
        val view = view ?: return

        view.showConnectionState(connectedSuccessful)
        driver?.let { view.showDriver(it) }
        model?.let { view.showModel(it) }
        deviceType?.let { view.showType(it) }
        settingsList().let { if (it.isNotEmpty()) view.buildSettingsUI(it) }
        view.showPrintAvailable(isPrintAvailable())
        view.showPrintState(printSuccessful)
    }

    private fun settingsList(): MutableList<SettingsPresenter<*, *>> {
        val settingsPresenters = mutableListOf<SettingsPresenter<*, *>>()
        deviceSettings?.let {
            settingsPresenters.addAll(it.boolSettings)
            settingsPresenters.addAll(it.stringSettings)
            settingsPresenters.addAll(it.listSettings)
        }

        return settingsPresenters
    }

    override fun unbindView(view: PrintServerFragment) {
        hideLoading()

        super.unbindView(view)
    }

    override fun onFinish() {
        deviceRequest?.unsubscribe()
    }

    fun getDevices() {

        if (deviceRequest.isActive()) return

        if (HttpUrl.parse(url.toHttpUrl(port)) == null) {
            view?.showError(context.getString(R.string.wrong_url_format))
            return
        }

        showProgress()

        deviceRequest = api.getDeviceTypes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe {
                    view?.let {
                        it.hideLoading()
                        it.showConnectionState(connectedSuccessful)
                    }
                }
                .subscribe({
                    connectedSuccessful = it.isSuccess()
                    deviceTypes = it.deviceTypes
                    showResultInfo(it)
                }, {
                    connectedSuccessful = false
                    handleError(it)
                })
    }

    fun getModelsAndDrivers(deviceTypeId: Int) {

        if (modelRequest.isActive()) {
            return
        }

        showProgress()

        modelRequest = api.getModels(deviceTypeId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe { hideLoading() }
                .subscribe({
                    models = it.models
                    drivers = it.drivers
                    showResultInfo(it)
                }, {
                    handleError(it)
                })
    }

    private fun requestSettings(curDriverId: String) {

        if (settingsRequest.isActive()) {
            return
        }

        showProgress()

        settingsRequest = api.getDeviceSettings(curDriverId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe {
                    view?.let {
                        it.hideLoading()
                        it.showPrintAvailable(isPrintAvailable())
                    }
                }
                .subscribe({ settings ->
                    deviceSettings = settings
                    view?.buildSettingsUI(settingsList())
                    showResultInfo(settings)
                }, { handleError(it) })
    }

    fun saveSettings() {
        val settings = deviceSettings ?: return
        if (zipSettingsRequest.isActive()) {
            return
        }

        val settingsValues = SettingsValues().apply {
            driverId = driver?.id ?: ""
            modelId = model?.id ?: ""
            typeId = deviceType?.id ?: 0
            boolValues = settings.boolSettings
            stringValues = settings.stringSettings
            listValues = settings.listSettings
        }

        showProgress()

        zipSettingsRequest = api.compressSettings(settingsValues)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe {
                    view?.let {
                        it.hideLoading()
                        it.showPrintAvailable(isPrintAvailable())
                    }
                }
                .subscribe({
                    zipSettings = it.compressedSettings
                    showResultInfo(it)
                }, { handleError(it) })
    }

    fun testPrint() {
        if (printRequest.isActive() || zipSettings.isNullOrEmpty()) {
            return
        }

        val printTasks = listOf(
                Task().apply { data = context.getString(R.string.test_print) },
                Task().apply { type = TaskType.PRINT_FOOTER },
                Task().apply { type = TaskType.CUT })

        view?.showLoading(context.getString(R.string.test_print))

        printRequest = PrintServer(url, port, zipSettings ?: "")
                .execute(printTasks, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe {
                    view?.let {
                        it.hideLoading()
                        it.showPrintAvailable(isPrintAvailable())
                    }
                }
                .subscribe({ response ->
                    printSuccessful = response.isSuccess()
                    showResultInfo(response)
                    view?.showPrintState(printSuccessful)
                }, { handleError(it) })
    }

    private fun showResultInfo(result: EquipmentResponse) {
        if (result.isSuccess()) {
            view?.showConfirm(result.resultInfo)
        } else {
            view?.showError(result.resultInfo)
        }
    }

    fun selectDeviceType(deviceType: PrintDeviceType) {
        this.deviceType = deviceType
        view?.showType(deviceType)
        getModelsAndDrivers(deviceType.id)
    }

    fun selectModel(model: PrintDeviceModel) {
        this.model = model
        view?.showModel(model)
    }

    fun selectDriver(driver: PrintDeviceDriver) {
        this.driver = driver
        view?.showDriver(driver)
        requestSettings(driver.id)
    }

    fun saveSettingValue(vp: SettingsPresenter<*, *>) {
        deviceSettings?.let {
            when (vp) {
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

    fun connect(url: String, port: Int) {
        updateUrlAndPort(url, port)
        if (zipSettings.isNullOrEmpty()) {
            getDevices()
        } else {
            restoreSettings()
        }
    }

    private fun updateUrlAndPort(url: String, port: Int) {
        var requiredUpdateSettings = false
        if (!this.url.equals(url, true)) {
            this.url = url
            requiredUpdateSettings = true
        }

        if (this.port != port) {
            this.port = port
            requiredUpdateSettings = true
        }

        if (requiredUpdateSettings) {
            zipSettings = null
        }
    }

    private fun restoreSettings() {
        if (restoreSettingsRequest.isActive()) {
            return
        }

        showProgress()

        val printServerApi = api
        restoreSettingsRequest = printServerApi.extractDeviceSettings(CompressedSettings.create(zipSettings))
                .subscribeOn(Schedulers.io())
                .doOnUnsubscribe {
                    hideLoading()
                }
                .doOnNext { response ->
                    deviceSettings = response
                }
                .flatMap { printServerApi.getDeviceTypes() }
                .flatMap {
                    deviceTypes = it.deviceTypes
                    printServerApi.getModels(deviceSettings!!.typeId)
                }
                .doOnNext {
                    models = it.models
                    drivers = it.drivers
                    restoreSettingsState()

                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    connectedSuccessful = response.isSuccess()
                    showResultInfo(response)
                    hideLoading()
                    restoreUiState()
                }, { handleError(it) })
    }

    private fun handleError(throwable: Throwable?) {
        if (throwable == null) return
        view?.showError(exceptionHandler.getUserFriendlyMessage(throwable))
    }

    private fun hideLoading() {
        view?.hideLoading()
    }

    private fun restoreSettingsState() {
        deviceSettings?.let { ds ->
            deviceType = deviceTypes.firstOrNull { it.id == ds.typeId }
            model = models.firstOrNull { it.id == ds.modelId }
            driver = drivers.firstOrNull { it.id == ds.driverId }
        }
    }

    private fun showProgress() {
        view?.showLoading(context.getString(R.string.connect_in_progress))
    }
}

