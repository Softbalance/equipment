package ru.softbalance.equipment.model

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonProperty

enum class DeviceConnectionType {
    @JsonEnumDefaultValue()
    @JsonProperty("1")
    NETWORK,
    @JsonProperty("2")
    USB
}