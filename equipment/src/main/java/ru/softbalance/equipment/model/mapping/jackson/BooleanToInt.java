package ru.softbalance.equipment.model.mapping.jackson;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({BooleanToInt.TRUE, BooleanToInt.FALSE})
@Retention(RetentionPolicy.SOURCE)
public @interface BooleanToInt {
    int FALSE = 0;
    int TRUE = 1;
}
