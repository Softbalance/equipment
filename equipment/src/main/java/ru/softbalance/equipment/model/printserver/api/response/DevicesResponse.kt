package ru.softbalance.equipment.model.printserver.api.response

import com.fasterxml.jackson.annotation.JsonProperty
import ru.softbalance.equipment.model.EquipmentResponse
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceType

class DevicesResponse : EquipmentResponse() {

    @JsonProperty("supportDeviceType")
    var deviceTypes = emptyList<PrintDeviceType>()
}
