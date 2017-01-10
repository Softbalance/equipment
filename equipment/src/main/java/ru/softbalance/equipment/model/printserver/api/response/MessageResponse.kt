package ru.softbalance.equipment.model.printserver.api.response

import com.fasterxml.jackson.annotation.JsonProperty
import ru.softbalance.equipment.model.EquipmentResponse

class MessageResponse : EquipmentResponse() {

    @JsonProperty("value")
    var message = ""
}
