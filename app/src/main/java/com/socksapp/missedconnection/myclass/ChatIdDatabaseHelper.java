package com.socksapp.missedconnection.myclass;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ChatIdDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "usersid.db";
    private static final int DATABASE_VERSION = 1;

    public ChatIdDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chatsId ( mail TEXT PRIMARY KEY , id TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS chatsId");
        onCreate(db);
    }
}

