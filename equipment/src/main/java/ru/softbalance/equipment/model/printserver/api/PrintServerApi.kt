package ru.softbalance.equipment.model.printserver.api

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import ru.softbalance.equipment.model.EquipmentResponse
import ru.softbalance.equipment.model.printserver.api.model.SettingsValues
import ru.softbalance.equipment.model.printserver.api.model.TasksRequest
import ru.softbalance.equipment.model.printserver.api.response.*
import ru.softbalance.equipment.model.printserver.api.response.settings.SettingsResponse
import rx.Single

interface PrintServerApi {

    @POST("/hi")
    fun hi(): Single<MessageResponse>

    @POST("/version")
    fun version(): Single<VersionResponse>

    @POST("/supportDeviceType")
    fun getDeviceTypes(): Single<DevicesResponse>

    @FormUrlEncoded
    @POST("/supportModels")
    fun getModels(@Field("typeId") deviceType: Int): Single<ModelsResponse>

    @FormUrlEncoded
    @POST("/deviceSetting")
    fun getDeviceSettings(@Field("driverId") driverId: String): Single<SettingsResponse>

    @POST("/deviceSetting")
    fun extractDeviceSettings(@Body compressedSettings: RequestBody): Single<SettingsResponse>

    @POST("/deviceSettingZip")
    fun compressSettings(@Body settingsValues: SettingsValues): Single<CompressedSettingsResponse>

    @POST("/taxes")
    fun getTaxes(@Body compressedSettings: RequestBody): Single<TaxesResponse>

    @POST("/execute")
    fun execute(@Body tasksRequest: TasksRequest): Single<EquipmentResponse>
}
