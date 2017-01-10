package ru.softbalance.equipment.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class Alignment {
    @JsonProperty("Left")
    LEFT,
    @JsonProperty("Center")
    CENTER,
    @JsonProperty("Right")
    RIGHT
}