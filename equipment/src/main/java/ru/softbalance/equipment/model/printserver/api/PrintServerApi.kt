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
import rx.Observable

interface PrintServerApi {

    @POST("/hi")
    fun hi(): Observable<MessageResponse>

    @POST("/version")
    fun version(): Observable<VersionResponse>

    @POST("/supportDeviceType")
    fun getDeviceTypes(): Observable<DevicesResponse>

    @FormUrlEncoded
    @POST("/supportModels")
    fun getModels(@Field("typeId") deviceType: Int): Observable<ModelsResponse>

    @FormUrlEncoded
    @POST("/deviceSetting")
    fun getDeviceSettings(@Field("driverId") driverId: String): Observable<SettingsResponse>

    @POST("/deviceSetting")
    fun extractDeviceSettings(@Body compressedSettings: RequestBody): Observable<SettingsResponse>

    @POST("/deviceSettingZip")
    fun compressSettings(@Body settingsValues: SettingsValues): Observable<CompressedSettingsResponse>

    @FormUrlEncoded
    @POST("/taxes")
    fun getTaxes(@Field("driverId") driverId: String): Observable<TaxesResponse>

    @POST("/execute")
    fun execute(@Body tasksRequest: TasksRequest): Observable<EquipmentResponse>
}
