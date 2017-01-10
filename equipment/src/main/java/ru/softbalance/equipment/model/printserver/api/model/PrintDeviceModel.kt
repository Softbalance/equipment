package ru.softbalance.equipment.model.printserver.api.model

import com.fasterxml.jackson.annotation.JsonProperty

class PrintDeviceModel {

    @JsonProperty("modelId")
    var id = ""

    @JsonProperty("modelName")
    var name = ""

    var supportDriverCodes = emptyList<String>()

}
