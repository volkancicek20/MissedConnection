package com.socksapp.missedconnection.myclass;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Utils {
    public static String loadJSONFromAsset(Context context, String fileName) {
        String json = null;
        try (InputStream is = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            json = builder.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return json;
    }

    public class CityData {
        private List<City> data;

        public List<City> getData() {
            return data;
        }

        public void setData(List<City> data) {
            this.data = data;
        }
    }

    public class City {
        private String il_adi;
        private List<District> ilceler;

        public String getIl_adi() {
            return il_adi;
        }

        public void setIl_adi(String il_adi) {
            this.il_adi = il_adi;
        }

        public List<District> getIlceler() {
            return ilceler;
        }

        public void setIlceler(List<District> ilceler) {
            this.ilceler = ilceler;
        }
    }

    public class District {
        private String ilce_adi;

        public String getIlce_adi() {
            return ilce_adi;
        }

        public void setIlce_adi(String ilce_adi) {
            this.ilce_adi = ilce_adi;
        }
    }
}

