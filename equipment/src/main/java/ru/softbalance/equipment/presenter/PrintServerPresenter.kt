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
import ru.softbalance.equipment.model.mapping.jackson.JacksonConfigurator
import ru.softbalance.equipment.model.printserver.api.PrintServerApi
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceDriver
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceModel
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceType
import ru.softbalance.equipment.model.printserver.api.response.settings.SettingsResponse
import ru.softbalance.equipment.view.fragment.PrintServerFragment

class PrintServerPresenter(var url: String, var port: Int) : Presenter<PrintServerFragment> () {

    private var isDeviceRequest: Boolean = false
    private var isModelRequest: Boolean = false
    private var isSettingsRequest: Boolean = false
    private var isPrintRequest: Boolean = false
    var connectedSuccessful: Boolean = false

    var settings: String = ""

    var deviceTypes: MutableList<PrintDeviceType>? = null
    var deviceType : PrintDeviceType? = null
    var models: MutableList<PrintDeviceModel>? = null
    private var model: PrintDeviceModel? = null
    var drivers: MutableList<PrintDeviceDriver>? = null
    var driver: PrintDeviceDriver? = null
    private var deviceSettings: SettingsResponse? = null

    private var deviceRequest: Disposable? = null
    private var modelRequest: Disposable? = null
    private var settingsRequest: Disposable? = null

    private var printAvailable:Boolean = false
        get() = connectedSuccessful && deviceSettings != null

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
            it.showPrintAvailable(printAvailable)
        }
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

    private fun requestSettings(driverId: String) {

        if (!isSettingsRequest && context != null && api != null) {

            isSettingsRequest = true

            view()?.showLoading(context?.getString(R.string.connect_in_progress) ?: "")

            settingsRequest = api!!.getDeviceSettings(driverId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose {
                        isSettingsRequest = false
                        view()?.hideLoading()
                        view()?.showPrintAvailable(printAvailable)
                    }
                    .subscribe({
                        deviceSettings = it
                        view()?.showConfirm(it.resultInfo)
                    }, {
                        view()?.showError(it.toString())
                    })
        }
    }

    fun testPrint() {
        // TODO
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
}
