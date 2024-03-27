package com.socksapp.missedconnection.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.ActivityMainBinding;
import com.socksapp.missedconnection.fragment.AddPostFragment;
import com.socksapp.missedconnection.fragment.FindFragment;
import com.socksapp.missedconnection.fragment.MainFragment;
import com.socksapp.missedconnection.fragment.ProfileFragment;
import com.socksapp.missedconnection.fragment.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
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
    }

    private void loadFragment(Class<? extends Fragment> fragmentClass) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView2, fragmentClass, null)
                .commit();
    }

}