package com.socksapp.missedconnection.myclass;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TimedDataManager {
    private static final String PREF_NAME = "timed_data_preferences";
    private static final String DATA_KEY = "data";
    private static final long EXPIRATION_TIME_MS = TimeUnit.MINUTES.toMillis(1440);

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public TimedDataManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        gson = new Gson();
    }

    private Map<String, TimedData> getStoredData() {
        String json = sharedPreferences.getString(DATA_KEY, "{}");
        Type type = new TypeToken<Map<String, TimedData>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void saveStoredData(Map<String, TimedData> data) {
        String json = gson.toJson(data);
        editor.putString(DATA_KEY, json);
        editor.apply();
    }

    public void saveData(String key, String value) {
        Map<String, TimedData> data = getStoredData();
        data.put(key, new TimedData(value, System.currentTimeMillis()));
        saveStoredData(data);
    }

    public String getData(String key, String defaultValue) {
        Map<String, TimedData> data = getStoredData();
        TimedData timedData = data.get(key);
        if (timedData == null || System.currentTimeMillis() - timedData.timestamp > EXPIRATION_TIME_MS) {
            removeData(key);
            return defaultValue;
        }
        return timedData.value;
    }

    public void removeData(String key) {
        Map<String, TimedData> data = getStoredData();
        data.remove(key);
        saveStoredData(data);
    }

    public void cleanUpExpiredData() {
        Map<String, TimedData> data = getStoredData();
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, TimedData>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TimedData> entry = iterator.next();
            if (currentTime - entry.getValue().timestamp > EXPIRATION_TIME_MS) {
                iterator.remove();
            }
        }
        saveStoredData(data);
    }

    private static class TimedData {
        String value;
        long timestamp;

        TimedData(String value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}

