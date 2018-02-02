package ru.softbalance.equipment.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Task {
    var data: String = ""

    @TaskType
    var type: String = TaskType.STRING

    @JsonProperty("Param")
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES])
    var param: Parameters = Parameters()
}
