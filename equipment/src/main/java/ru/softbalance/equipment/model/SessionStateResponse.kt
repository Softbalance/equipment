package ru.softbalance.equipment.model

open class SessionStateResponse : BaseResponse() {
    var frSessionState = FrSessionState()
}

data class FrSessionState (
    var shiftOpen: Boolean = false,
    var shiftNumber: Int = 0,
    var paperExists: Boolean = false
)
