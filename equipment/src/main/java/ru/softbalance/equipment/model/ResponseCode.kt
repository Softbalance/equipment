package ru.softbalance.equipment.model

object ResponseCode {
    val SUCCESS = 0
    val MISSED_PARAMETERS = 1
    val WRONG_PARAMETERS = 2
    val HANDLING_ERROR = 4
    val AUTHORIZATION_ERROR = 8
    val NO_CONNECTION = 16
    val LOGICAL_ERROR = 32
    val INTERNAL_ERROR = 64
}