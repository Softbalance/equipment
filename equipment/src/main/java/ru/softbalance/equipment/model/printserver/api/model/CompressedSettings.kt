package ru.softbalance.equipment.model.printserver.api.model

import okhttp3.MediaType
import okhttp3.RequestBody

object CompressedSettings {
    fun create(settings: String?): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), "settingZip=" + settings)
    }
}