package com.socksapp.missedconnection.myclass;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RefDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "saves.db";
    private static final int DATABASE_VERSION = 1;

    public RefDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE refs (ref TEXT PRIMARY KEY, mail TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS refs");
        onCreate(db);
    }

}
