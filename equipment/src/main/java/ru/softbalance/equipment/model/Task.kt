package ru.softbalance.equipment.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class Task {
    var data = ""

    var type = TaskType.STRING

    @JsonProperty("param")
    var params: Parameters? = null
}
