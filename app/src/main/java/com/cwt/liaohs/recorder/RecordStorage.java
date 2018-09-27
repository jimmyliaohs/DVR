package com.cwt.liaohs.recorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;

import com.cwt.liaohs.cwtdvrplus.ContextUtil;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordStorage {

    private static final String RECORD_DIR = "/mnt/sdcard";
    private static final String RECORD_ROOT = "Megafone";
    private static final String RECORD_PATH = "Record/";
    private static final String PIC_PATH = "Pic/";
    private static final String FRONT_RECORD = "_front_";
    private static final String BACK_RECORD = "_back_";
    private static final String LOCKED = "_lock";
    private static final String CRASH = "_crash";
    private static final String MIN = "min";
    private static final String RECORD_SUFFIX = ".ts";//.mp4
    private static final String PIC_SUFFIX = ".jpg";

    private static final float RECORD_SD_MIN_FREE_PERCENT = 0.1f;
    private static final float RECORD_SD_MIN_FREE_STORAGE = 100f;


    public static boolean isSdcardAvailable() {
        return !TextUtils.isEmpty(getSdcadDir());
//        return Environment.getExternalStorageState(new File(/*RECORD_DIR*/getSdcadDir())).equals(
//                Environment.MEDIA_MOUNTED);
    }

    public static String getSdcadDir() {
        StorageManager mStorageManager = (StorageManager) ContextUtil.getInstance().getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);

            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (removable) {
                    return path;
                }
            }
        } catch (Exception e) {
            Log.d("cwt","getSdcadDir()-->e:"+e.toString());
        }
        return null;
    }

    /*
    public static String getSdcadDir() {
        return RECORD_DIR;
    }
    */

    public static String getPicDir() {
        File file = new File(getSdcadDir() + File.separator + RECORD_ROOT + File.separator + PIC_PATH);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }

        if (!file.exists()) {
            file.mkdir();
        }

        return file.getAbsolutePath();
    }

    public static String getVedioDir() {
        File file = new File(getSdcadDir() + File.separator + RECORD_ROOT + File.separator + RECORD_PATH);

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }

        if (!file.exists()) {
            file.mkdir();
        }

        return file.getAbsolutePath();
    }

    @SuppressLint("SimpleDateFormat")
    public static String getCurrentFrontRecordPath(boolean isLocked) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

        if (RecordSettings.isFrontCrashedOn()) {
            return getVedioDir() + File.separator + df.format(new Date()) + FRONT_RECORD + RecordSettings.getRecordInterval() + MIN + LOCKED + CRASH + RECORD_SUFFIX;
        } else {
            if (isLocked) {
                return getVedioDir() + File.separator + df.format(new Date()) + FRONT_RECORD + RecordSettings.getRecordInterval() + MIN + LOCKED + RECORD_SUFFIX;
            }
        }

        return getVedioDir() + File.separator + df.format(new Date()) + FRONT_RECORD + RecordSettings.getRecordInterval() + MIN + RECORD_SUFFIX;
//        return "/sdcard/record/"+df.format(new Date()) + VIDEO_SUFFIX;
    }

    public static String getCurrentBackRecordPath(boolean isLocked) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

        if (RecordSettings.isBackCrashedOn()) {
            return getVedioDir() + File.separator + df.format(new Date()) + BACK_RECORD + RecordSettings.getRecordInterval() + MIN + LOCKED + CRASH + RECORD_SUFFIX;
        } else {
            if (isLocked) {
                return getVedioDir() + File.separator + df.format(new Date()) + BACK_RECORD + RecordSettings.getRecordInterval() + MIN + LOCKED + RECORD_SUFFIX;
            }
        }

        return getVedioDir() + File.separator + df.format(new Date()) + BACK_RECORD + RecordSettings.getRecordInterval() + MIN + RECORD_SUFFIX;
//        return "/sdcard/record/"+df.format(new Date()) + VIDEO_SUFFIX;
    }

    public static String getCurrentPicPath() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        return getPicDir() + File.separator + df.format(new Date()) + PIC_SUFFIX;
    }

    /**
     * @return
     */
    public static long getSdcardTotalStorage() {
        StatFs stat;
        try {
            stat = new StatFs(getSdcadDir());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return (blockSize * totalBlocks) / (1024 * 1024);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * @return
     */
    public static long getSdcardAvailableStorage() {
        StatFs stat;
        try {
            stat = new StatFs(getSdcadDir());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return (blockSize * availableBlocks) / (1024 * 1024);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 删除单个文件
     *
     * @param fileName
     * @return
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.v("cwt", "delete " + fileName + " success");
                return true;
            } else {
                Log.v("cwt", "delete " + fileName + " fail");
                return false;
            }
        } else {
            Log.v("cwt", fileName + " not exit");
            return false;
        }
    }

    /**
     * 递归删除文件
     *
     * @param path
     * @return
     */
    public static boolean recursionDeleteFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        if (file.isFile()) {
            file.delete();
            return true;
        } else {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                String root = files[i].getAbsolutePath();
                recursionDeleteFile(root);
            }
            file.delete();
            return true;
        }

    }

    /**
     * 检查存储情况
     */
    public static void checkSdcard(ISdcardCheckoutListener listener) throws Exception {

        if (listener == null) {
            throw new Exception("onSdcardCheckListener is null");
        }

        if (!isSdcardAvailable()) {
            Log.d("cwt","checkSdcard()--->listener.sdcardNoMounted()");
            listener.sdcardNoMounted();
            return;
        }

        float sdFree = getSdcardAvailableStorage();
        float sdTotal = getSdcardTotalStorage();
        boolean flag = true;

        while (sdFree < sdTotal * RECORD_SD_MIN_FREE_PERCENT || sdFree < RECORD_SD_MIN_FREE_STORAGE) {
            Log.v("cwt", "storage not enough");

            if (flag) {
                listener.sdcardStorageNotEnough();
                flag = false;
            }

            int oldestUnlockRecordId = RecordDatabaseManager.getInstance().getOldestUnlockRecordItemId();
            if (oldestUnlockRecordId != -1) {
                String oldestUnlockRecordName = RecordDatabaseManager.getInstance().getRecordItemNameById(oldestUnlockRecordId);

                if (oldestUnlockRecordName != null && !oldestUnlockRecordName.isEmpty()) {
                    File oldestUnlockFile = new File(oldestUnlockRecordName);

                    if (oldestUnlockFile.exists() && oldestUnlockFile.isFile()) {
                        oldestUnlockFile.delete();
                        RecordDatabaseManager.getInstance().deleteRecordItemById(oldestUnlockRecordId);
                    }
                }
            } else {
                int oldestRecordId = RecordDatabaseManager.getInstance().getOldestRecordItemId();

                if (oldestRecordId != -1) {
                    String oldestRecordName = RecordDatabaseManager.getInstance().getRecordItemNameById(oldestRecordId);
                    if (oldestRecordName != null && !oldestRecordName.isEmpty()) {
                        File oldestFile = new File(oldestRecordName);
                        if (oldestFile.exists() && oldestFile.isFile()) {
                            oldestFile.delete();
                            RecordDatabaseManager.getInstance().deleteRecordItemById(oldestRecordId);
                        }
                    }
                } else {
                    recursionDeleteFile(getVedioDir());
                    sdFree = getSdcardAvailableStorage();

                    if (sdFree < sdTotal * RECORD_SD_MIN_FREE_PERCENT || sdFree < RECORD_SD_MIN_FREE_STORAGE) {
                        //notice format sdcard
                    }
                }

            }

            sdFree = getSdcardAvailableStorage();
        }
        listener.sdcardStorageEnough();
    }

}
