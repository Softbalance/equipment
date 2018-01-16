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
        TaskType.REPORT,
        TaskType.SYNC_TIME,
        TaskType.PRINT_HEADER,
        TaskType.PRINT_FOOTER,
        TaskType.CUT})
public @interface TaskType {

    String STRING = "string";
    String BARCODE = "barcode";
    String IMAGE = "image";
    String REGISTRATION = "registration";
    String CLOSE_CHECK = "closecheck";
    String CANCEL_CHECK = "cancelcheck";
    String OPEN_CHECK_SELL = "openchecksell";
    String PAYMENT = "payment";
    String OPEN_CHECK_RETURN = "opencheckreturn";
    String RETURN = "return";
    String CASH_INCOME = "cashincome";
    String CASH_OUTCOME = "cashoutcome";
    String CLIENT_CONTACT = "clientcontact";
    String REPORT = "report";
    String SYNC_TIME = "synctime";
    String PRINT_HEADER = "printheader";
    String PRINT_SLIP = "printslip";
    String PRINT_FOOTER = "printfooter";
    String CUT = "cut";
}
