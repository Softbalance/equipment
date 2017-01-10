package ru.softbalance.equipment.model.printserver.api.model

import com.fasterxml.jackson.annotation.JsonProperty

class PrintDeviceType {

    @JsonProperty("typeId")
    var id = 0

    @JsonProperty("typeName")
    var name = ""

}
