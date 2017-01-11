package ru.softbalance.equipment.model;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ReportType.REPORT_Z,
        ReportType.REPORT_X,
        ReportType.REPORT_DEPARTMENT,
        ReportType.REPORT_CASHIERS,
        ReportType.REPORT_HOURS})
public @interface ReportType {
    int REPORT_Z = 1;
    int REPORT_X = 2;
    int REPORT_DEPARTMENT = 7;
    int REPORT_CASHIERS = 8;
    int REPORT_HOURS = 10;
}
