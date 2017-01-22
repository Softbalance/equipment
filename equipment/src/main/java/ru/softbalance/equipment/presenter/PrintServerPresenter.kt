package ru.softbalance.equipment.presenter


import android.content.Context
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

class PrintServerPresenter(context: Context, var url: String, var port: Int) : Presenter<PrintServerFragment>(context) {

    private var isDeviceRequest: Boolean = false
    private var isModelRequest: Boolean = false
    private var isSettingsRequest: Boolean = false
    private var isZipSettingsRequest: Boolean = false
    private var isPrintRequest: Boolean = false
    var connectedSuccessful: Boolean = false
    var printSuccessful: Boolean = false

    var settings: String = ""

    var deviceTypes: MutableList<PrintDeviceType>? = null
    var deviceType: PrintDeviceType? = null
    var models: MutableList<PrintDeviceModel>? = null
    private var model: PrintDeviceModel? = null
    var drivers: MutableList<PrintDeviceDriver>? = null
    var driver: PrintDeviceDriver? = null
    private var deviceSettings: SettingsResponse? = null
    var zipSettings: String? = null

    private var deviceRequest: Disposable? = null
    private var modelRequest: Disposable? = null
    private var settingsRequest: Disposable? = null
    private var zipSettingsRequest: Disposable? = null

    private fun isPrintAvailable(): Boolean {
        return connectedSuccessful && zipSettings != null
    }

    val api: PrintServerApi
        get() {
            return Retrofit.Builder()
                    .baseUrl(getPrintServerUrl(url, port))
                    .client(OkHttpClient.Builder()
                            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                            .build())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(JacksonConverterFactory.create(JacksonConfigurator.build()))
                    .build()
                    .create(PrintServerApi::class.java)
        }

    override fun bindView(view: PrintServerFragment) {
        super.bindView(view)

        view()?.let {
            if (isDeviceRequest) {
                it.showLoading(context.getString(R.string.test_print))
            } else {
                it.hideLoading()
            }
        }

        restoreUiState()
    }

    fun restoreUiState() {
        val view = view() ?: return

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
        view()?.hideLoading()

        super.unbindView(view)
    }

    override fun onFinish() {
        deviceRequest?.dispose()
    }

    fun getDevices(url: String, port: Int) {

        this.url = url
        this.port = port

        val baseUrl: String = getPrintServerUrl(url, port)

        if (!isDeviceRequest) {

            if (HttpUrl.parse(baseUrl) == null) {
                view()?.showError(context.getString(R.string.wrong_url_format))
                return
            }

            isDeviceRequest = true

            view()?.showLoading(context.getString(R.string.connect_in_progress))

            deviceRequest = api.getDeviceTypes()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isDeviceRequest = false
                        view()?.let {
                            it.hideLoading()
                            it.showConnectionState(connectedSuccessful)
                        }
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

    fun getModelsAndDrivers(deviceTypeId: Int) {

        if (!isModelRequest) {

            isModelRequest = true

            view()?.showLoading(context.getString(R.string.connect_in_progress))

            modelRequest = api.getModels(deviceTypeId)
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

        if (!isSettingsRequest) {

            isSettingsRequest = true

            view()?.showLoading(context.getString(R.string.connect_in_progress))

            settingsRequest = api.getDeviceSettings(curDriverId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isSettingsRequest = false
                        view()?.let {
                            it.hideLoading()
                            it.showPrintAvailable(isPrintAvailable())
                        }
                    }
                    .subscribe({ settings ->
                        deviceSettings = settings
                        view()?.let {
                            it.buildSettingsUI(settingsList())
                            it.showConfirm(settings.resultInfo)
                        }
                    }, {
                        view()?.showError(it.toString())
                    })
        }
    }

    fun saveSettings() {
        val settings = deviceSettings ?: return
        if (!isZipSettingsRequest) {
            isZipSettingsRequest = true

            val settingsValues = SettingsValues().apply {
                driverId = driver?.id ?: ""
                modelId = model?.id ?: ""
                typeId = deviceType?.id ?: 0
                boolValues = settings.boolSettings
                stringValues = settings.stringSettings
                listValues = settings.listSettings
            }

            view()?.showLoading(context.getString(R.string.connect_in_progress))

            zipSettingsRequest = api.compressSettings(settingsValues)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isZipSettingsRequest = false
                        view()?.let {
                            it.hideLoading()
                            it.showPrintAvailable(isPrintAvailable())
                        }
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
        if (!isPrintRequest && !zipSettings.isNullOrEmpty()) {
            val printer = PrintServer(api, zipSettings ?: "")
            isPrintRequest = true
            printer.execute(listOf(Task(context.getString(R.string.test_print))))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isPrintRequest = false
                        view()?.let {
                            it.hideLoading()
                            it.showPrintAvailable(isPrintAvailable())
                        }
                    }
                    .subscribe({ response ->
                        printSuccessful = response.isSuccess()
                        view()?.let {
                            it.showConfirm(response.resultInfo)
                            it.showPrintState(printSuccessful)
                        }
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
}
