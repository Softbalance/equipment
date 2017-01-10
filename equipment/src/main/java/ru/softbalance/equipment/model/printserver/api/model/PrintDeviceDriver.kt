package ru.softbalance.equipment.model.printserver.api.model

import com.fasterxml.jackson.annotation.JsonProperty

class PrintDeviceDriver {

    @JsonProperty("driverId")
    var id = ""

    @JsonProperty("driverName")
    var name = ""
}
