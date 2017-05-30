package ru.softbalance.equipment.model

open class OpenShiftResponse : BaseResponse() {
    val shiftAlreadyOpened: Boolean
       get() = resultInfo.contains("-3837")

    val shiftExpired24Hours: Boolean
        get() = resultInfo.contains("-3822")
}