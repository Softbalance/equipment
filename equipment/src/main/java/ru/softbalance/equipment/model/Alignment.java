package ru.softbalance.equipment.model;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({Alignment.LEFT,
        Alignment.CENTER,
        Alignment.RIGHT})
public @interface Alignment {
    String LEFT = "Left";
    String CENTER = "Center";
    String RIGHT = "Right";
}
