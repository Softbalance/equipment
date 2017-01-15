package ru.softbalance.equipment.view;

import android.content.Context;
import android.support.annotation.MenuRes;
import android.support.v7.widget.PopupMenu;
import android.view.MenuInflater;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ViewUtils {

    private ViewUtils() {
    }


    public static void enableIcons(PopupMenu popupMenu) {
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper
                            .getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod(
                            "setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds {@link PopupMenu}
     * @param context Context the popup menu is running in, through which it
     *        can access the current theme, resources, etc.
     * @param anchor Anchor view for this popup. The popup will appear below
     *        the anchor if there is room, or above it if there is not.
     * @param menu Menu resource id for inflate menu from resources. Can be 0 for ignore inflating
     * @param enableIcons If flag {@code enableIcons} is true, then popup menu will have been built with icons
     */
    public static PopupMenu createPopupMenu(Context context, View anchor, @MenuRes int menu, boolean enableIcons) {
        PopupMenu popupMenu = new PopupMenu(context, anchor);

        if (menu > 0) {
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(menu, popupMenu.getMenu());
        }

        if (enableIcons) {
            enableIcons(popupMenu);
        }

        return popupMenu;
    }
}
