package ru.softbalance.equipment.model

object ResponseCode {
    const val SUCCESS = 0
    const val MISSED_PARAMETERS = 1
    const val WRONG_PARAMETERS = 2
    const val HANDLING_ERROR = 4
    const val AUTHORIZATION_ERROR = 8
    const val NO_CONNECTION = 16
    const val LOGICAL_ERROR = 32
    const val INTERNAL_ERROR = 64
}