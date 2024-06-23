package com.socksapp.missedconnection.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.fragment.ChatFragment;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();

        if(getIntent().getExtras()!=null){
            if(auth.getCurrentUser() != null && auth.getCurrentUser().isEmailVerified()){
                String senderId = getIntent().getExtras().getString("senderId");

                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("senderId", senderId);
                startActivity(intent);

            }else {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(SplashActivity.this,LoginActivity.class));
                    }
                },1000);
            }
        }else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(SplashActivity.this,LoginActivity.class));
                }
            },1000);
        }
    }
}