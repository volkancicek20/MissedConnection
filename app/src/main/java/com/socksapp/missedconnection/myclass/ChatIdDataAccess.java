package com.socksapp.missedconnection.myclass;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.socksapp.missedconnection.model.ChatsId;
import com.socksapp.missedconnection.model.RefItem;

import java.util.ArrayList;
import java.util.List;

public class ChatIdDataAccess {
    private SQLiteDatabase database;
    private ChatIdDatabaseHelper dbHelper;

    public ChatIdDataAccess(Context context) {
        dbHelper = new ChatIdDatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void addChatsId(String mail,String id) {
        ContentValues values = new ContentValues();
        values.put("mail", mail);
        values.put("id", id);
        database.insert("chatsId", null, values);
    }

    public void deleteChatsId(String mail) {
        database.delete("chatsId", "mail = ?", new String[]{mail});
    }

    public List<ChatsId> getAllChatsId() {
        List<ChatsId> chatsIdList = new ArrayList<>();

        try (Cursor cursor = database.query("chatsId", new String[]{"mail", "id"}, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String mail = cursor.getString(cursor.getColumnIndexOrThrow("mail"));
                    String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                    ChatsId item = new ChatsId(mail, id);
                    chatsIdList.add(item);
                } while (cursor.moveToNext());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return chatsIdList;
    }

    public String getChatsIdByMail(String mail) {
        String id = null;

        String selection = "mail = ?";
        String[] selectionArgs = { mail };

        try (Cursor cursor = database.query("chatsId", new String[]{"id"}, selection, selectionArgs, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    public void deleteAllData() {
        database.delete("chatsId", null, null);
    }
}

