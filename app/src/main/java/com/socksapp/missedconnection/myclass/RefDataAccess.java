package com.socksapp.missedconnection.myclass;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class RefDataAccess {
    private SQLiteDatabase database;
    private RefDatabaseHelper dbHelper;

    public RefDataAccess(Context context) {
        dbHelper = new RefDatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertRef(String ref) {
        ContentValues values = new ContentValues();
        values.put("ref", ref);
        return database.insert("refs", null, values);
    }

    public void deleteRef(String ref) {
        database.delete("refs", "ref=?", new String[]{ref});
    }

    public List<String> getAllRefs() {
        List<String> refList = new ArrayList<>();

        try (Cursor cursor = database.query("refs", new String[]{"ref"}, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String ref = cursor.getString(cursor.getColumnIndexOrThrow("ref"));
                    refList.add(ref);
                } while (cursor.moveToNext());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return refList;
    }

    public void deleteAllData() {
        database.delete("refs", null, null);
    }

}

