package ru.softbalance.equipment.model

enum class TaskType(val value : String) {
    STRING("String"),
    BARCODE("BarCode"),
    IMAGE("Image"),
    REGISTRATION("Registration"),
    CLOSE_CHECK("CloseCheck"),
    CANCEL_CHECK("CancelCheck"),
    OPEN_CHECK_SELL("OpenCheckSell"),
    PAYMENT("Payment"),
    OPEN_CHECK_RETURN("OpenCheckReturn"),
    RETURN("Return"),
    CASH_INCOME("CashIncome"),
    CASH_OUTCOME("CashOutcome"),
    CLIENT_CONTACT("ClientContact")
}