package ru.softbalance.equipment.model

open class OfdStatusResponse(val ofdStatus: OfdStatus) : BaseResponse()

class OfdStatus (
    var isError: Boolean = false,
    var unsetDocsCount: Int = 0,
    var errorCode: Int = 0,
    var errorDate: Long = 0,
    var errorText: String = ""
)