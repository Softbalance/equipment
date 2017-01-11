package ru.softbalance.equipment.model.printserver.api.response

import ru.softbalance.equipment.model.EquipmentResponse
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceDriver
import ru.softbalance.equipment.model.printserver.api.model.PrintDeviceModel

class ModelsResponse : EquipmentResponse() {

    var models = emptyList<PrintDeviceModel>()

    var drivers = emptyList<PrintDeviceDriver>()

}
