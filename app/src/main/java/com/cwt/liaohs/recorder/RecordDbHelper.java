package com.cwt.liaohs.recorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class RecordDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "record_db";

    private static final String RECORD_TABLE_NAME = "record";
    private static final String RECORD_COL_ID = "_id";
    private static final String RECORD_COL_NAME = "name";
    private static final String RECORD_COL_LOCK = "lock";
    private static final String RECORD_COL_RESOLUTION = "resolution";

    private static final String[] RECORD_COL_PROJECTION = new String[]{
            RECORD_COL_ID, RECORD_COL_NAME, RECORD_COL_LOCK,
            RECORD_COL_RESOLUTION,};

    private static final RecordItem RecordItem = null;

    public RecordDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableSql = "CREATE TABLE " + RECORD_TABLE_NAME + " ("
                + RECORD_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + RECORD_COL_NAME + " TEXT," + RECORD_COL_LOCK + " INTEGER,"
                + RECORD_COL_RESOLUTION + " INTEGER" + ");";
        db.execSQL(createTableSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + RECORD_TABLE_NAME);

        // Create tables again
        onCreate(db);
    }

    // Add new RecordItem
    public int addRecordItem(RecordItem recordItem) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RECORD_COL_NAME, recordItem.getRecord_name());
        values.put(RECORD_COL_LOCK, recordItem.getRecord_lock());
        values.put(RECORD_COL_RESOLUTION, recordItem.getRecord_resolution());

        // Insert to database
        long rowId = db.insert(RECORD_TABLE_NAME, null, values);

        // Close the database
        db.close();

        return (int) rowId;
    }

    // Get RecordItem By ID
    public RecordItem getRecordItemById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(RECORD_TABLE_NAME, RECORD_COL_PROJECTION,
                RECORD_COL_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();
        RecordItem recordItem = new RecordItem();
        recordItem.setRecord_id(cursor.getInt(0));
        recordItem.setRecord_name(cursor.getString(1));
        recordItem.setRecord_lock(cursor.getInt(2));
        recordItem.setRecord_resolution(cursor.getInt(3));

        db.close();
        cursor.close();

        return RecordItem;
    }

    // Get RecordItem By Name
    public RecordItem getRecordItemByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(RECORD_TABLE_NAME, RECORD_COL_PROJECTION,
                RECORD_COL_NAME + "=?", new String[]{name}, null, null,
                null, null);

        if (cursor != null)
            cursor.moveToFirst();

        RecordItem recordItem = new RecordItem();
        recordItem.setRecord_id(cursor.getInt(0));
        recordItem.setRecord_name(cursor.getString(1));
        recordItem.setRecord_lock(cursor.getInt(2));
        recordItem.setRecord_resolution(cursor.getInt(3));

        db.close();
        cursor.close();

        return recordItem;
    }

    /**
     * @param name
     * @return
     */
    public boolean isRecordItemExist(String name) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(RECORD_TABLE_NAME, RECORD_COL_PROJECTION,
                RECORD_COL_NAME + "=?", new String[]{name}, null, null,
                null, null);

        if (cursor.getCount() > 0) {
            db.close();
            cursor.close();
            return true;
        } else {
            db.close();
            cursor.close();
            return false;
        }
    }

    /**
     * @param name
     * @return
     */
    public int getLockStateByRecordItemName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(RECORD_TABLE_NAME, RECORD_COL_PROJECTION,
                RECORD_COL_NAME + "=?", new String[]{name}, null, null,
                null, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int videoLock = cursor.getInt(cursor
                    .getColumnIndex(RECORD_COL_LOCK));

            db.close();
            cursor.close();

            return videoLock;
        } else {
            db.close();
            cursor.close();

            return 0;
        }
    }

    /**
     * @return
     */
    public List<RecordItem> getAllRecordItem() {
        List<RecordItem> RecordItemList = new ArrayList<RecordItem>();
        String selectQuery = "SELECT * FROM " + RECORD_TABLE_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
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

        db.close();
        cursor.close();

        // return list
        return RecordItemList;
    }

    public Cursor getAllRecordItemCursor() {
        String selectQuery = "SELECT * FROM " + RECORD_TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        return cursor;
    }

    /**
     * @return
     */
    public Cursor getLockRecordItemCursor() {
        String sqlLine = "SELECT * FROM " + RECORD_TABLE_NAME + " WHERE "
                + RECORD_COL_LOCK + "=?";
        String selection[] = new String[]{"1"};
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(sqlLine, selection);
        return cursor;
    }

    /**
     * @return
     */
    public int getOldestUnlockRecordItemId() {
        String sqlLine = "SELECT * FROM " + RECORD_TABLE_NAME + " WHERE "
                + RECORD_COL_LOCK + "=?";
        String selection[] = new String[]{"0"};
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(sqlLine, selection);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int id = cursor.getInt(cursor.getColumnIndex(RECORD_COL_ID));

            db.close();
            cursor.close();

            return id;
        } else {
            db.close();
            cursor.close();

            return -1;
        }
    }

    /**
     * @return
     */
    public int getOldestRecordItemId() {
        String sqlLine = "SELECT * FROM " + RECORD_TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(sqlLine, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int id = cursor.getInt(cursor.getColumnIndex(RECORD_COL_ID));

            db.close();
            cursor.close();

            return id;
        } else {
            db.close();
            cursor.close();

            return -1;
        }
    }

    public String getRecordItemNameById(int id) {
        String sqlLine = "SELECT * FROM " + RECORD_TABLE_NAME + " WHERE "
                + RECORD_COL_ID + "=?";
        String selection[] = new String[]{String.valueOf(id)};
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(sqlLine, selection);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String videoName = cursor.getString(cursor
                    .getColumnIndex(RECORD_COL_NAME));

            db.close();
            cursor.close();

            return videoName;
        } else {
            db.close();
            cursor.close();

            return "";
        }
    }

    public int updateRecordItem(RecordItem recordItem) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(RECORD_COL_NAME, recordItem.getRecord_name());
        values.put(RECORD_COL_LOCK, recordItem.getRecord_lock());
        values.put(RECORD_COL_RESOLUTION, recordItem.getRecord_resolution());

        return db.update(RECORD_TABLE_NAME, values, RECORD_COL_ID + "=?",
                new String[]{String.valueOf(recordItem.getRecord_id())});
    }

    public void deleteRecordItemById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(RECORD_TABLE_NAME, RECORD_COL_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteRecordItemByName(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(RECORD_TABLE_NAME, RECORD_COL_NAME + "=?",
                new String[]{name});
        db.close();
    }

    public void deleteAllRecordItem() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(RECORD_TABLE_NAME, null, null);
        db.close();
    }

}
