package ru.softbalance.equipment.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class ReportType {
    @JsonProperty("1")
    ReportZ,
    @JsonProperty("2")
    ReportX,
    @JsonProperty("7")
    ReportDepartment,
    @JsonProperty("8")
    ReportCashiers,
    @JsonProperty("10")
    ReportHours
}