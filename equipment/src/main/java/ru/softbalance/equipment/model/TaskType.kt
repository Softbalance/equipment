package ru.softbalance.equipment.model

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonProperty

enum class TaskType {
    @JsonProperty("String")
    STRING,
    @JsonProperty("BarCode")
    BARCODE,
    @JsonProperty("Image")
    IMAGE,
    @JsonProperty("Registration")
    REGISTRATION,
    @JsonProperty("CloseCheck")
    CLOSE_CHECK,
    @JsonProperty("CancelCheck")
    CANCEL_CHECK,
    @JsonProperty("OpenCheckSell")
    OPEN_CHECK_SELL,
    @JsonProperty("Payment")
    PAYMENT,
    @JsonProperty("OpenCheckReturn")
    OPEN_CHECK_RETURN,
    @JsonProperty("Return")
    RETURN,
    @JsonProperty("CashIncome")
    CASH_INCOME,
    @JsonProperty("CashOutcome")
    CASH_OUTCOME,
    @JsonProperty("ClientContact")
    CLIENT_CONTACT,
    @JsonProperty("Report")
    REPORT,
    @JsonEnumDefaultValue
    @JsonProperty("Unknown")
    UNKNOWN
}