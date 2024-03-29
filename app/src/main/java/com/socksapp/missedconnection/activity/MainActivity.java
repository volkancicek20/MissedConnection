package com.socksapp.missedconnection.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.ActivityMainBinding;
import com.socksapp.missedconnection.fragment.AddPostFragment;
import com.socksapp.missedconnection.fragment.FindFragment;
import com.socksapp.missedconnection.fragment.MainFragment;
import com.socksapp.missedconnection.fragment.ProfileFragment;
import com.socksapp.missedconnection.fragment.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private SharedPreferences nameShared,imageUrlShared;
    private SharedPreferences userDone;
    private String userMail;
    public FragmentContainerView fragmentContainerView;
    public BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        bottomNavigationView = binding.bottomNavView;

        nameShared = getSharedPreferences("Name", Context.MODE_PRIVATE);
        imageUrlShared = getSharedPreferences("ImageUrl", Context.MODE_PRIVATE);

        userDone = getSharedPreferences("UserDone", Context.MODE_PRIVATE);

        fragmentContainerView = findViewById(R.id.fragmentContainerView2);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if(itemId == R.id.navHome){
                    loadFragment(MainFragment.class);
                } else if (itemId == R.id.navFind) {
                    loadFragment(FindFragment.class);
                } else if (itemId == R.id.navAdd) {
                    loadFragment(AddPostFragment.class);
                } else if (itemId == R.id.navProfile) {
                    loadFragment(ProfileFragment.class);
                } else {
                    loadFragment(SettingsFragment.class);
                }

                return true;
            }
        });

        userMail = user.getEmail();

        getDataUser();
    }

    private void loadFragment(Class<? extends Fragment> fragmentClass) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainerView2, fragmentClass, null)
            .commit();
    }

    private void getDataUser(){
        if(!userDone.getString("done","").equals("done")){
            firestore.collection("users").document(userMail).get().addOnSuccessListener(documentSnapshot -> {
                if(documentSnapshot.exists()){
                    String name = documentSnapshot.getString("name");
                    String imageUrl = documentSnapshot.getString("imageUrl");

                    if(name != null && !name.isEmpty()){
                        SharedPreferences.Editor editor = nameShared.edit();
                        editor.putString("name",name);
                        editor.apply();
                    }
                    if(imageUrl != null && !imageUrl.isEmpty()){
                        SharedPreferences.Editor editor = imageUrlShared.edit();
                        editor.putString("imageUrl",imageUrl);
                        editor.apply();
                    }
                    if(name != null && !name.isEmpty() && imageUrl != null && !imageUrl.isEmpty()){
                        SharedPreferences.Editor editor = userDone.edit();
                        editor.putString("done","done");
                        editor.apply();
                    }
                }
            });
        }
    }

}