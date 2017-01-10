package ru.softbalance.equipment.model.printserver.api.response.settings

import com.fasterxml.jackson.annotation.JsonProperty

abstract class Dependency<T> {

    @JsonProperty("depend")
    var settingsIds = emptyList<String>()

    @JsonProperty("value")
    var values = emptyList<T>()

    @JsonProperty("visible")
    var isVisible = false
}
