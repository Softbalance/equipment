package ru.softbalance.equipment.model.printserver.api.response.settings

import com.fasterxml.jackson.annotation.JsonProperty

class StringSettingsPresenter : SettingsPresenter<String, StringSettingsPresenter.StringDependency>() {

    @JsonProperty
    var isNumber: Boolean = false

    @JsonProperty
    var maxLength: Int = 0

    class StringDependency : Dependency<String>()
}
