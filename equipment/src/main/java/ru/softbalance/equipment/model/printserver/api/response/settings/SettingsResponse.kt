package ru.softbalance.equipment.model.printserver.api.response.settings

import com.fasterxml.jackson.annotation.JsonProperty
import ru.softbalance.equipment.model.EquipmentResponse

class SettingsResponse : EquipmentResponse() {

    var driverId = ""

    var modelId = ""

    @JsonProperty("typeBool")
    var boolSettings = emptyList<BooleanSettingsPresenter>()

    @JsonProperty("typeString")
    var stringSettings = emptyList<StringSettingsPresenter>()

    @JsonProperty("typeList")
    var listSettings = emptyList<ListSettingsPresenter>()

}
