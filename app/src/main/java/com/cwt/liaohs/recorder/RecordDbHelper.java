package com.cwt.liaohs.recorder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RecordDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "record.db";

    private static final String RECORD_TABLE_NAME = "record";
    private static final String RECORD_COL_ID = "_id";
    private static final String RECORD_COL_NAME = "name";
    private static final String RECORD_COL_LOCK = "lock";
    private static final String RECORD_COL_RESOLUTION = "resolution";

    private static RecordDbHelper instance;

    public static RecordDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new RecordDbHelper(context);
        }
        return instance;
    }

    private RecordDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + RECORD_TABLE_NAME + " ("
                + RECORD_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + RECORD_COL_NAME + " TEXT," + RECORD_COL_LOCK + " INTEGER,"
                + RECORD_COL_RESOLUTION + " INTEGER" + ")";
        db.execSQL(createTableSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RECORD_TABLE_NAME);
        this.onCreate(db);
    }
}
