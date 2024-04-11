package com.socksapp.missedconnection.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.ActivityMainBinding;
import com.socksapp.missedconnection.fragment.AddPostFragment;
import com.socksapp.missedconnection.fragment.FindFragment;
import com.socksapp.missedconnection.fragment.MainFragment;
import com.socksapp.missedconnection.fragment.MessageFragment;
import com.socksapp.missedconnection.fragment.ProfileFragment;
import com.socksapp.missedconnection.fragment.SettingsFragment;
import com.socksapp.missedconnection.myclass.RefDataAccess;
import com.squareup.picasso.Picasso;

import java.util.List;

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
    public DrawerLayout drawerLayout;
    public NavigationView navigationView;
    public ImageView buttonDrawerToggle;
    private ImageView headerImage;
    private TextView headerName;
    public View headerView,includedLayout;
    public RefDataAccess refDataAccess;
    public boolean active_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        active_map = true;

        bottomNavigationView = binding.bottomNavView;

        bottomNavigationView.setItemIconTintList(null);

        refDataAccess = new RefDataAccess(this);
        refDataAccess.open();

////        refDataAccess.insertRef("Volkan");
////        refDataAccess.insertRef("Okan");
////        refDataAccess.insertRef("Ahmet");
////        refDataAccess.insertRef("Mehmet");
//
//        refDataAccess.deleteRef("Okan");
////
//        List<String> namesList = refDataAccess.getAllRefs();
//        StringBuilder stringBuilder = new StringBuilder();
//        for (String name : namesList) {
//            stringBuilder.append(name).append("\n");
//        }
//
//        Toast.makeText(this,stringBuilder.toString(),Toast.LENGTH_LONG).show();

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();


        drawerLayout = binding.drawerLayout;
        navigationView = binding.navView;
        buttonDrawerToggle = findViewById(R.id.buttonDrawerToggle);

        buttonDrawerToggle.setOnClickListener(v ->{
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView2);
            if (currentFragment instanceof SettingsFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                bottomNavigationView.setVisibility(View.VISIBLE);
                getSupportFragmentManager().popBackStack();
            } else {
                drawerLayout.open();
            }
        });

        includedLayout = findViewById(R.id.content);

        headerView = navigationView.getHeaderView(0);
        headerImage = headerView.findViewById(R.id.drawer_image);
        headerName = headerView.findViewById(R.id.drawer_user_name);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                if(item.getItemId() == R.id.nav_drawer_home){
                    bottomNavigationView.setSelectedItemId(R.id.navHome);
                    loadFragment(MainFragment.class);
                } else if (item.getItemId() == R.id.nav_drawer_setting) {
                    loadFragment(SettingsFragment.class);
                } else if (item.getItemId() == R.id.nav_drawer_logout) {
                    signOut();
                }

                item.setChecked(true);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

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
                    loadFragment(MessageFragment.class);
                }

                return true;
            }
        });

        userMail = user.getEmail();

        getDataUser();
    }

    private void signOut(){
        new AlertDialog.Builder(MainActivity.this)
            .setMessage("Hesaptan çıkış yapılıyor..")
            .setPositiveButton("Çıkış yap", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    SharedPreferences.Editor editor = nameShared.edit();
                    clearShared(editor);
                    SharedPreferences.Editor editor1 = imageUrlShared.edit();
                    clearShared(editor1);
                    SharedPreferences.Editor editor2 = userDone.edit();
                    clearShared(editor2);

                    refDataAccess.deleteAllData();
                    logout();
                    drawerLayout.closeDrawers();
                }
            })
            .setNegativeButton("Hayır", null).show();
    }

    private void clearShared(SharedPreferences.Editor editor){
        editor.clear();
        editor.apply();
    }

    private void logout() {
        auth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void loadFragment(Class<? extends Fragment> fragmentClass) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainerView2, fragmentClass, null)
            .addToBackStack(null)
            .commit();
    }

    private void getDataUser(){
        if(!userDone.getString("done","").equals("done")){
            firestore.collection("users").document(userMail).get().addOnSuccessListener(documentSnapshot -> {
                if(documentSnapshot.exists()){
                    String name = documentSnapshot.getString("name");
                    String imageUrl = documentSnapshot.getString("imageUrl");

                    if(name != null && !name.isEmpty()){
                        headerName.setText(name);

                        SharedPreferences.Editor editor = nameShared.edit();
                        editor.putString("name",name);
                        editor.apply();
                    }
                    if(imageUrl != null && !imageUrl.isEmpty()){
//                        Picasso.get().load(imageUrl).into(headerImage);

                        Glide.with(this)
                            .load(imageUrl)
                            .apply(new RequestOptions()
                            .error(R.drawable.person_active_96)
                            .centerCrop())
                            .into(headerImage);

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
        }else {
            if(!nameShared.getString("name","").isEmpty()){
                headerName.setText(nameShared.getString("name",""));
            }

            if(!imageUrlShared.getString("imageUrl","").isEmpty()){
//                Picasso.get().load(imageUrlShared.getString("imageUrl","")).into(headerImage);

                Glide.with(this)
                    .load(imageUrlShared.getString("imageUrl",""))
                    .apply(new RequestOptions()
                    .error(R.drawable.person_active_96)
                    .centerCrop())
                    .into(headerImage);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refDataAccess.close();
    }
}