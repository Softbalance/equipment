package ru.softbalance.equipment.model;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({TaskType.STRING,
        TaskType.BARCODE,
        TaskType.IMAGE,
        TaskType.REGISTRATION,
        TaskType.CLOSE_CHECK,
        TaskType.CANCEL_CHECK,
        TaskType.OPEN_CHECK_SELL,
        TaskType.PAYMENT,
        TaskType.OPEN_CHECK_RETURN,
        TaskType.RETURN,
        TaskType.CASH_INCOME,
        TaskType.CASH_OUTCOME,
        TaskType.CLIENT_CONTACT,
        TaskType.REPORT})
public @interface TaskType {

    String STRING = "String";
    String BARCODE = "BarCode";
    String IMAGE = "Image";
    String REGISTRATION = "Registration";
    String CLOSE_CHECK = "CloseCheck";
    String CANCEL_CHECK = "CancelCheck";
    String OPEN_CHECK_SELL = "OpenCheckSell";
    String PAYMENT = "Payment";
    String OPEN_CHECK_RETURN = "OpenCheckReturn";
    String RETURN = "Return";
    String CASH_INCOME = "CashIncome";
    String CASH_OUTCOME = "CashOutcome";
    String CLIENT_CONTACT = "ClientContact";
    String REPORT = "Report";
}
