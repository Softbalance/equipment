package ru.softbalance.equipment.model.printserver

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.softbalance.equipment.BuildConfig
import ru.softbalance.equipment.R
import ru.softbalance.equipment.model.*
import ru.softbalance.equipment.model.mapping.jackson.JacksonConfigurator
import ru.softbalance.equipment.model.printserver.api.PrintServerApi
import ru.softbalance.equipment.model.printserver.api.model.CompressedSettings
import ru.softbalance.equipment.model.printserver.api.model.TasksRequest
import ru.softbalance.equipment.toHttpUrl
import rx.Single
import rx.schedulers.Schedulers

class PrintServer(
    val context: Context,
    url: String,
    port: Int,
    val settings: String) : EcrDriver {

    override fun getSerial(finishAfterExecute: Boolean): Single<SerialResponse> {
        return Single.just(SerialResponse().apply {
            resultInfo = "getInfo is not implemented for PrintServer"
        })
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

    override fun execute(tasks: List<Task>,
                         finishAfterExecute: Boolean): Single<EquipmentResponse> {
        return Single.fromCallable { TasksRequest(tasks, settings) }
            .flatMap { api.execute(it) }
    }

    override fun getTaxes(finishAfterExecute: Boolean): Single<List<Tax>> {
        return api.getTaxes(CompressedSettings.create(settings))
            .subscribeOn(Schedulers.io())
            .map { response ->
                if (!response.isSuccess()) {
                    val message = context.getString(R.string.equipment_lib_obtaining_taxes_failed, response.resultInfo)
                    throw RuntimeException(message)
                } else if (response.taxes.isEmpty()) {
                    throw RuntimeException(R.string.equipment_lib_error_taxes_empty.toStrRes())
                }
                response.taxes
            }
    }

    override fun getSessionState(finishAfterExecute: Boolean): Single<SessionStateResponse> {
        return Single.just(SessionStateResponse()
            .apply { resultInfo = "getSessionState is not implemented for PrintServer" })
    }

    override fun openShift(finishAfterExecute: Boolean): Single<OpenShiftResponse> {
        return Single.just(OpenShiftResponse()
            .apply { resultInfo = "getSessionState is not implemented for PrintServer" })
    }

    override fun getOfdStatus(finishAfterExecute: Boolean): Single<OfdStatusResponse> {
        val accessException =
            IllegalAccessException("The method isn't supported")
        return Single.error<OfdStatusResponse>(accessException)
    }

    override fun finish() {
        // do nothing since it is not required here
    }

    private fun Int.toStrRes() = context.getString(this)
}