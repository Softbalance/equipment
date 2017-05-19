package ru.softbalance.equipment.model.printserver

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.softbalance.equipment.BuildConfig
import ru.softbalance.equipment.model.*
import ru.softbalance.equipment.model.mapping.jackson.JacksonConfigurator
import ru.softbalance.equipment.model.printserver.api.PrintServerApi
import ru.softbalance.equipment.model.printserver.api.model.CompressedSettings
import ru.softbalance.equipment.model.printserver.api.model.TasksRequest
import ru.softbalance.equipment.model.printserver.api.response.TaxesResponse
import ru.softbalance.equipment.toHttpUrl
import rx.Observable
import rx.Single

class PrintServer(url: String, port: Int, val settings: String) : EcrDriver {

    override fun getSerial(finishAfterExecute: Boolean): Single<String> {
        return Single.just ("getInfo is not implemented for PrintServer")
    }

    val api: PrintServerApi

    init {
        api = Retrofit.Builder()
                .baseUrl(url.toHttpUrl(port))
                .client(getClient())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(JacksonConfigurator.build()))
                .build()
                .create(PrintServerApi::class.java)
    }

    private fun getClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }
        return builder.build()
    }

    override fun execute(tasks: List<Task>, finishAfterExecute: Boolean): Single<EquipmentResponse> {
        return Single.fromCallable { TasksRequest(tasks, settings) }
                .flatMap { api.execute(it).toSingle() }
    }

    fun getTaxes(): Observable<TaxesResponse> {
        return api.getTaxes(CompressedSettings.create(settings))
    }

    override fun getSessionState(finishAfterExecute: Boolean): Single<SessionStateResponse> {
        return Single.just(SessionStateResponse()
                .apply { resultInfo = "getSessionState is not implemented for PrintServer" })
    }

    override fun openShift(finishAfterExecute: Boolean): Single<OpenShiftResponse> {
        return Single.just(OpenShiftResponse()
                .apply { resultInfo = "getSessionState is not implemented for PrintServer" })
    }

    override fun finish() {
        // do nothing since it is not required here
    }
}