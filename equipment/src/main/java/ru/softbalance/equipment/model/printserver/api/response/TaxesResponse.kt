package ru.softbalance.equipment.model.printserver.api.response

import ru.softbalance.equipment.model.EquipmentResponse
import ru.softbalance.equipment.model.printserver.api.model.Tax

class TaxesResponse : EquipmentResponse() {

    var taxes = emptyList<Tax>()

}
