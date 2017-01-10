package ru.softbalance.equipment.model.printserver.api.response.settings

import com.fasterxml.jackson.annotation.JsonProperty

class ListSettingsPresenter : SettingsPresenter<Int, ListSettingsPresenter.IntegerDependency>() {

    @JsonProperty("list")
    var values = emptyList<ListValue>()

    class ListValue {
        @JsonProperty
        var title = ""

        @JsonProperty
        var valueId: Int = 0

    }

    class IntegerDependency : Dependency<Int>()
}
