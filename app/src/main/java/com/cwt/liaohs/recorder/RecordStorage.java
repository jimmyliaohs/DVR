package com.cwt.liaohs.recorder;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordStorage {

    private static final String RECORD_DIR = "/mnt/sdcard";
    private static final String RECORD_PATH = "record/";
    private static final String FRONT_RECORD = "_front";
    private static final String BACK_RECORD = "_back";
    private static final String RECORD_SUFFIX = ".mp4";

    private static final float RECORD_SD_MIN_FREE_PERCENT = 0.1f;
    private static final float RECORD_SD_MIN_FREE_STORAGE = 100f;


    public static boolean isSdcardAvailable() {
        return Environment.getExternalStorageState(new File(RECORD_DIR)).equals(
                Environment.MEDIA_MOUNTED);
    }

    public static String getSdcadDir() {
        return RECORD_DIR;
//        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getVedioDir() {
        File file = new File(getSdcadDir() + File.separator + RECORD_PATH);

        if (!file.exists()) {
            file.mkdirs();
        }

        return file.getAbsolutePath();
    }

    @SuppressLint("SimpleDateFormat")
    public static String getCurrentFrontRecordPath() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        return getVedioDir() + File.separator + df.format(new Date()) +FRONT_RECORD+ RECORD_SUFFIX;
//        return "/sdcard/record/"+df.format(new Date()) + VIDEO_SUFFIX;
    }

    public static String getCurrentBackRecordPath() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        return getVedioDir() + File.separator + df.format(new Date()) +BACK_RECORD+ RECORD_SUFFIX;
//        return "/sdcard/record/"+df.format(new Date()) + VIDEO_SUFFIX;
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
     * @param fileName
     * @return
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.v("cwt","delete "+fileName+" success");
                return true;
            } else {
                Log.v("cwt","delete "+fileName+" fail");
                return false;
            }
        } else {
            Log.v("cwt",fileName+" not exit");
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
     * 删除录像
     *
     * @param dbHelper
     */
    public static void checkSdcard(RecordDbHelper dbHelper, ISdcardCheckoutListener listener) throws Exception {

        if (listener == null) {
            throw new Exception("onSdcardCheckListener is null");
        }

        if (dbHelper == null) {
            throw new Exception("RecordDbHelper is null");
        }

        if (!isSdcardAvailable()) {
            listener.sdcardNoMounted();
            return;
        }

        long start = System.currentTimeMillis();

        float sdFree = getSdcardAvailableStorage();
        float sdTotal = getSdcardTotalStorage();
        boolean flag = true;

        while (sdFree < sdTotal * RECORD_SD_MIN_FREE_PERCENT || sdFree < RECORD_SD_MIN_FREE_STORAGE) {

            Log.v("cwt", "storage not enough");

            if (flag) {
                listener.sdcardStorageNotEnough();
                flag = false;
            }

            int oldestUnlockRecordId = dbHelper.getOldestUnlockRecordItemId();

            if (oldestUnlockRecordId != -1) {
                String oldestUnlockRecordName = dbHelper.getRecordItemNameById(oldestUnlockRecordId);

                if (oldestUnlockRecordName != null && !oldestUnlockRecordName.isEmpty()) {
                    File oldestUnlockFile = new File(oldestUnlockRecordName);

                    if (oldestUnlockFile.exists() && oldestUnlockFile.isFile()) {
                        oldestUnlockFile.delete();
                        dbHelper.deleteRecordItemById(oldestUnlockRecordId);
                    }
                }
            } else {
                int oldestRecordId = dbHelper.getOldestRecordItemId();

                if (oldestRecordId != -1) {
                    String oldestRecordName = dbHelper.getRecordItemNameById(oldestRecordId);
                    if (oldestRecordName != null && !oldestRecordName.isEmpty()) {
                        File oldestFile = new File(oldestRecordName);
                        if (oldestFile.exists() && oldestFile.isFile()) {
                            oldestFile.delete();
                            dbHelper.deleteRecordItemById(oldestRecordId);
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

        long end = System.currentTimeMillis();

        Log.v("cwt", "interval:" + (end - start));
        listener.sdcardStorageEnough();

    }


}
