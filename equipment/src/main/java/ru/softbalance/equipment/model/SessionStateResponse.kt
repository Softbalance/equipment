package ru.softbalance.equipment.model

open class SessionStateResponse {

    var frSessionState = FrSessionState()

    var resultCode = ResponseCode.HANDLING_ERROR

    var resultInfo = ""

    fun isSuccess() = resultCode == ResponseCode.SUCCESS
}

data class FrSessionState (
    var shiftOpen: Boolean = false,
    var shiftNumber: Int = 0,
    var paperExists: Boolean = false
)
