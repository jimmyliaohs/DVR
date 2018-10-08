package com.cwt.liaohs.recorder;

import android.content.Context;
import android.content.SharedPreferences;

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

    public static void setVoiceDisable(boolean value) {
        setValue("disable_voice", value);
    }

    public static boolean isVoiceDisable() {
        return getBoolean("disable_voice", false);
    }

    public static void setLock(boolean value) {
        setValue("is_lock", value);
    }

    public static boolean isLock() {
        return getBoolean("is_lock", false);
    }

    public static void setRecordInterval(int value) {
        setValue("record_interval", value);
    }

    public static int getRecordInterval() {
        return getInt("record_interval", 1);
    }

    public static void setRecordBitRate(int value) {
        setValue("record_bitrate", value);
    }

    public static int getRecordBitRate() {
        return getInt("record_bitrate", 1);
    }

    public static void setCrashSenseLevel(int value) {
        setValue("crash_sense_level", value);
    }

    public static int getCrashSenseLevel() {
        return getInt("crash_sense_level", 8);
    }

    public static boolean isCrashToggle(){
        return getBoolean("crash_toggle", true);
    }

    public static void setCrashToggle(boolean value){
        setValue("crash_toggle", value);
    }

    public static boolean isFrontCrashedOn(){
        return getBoolean("is_front_crashed", false);
    }

    public static void setFrontCrashed(boolean value){
        setValue("is_front_crashed", value);
    }

    public static boolean isBackCrashedOn(){
        return getBoolean("is_back_crashed", false);
    }

    public static void setBackCrashed(boolean value){
        setValue("is_back_crashed", value);
    }

    public static void setQuality720p(boolean value){
        setValue("is_quality_720p", value);
    }

    public static boolean isQuality720p(){
        return getBoolean("is_quality_720p", true);
    }

    public static boolean isFrontWechatVedioOn(){
        return getBoolean("is_front_wechat_vedio", false);
    }

    public static void setFrontWechatVedio(boolean value){
        setValue("is_front_wechat_vedio", value);
    }

    public static boolean isBackWechatVedioOn(){
        return getBoolean("is_back_wechat_vedio", false);
    }

    public static void setBackWechatVedio(boolean value){
        setValue("is_back_wechat_vedio", value);
    }
}
