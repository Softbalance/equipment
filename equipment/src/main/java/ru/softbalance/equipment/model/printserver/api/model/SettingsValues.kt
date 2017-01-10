package ru.softbalance.equipment.model.printserver.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import ru.softbalance.equipment.model.printserver.api.response.settings.TypedSettings

class SettingsValues {

    var typeId = 0

    @JsonProperty
    var modelId = ""

    @JsonProperty
    var driverId = ""

    @JsonProperty("settingBool")
    @JsonSerialize(typing = JsonSerialize.Typing.STATIC)
    var boolValues = emptyList<TypedSettings<Boolean>>()

    @JsonProperty("settingString")
    @JsonSerialize(typing = JsonSerialize.Typing.STATIC)
    var stringValues = emptyList<TypedSettings<String>>()

    @JsonProperty("settingList")
    @JsonSerialize(typing = JsonSerialize.Typing.STATIC)
    var listValues = emptyList<TypedSettings<Int>>()

}
