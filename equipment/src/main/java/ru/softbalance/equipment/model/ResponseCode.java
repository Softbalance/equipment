package ru.softbalance.equipment.model;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ResponseCode.SUCCESS,
        ResponseCode.MISSED_PARAMETERS,
        ResponseCode.WRONG_PARAMETERS,
        ResponseCode.HANDLING_ERROR,
        ResponseCode.AUTHORIZATION_ERROR,
        ResponseCode.NO_CONNECTION,
        ResponseCode.LOGICAL_ERROR,
        ResponseCode.INTERNAL_ERROR})
public @interface ResponseCode {

    int SUCCESS = 0;
    int MISSED_PARAMETERS = 1;
    int WRONG_PARAMETERS = 2;
    int HANDLING_ERROR = 4;
    int AUTHORIZATION_ERROR = 8;
    int NO_CONNECTION = 16;
    int LOGICAL_ERROR = 32;
    int INTERNAL_ERROR = 64;

}
