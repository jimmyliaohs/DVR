package com.cwt.liaohs.recorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.cwt.liaohs.cwtdvrplus.ContextUtil;

public class RecordSettings {
    private static SharedPreferences preferences;

    private static SharedPreferences getSharedPref() {
        if (preferences == null) {
            preferences = ContextUtil.getInstance().getSharedPreferences(
                    "record_settings", Context.MODE_PRIVATE);
        }
        return preferences;
    }

    private static void setValue(String key, Object value) {
        if (value instanceof Boolean) {
            getSharedPref().edit().putBoolean(key, (Boolean) value).commit();
        } else if (value instanceof Integer) {
            getSharedPref().edit().putInt(key, (Integer) value).commit();
        } else if (value instanceof String) {
            getSharedPref().edit().putString(key, (String) value).commit();
        }
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        return getSharedPref().getBoolean(key, defaultValue);
    }

    private static int getInt(String key, int defaultValue) {
        return getSharedPref().getInt(key, defaultValue);
    }

    public static void setPreview(boolean value) {
        setValue("is_previewing", value);
    }

    public static boolean isPreview() {
        return getBoolean("is_previewing", false);
    }

    public static void setRecord(boolean value) {
        setValue("is_recording", value);
    }

    public static boolean isRecord() {
        return getBoolean("is_recording", false);
    }


    public static void setLock(int value) {
        setValue("is_lock", value);
    }

    public static int getLock() {
        return getInt("is_lock", 0);
    }
}
