package ru.softbalance.equipment.model

open class BaseResponse {

    var resultCode = ResponseCode.HANDLING_ERROR

    var resultInfo = ""

    fun isSuccess() = resultCode == ResponseCode.SUCCESS
}
