package ru.softbalance.equipment.model

import android.content.Context
import com.fasterxml.jackson.databind.JsonMappingException
import retrofit2.adapter.rxjava.HttpException
import ru.softbalance.equipment.R
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ExceptionHandler(context: Context) {

    val context: Context = context.applicationContext

    fun getUserFriendlyMessage(throwable: Throwable) : String {
        val message: String
        val details = if (throwable.message.isNullOrEmpty()) throwable.toString() else throwable.message

        if (throwable is UnknownHostException
                || throwable is SocketTimeoutException
                || throwable is HttpException
                || throwable is RuntimeException && (throwable.cause is HttpException || throwable.cause is ConnectException)
                || throwable is ConnectException) {
            message = context.getString(R.string.equipment_error_network_with_description, details)
        } else if (throwable is JsonMappingException) {
            message = context.getString(R.string.equipment_error_mapping_with_description, details)
        } else {
            message = context.getString(R.string.equipment_error_unknown_with_description, details)
        }

        return message
    }
}
