package com.cwt.liaohs.recorder;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by liaohs on 2018/9/19.
 */

public class RecordDatabaseManager {

    private AtomicInteger mOpenCounter = new AtomicInteger();

    private static RecordDatabaseManager instance;
    private static RecordDbHelper mRecordDbHelper;
    private SQLiteDatabase mDatabase;

    private static final String RECORD_TABLE_NAME = "record";
    private static final String RECORD_COL_ID = "_id";
    private static final String RECORD_COL_NAME = "name";
    private static final String RECORD_COL_LOCK = "lock";
    private static final String RECORD_COL_RESOLUTION = "resolution";

    private static final String[] RECORD_COL_PROJECTION = new String[]{
            RECORD_COL_ID, RECORD_COL_NAME, RECORD_COL_LOCK,
            RECORD_COL_RESOLUTION,};

    public static synchronized void initializeInstance(RecordDbHelper helper) {
        if (instance == null) {
            instance = new RecordDatabaseManager();
            mRecordDbHelper = helper;
        }
    }

    public static synchronized RecordDatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(RecordDatabaseManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }
        return instance;
    }

    public synchronized SQLiteDatabase openDatabase() {
        if (mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = mRecordDbHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        if (mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();
        }
    }

    public int addRecordItem(RecordItem recordItem) {
        ContentValues values = new ContentValues();
        values.put(RECORD_COL_NAME, recordItem.getRecord_name());
        values.put(RECORD_COL_LOCK, recordItem.getRecord_lock());
        values.put(RECORD_COL_RESOLUTION, recordItem.getRecord_resolution());
        long rowId = openDatabase().insert(RECORD_TABLE_NAME, null, values);
        closeDatabase();
        return (int) rowId;
    }

    public RecordItem getRecordItemById(int id) {
        Cursor cursor = openDatabase().query(RECORD_TABLE_NAME, RECORD_COL_PROJECTION,
                RECORD_COL_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();
        RecordItem recordItem = new RecordItem();
        recordItem.setRecord_id(cursor.getInt(0));
        recordItem.setRecord_name(cursor.getString(1));
        recordItem.setRecord_lock(cursor.getInt(2));
        recordItem.setRecord_resolution(cursor.getInt(3));
        closeDatabase();
        cursor.close();
        return recordItem;
    }

    public RecordItem getRecordItemByName(String name) {
        Cursor cursor = openDatabase().query(RECORD_TABLE_NAME, RECORD_COL_PROJECTION,
                RECORD_COL_NAME + "=?", new String[]{name}, null, null,
                null, null);
        if (cursor != null)
            cursor.moveToFirst();
        RecordItem recordItem = new RecordItem();
        recordItem.setRecord_id(cursor.getInt(0));
        recordItem.setRecord_name(cursor.getString(1));
        recordItem.setRecord_lock(cursor.getInt(2));
        recordItem.setRecord_resolution(cursor.getInt(3));
        closeDatabase();
        cursor.close();
        return recordItem;
    }

    public boolean isRecordItemExist(String name) {
        Cursor cursor = openDatabase().query(RECORD_TABLE_NAME, RECORD_COL_PROJECTION,
                RECORD_COL_NAME + "=?", new String[]{name}, null, null,
                null, null);
        if (cursor.getCount() > 0) {
            closeDatabase();
            cursor.close();
            return true;
        } else {
            closeDatabase();
            cursor.close();
            return false;
        }
    }

    public int getLockStateByRecordItemName(String name) {
        Cursor cursor = openDatabase().query(RECORD_TABLE_NAME, RECORD_COL_PROJECTION,
                RECORD_COL_NAME + "=?", new String[]{name}, null, null,
                null, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int videoLock = cursor.getInt(cursor.getColumnIndex(RECORD_COL_LOCK));
            closeDatabase();
            cursor.close();
            return videoLock;
        } else {
            closeDatabase();
            cursor.close();
            return 0;
        }
    }

    public List<RecordItem> getAllRecordItem() {
        List<RecordItem> RecordItemList = new ArrayList<RecordItem>();
        String selectQuery = "SELECT * FROM " + RECORD_TABLE_NAME;
        Cursor cursor = openDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                RecordItem recordItem = new RecordItem();
                recordItem.setRecord_id(cursor.getInt(0));
                recordItem.setRecord_name(cursor.getString(1));
                recordItem.setRecord_lock(cursor.getInt(2));
                recordItem.setRecord_resolution(cursor.getInt(3));
                RecordItemList.add(recordItem);
            } while (cursor.moveToNext());
        }
        closeDatabase();
        cursor.close();
        return RecordItemList;
    }

    public Cursor getAllRecordItemCursor() {
        String selectQuery = "SELECT * FROM " + RECORD_TABLE_NAME;
        Cursor cursor = openDatabase().rawQuery(selectQuery, null);
        closeDatabase();
        return cursor;
    }

    public Cursor getLockRecordItemCursor() {
        String sqlLine = "SELECT * FROM " + RECORD_TABLE_NAME + " WHERE "
                + RECORD_COL_LOCK + "=?";
        String selection[] = new String[]{"1"};
        Cursor cursor = openDatabase().rawQuery(sqlLine, selection);
        closeDatabase();
        return cursor;
    }

    public int getOldestUnlockRecordItemId() {
        String sqlLine = "SELECT * FROM " + RECORD_TABLE_NAME + " WHERE "
                + RECORD_COL_LOCK + "=?";
        String selection[] = new String[]{"0"};
        Cursor cursor = openDatabase().rawQuery(sqlLine, selection);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int id = cursor.getInt(cursor.getColumnIndex(RECORD_COL_ID));
            closeDatabase();
            cursor.close();
            return id;
        } else {
            closeDatabase();
            cursor.close();
            return -1;
        }
    }

    public int getOldestRecordItemId() {
        String sqlLine = "SELECT * FROM " + RECORD_TABLE_NAME;
        Cursor cursor = openDatabase().rawQuery(sqlLine, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int id = cursor.getInt(cursor.getColumnIndex(RECORD_COL_ID));
            closeDatabase();
            cursor.close();
            return id;
        } else {
            closeDatabase();
            cursor.close();
            return -1;
        }
    }

    public String getRecordItemNameById(int id) {
        String sqlLine = "SELECT * FROM " + RECORD_TABLE_NAME + " WHERE "
                + RECORD_COL_ID + "=?";
        String selection[] = new String[]{String.valueOf(id)};
        Cursor cursor = openDatabase().rawQuery(sqlLine, selection);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String videoName = cursor.getString(cursor.getColumnIndex(RECORD_COL_NAME));
            closeDatabase();
            cursor.close();
            return videoName;
        } else {
            closeDatabase();
            cursor.close();
            return "";
        }
    }

    public void updateRecordItem(RecordItem recordItem) {
        ContentValues values = new ContentValues();
        values.put(RECORD_COL_NAME, recordItem.getRecord_name());
        values.put(RECORD_COL_LOCK, recordItem.getRecord_lock());
        values.put(RECORD_COL_RESOLUTION, recordItem.getRecord_resolution());
        openDatabase().update(RECORD_TABLE_NAME, values, RECORD_COL_ID + "=?",
                new String[]{String.valueOf(recordItem.getRecord_id())});
        closeDatabase();
    }

    public void deleteRecordItemById(int id) {
        openDatabase().delete(RECORD_TABLE_NAME, RECORD_COL_ID + "=?",
                new String[]{String.valueOf(id)});
        closeDatabase();
    }

    public void deleteRecordItemByName(String name) {
        openDatabase().delete(RECORD_TABLE_NAME, RECORD_COL_NAME + "=?",
                new String[]{name});
        closeDatabase();
    }

    public void deleteAllRecordItem() {
        openDatabase().delete(RECORD_TABLE_NAME, null, null);
        closeDatabase();
    }

}