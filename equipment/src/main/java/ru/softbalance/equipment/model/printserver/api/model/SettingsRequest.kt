package ru.softbalance.equipment.model.printserver.api.model

import com.fasterxml.jackson.annotation.JsonProperty

open class SettingsRequest {

    @JsonProperty("settingZip")
    var settings: String = ""
}
