package ru.softbalance.equipment.model.printserver.api.response.settings

import com.fasterxml.jackson.annotation.JsonProperty

abstract class TypedSettings<T> : Settings() {

    @JsonProperty
    var value: T? = null
}
