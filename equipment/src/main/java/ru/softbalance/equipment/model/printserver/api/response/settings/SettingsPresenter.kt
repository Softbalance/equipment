package ru.softbalance.equipment.model.printserver.api.response.settings

import com.fasterxml.jackson.annotation.JsonProperty

open class SettingsPresenter<T, D : Dependency<T>> : TypedSettings<T>() {

    @JsonProperty
    var title = ""

    @JsonProperty
    var sort = 0

    @JsonProperty("depend")
    var dependencies = emptyList<D>()

}
