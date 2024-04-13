package com.socksapp.missedconnection.myclass;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.socksapp.missedconnection.model.RefItem;

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

    public long insertRef(String ref, String mail) {
        ContentValues values = new ContentValues();
        values.put("ref", ref);
        values.put("mail", mail);
        return database.insert("refs", null, values);
    }

    public void deleteRef(String ref) {
        database.delete("refs", "ref=?", new String[]{ref});
    }

    public List<RefItem> getAllRefs() {
        List<RefItem> refList = new ArrayList<>();

        try (Cursor cursor = database.query("refs", new String[]{"ref", "mail"}, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String ref = cursor.getString(cursor.getColumnIndexOrThrow("ref"));
                    String mail = cursor.getString(cursor.getColumnIndexOrThrow("mail"));
                    RefItem item = new RefItem(ref, mail);
                    refList.add(item);
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

