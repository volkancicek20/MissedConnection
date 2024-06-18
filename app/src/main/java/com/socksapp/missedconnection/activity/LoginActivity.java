package com.socksapp.missedconnection.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;
import com.socksapp.missedconnection.R;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private SharedPreferences language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        language = getSharedPreferences("Language", Context.MODE_PRIVATE);
        setLanguage();

        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        if(auth.getCurrentUser() != null && auth.getCurrentUser().isEmailVerified()){
            Intent intent = new Intent(LoginActivity.this,MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setLanguage() {
        String getLanguage = language.getString("language","");
        SharedPreferences.Editor editor = language.edit();
        if(getLanguage.equals("english")){

            editor.putString("language","english");
            editor.apply();

            Locale locale = new Locale("en");
            Locale.setDefault(locale);

            Configuration configuration = new Configuration();
            configuration.setLocale(locale);

            getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());

        }else if(getLanguage.equals("turkish")){
            editor.putString("language","turkish");
            editor.apply();

            Locale locale = new Locale("tr");
            Locale.setDefault(locale);

            Configuration configuration = new Configuration();
            configuration.setLocale(locale);

            getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());
        }else {
            String language = getCurrentLanguageName();
            System.out.println("language: "+language);
            if(language.equals("English")){
                editor.putString("language","english");
                editor.apply();

                Locale locale = new Locale("en");
                Locale.setDefault(locale);

                Configuration configuration = new Configuration();
                configuration.setLocale(locale);

                getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());

            }else if(language.equals("Türkçe")){
                editor.putString("language","turkish");
                editor.apply();

                Locale locale = new Locale("tr");
                Locale.setDefault(locale);

                Configuration configuration = new Configuration();
                configuration.setLocale(locale);

                getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());
            }else {
                editor.putString("language","english");
                editor.apply();

                Locale locale = new Locale("en");
                Locale.setDefault(locale);

                Configuration configuration = new Configuration();
                configuration.setLocale(locale);

                getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());
            }
        }
    }

    public String getCurrentLanguageName() {
        Locale locale = Locale.getDefault();
        return locale.getDisplayLanguage(locale);
    }
}