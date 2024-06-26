package com.socksapp.missedconnection.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.ActivityMainBinding;
import com.socksapp.missedconnection.fragment.AboutUsFragment;
import com.socksapp.missedconnection.fragment.AccountSettingFragment;
import com.socksapp.missedconnection.fragment.AddPostFragment;
import com.socksapp.missedconnection.fragment.ChangePasswordFragment;
import com.socksapp.missedconnection.fragment.ChatFragment;
import com.socksapp.missedconnection.fragment.DeleteAccountFragment;
import com.socksapp.missedconnection.fragment.EditProfileFragment;
import com.socksapp.missedconnection.fragment.FindFragment;
import com.socksapp.missedconnection.fragment.MainFragment;
import com.socksapp.missedconnection.fragment.MessageFragment;
import com.socksapp.missedconnection.fragment.MyPostFragment;
import com.socksapp.missedconnection.fragment.PostsActivityFragment;
import com.socksapp.missedconnection.fragment.ProfileFragment;
import com.socksapp.missedconnection.fragment.SavedPostFragment;
import com.socksapp.missedconnection.fragment.SettingsFragment;
import com.socksapp.missedconnection.myclass.SharedPreferencesHelper;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private SharedPreferences nameShared,imageUrlShared,language,myLocationCity,myLocationDistrict;
    private SharedPreferences userDone;
    private String userMail;
    public FragmentContainerView fragmentContainerView;
    public BottomNavigationView bottomNavigationView;
    public DrawerLayout drawerLayout;
    public NavigationView navigationView;
    public View bottomViewLine;
    public ImageView buttonDrawerToggle;
    private ImageView headerImage;
    private TextView headerName;
    public View headerView,includedLayout;
    private SharedPreferencesHelper sharedPreferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = getSharedPreferences("Language",Context.MODE_PRIVATE);
        setLanguage();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().getExtras() != null) {
            String senderId = getIntent().getExtras().getString("senderId");
            if(senderId != null){
                if(senderId.isEmpty()){
                    PostsActivityFragment fragment = new PostsActivityFragment();
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }else {
                    Bundle args = new Bundle();
                    args.putString("anotherMail", senderId);
                    ChatFragment fragment = new ChatFragment();
                    fragment.setArguments(args);
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            }
        }

        sharedPreferencesHelper = new SharedPreferencesHelper(this);

        fragmentContainerView = findViewById(R.id.fragmentContainerView2);

        bottomNavigationView = binding.bottomNavView;

        bottomViewLine = binding.bottomViewLine;

        includedLayout = findViewById(R.id.content);

        bottomNavigationView.setItemIconTintList(null);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        drawerLayout = binding.drawerLayout;
        navigationView = binding.navView;
        buttonDrawerToggle = findViewById(R.id.buttonDrawerToggle);

        buttonDrawerToggle.setOnClickListener(v ->{
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView2);
            if (currentFragment instanceof SettingsFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);
                bottomNavigationView.setVisibility(View.GONE);
                getSupportFragmentManager().popBackStack();
            } else if (currentFragment instanceof SavedPostFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                bottomNavigationView.setVisibility(View.VISIBLE);
                getSupportFragmentManager().popBackStack();
            }else if (currentFragment instanceof MyPostFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                bottomNavigationView.setVisibility(View.VISIBLE);
                getSupportFragmentManager().popBackStack();
            }else if (currentFragment instanceof EditProfileFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                bottomNavigationView.setVisibility(View.VISIBLE);
                getSupportFragmentManager().popBackStack();
            }else if (currentFragment instanceof AccountSettingFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                bottomNavigationView.setVisibility(View.VISIBLE);
                getSupportFragmentManager().popBackStack();
            }else if (currentFragment instanceof AboutUsFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);
                bottomNavigationView.setVisibility(View.GONE);
                getSupportFragmentManager().popBackStack();
            }else if (currentFragment instanceof ChangePasswordFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);
                bottomNavigationView.setVisibility(View.GONE);
                getSupportFragmentManager().popBackStack();
            }else if (currentFragment instanceof DeleteAccountFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);
                bottomNavigationView.setVisibility(View.GONE);
                getSupportFragmentManager().popBackStack();
            }else if (currentFragment instanceof PostsActivityFragment) {
                buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);
                bottomNavigationView.setVisibility(View.GONE);
                getSupportFragmentManager().popBackStack();
            }else {
                drawerLayout.open();
            }
        });

        headerView = navigationView.getHeaderView(0);
        headerImage = headerView.findViewById(R.id.drawer_image);
        headerName = headerView.findViewById(R.id.drawer_user_name);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                if(item.getItemId() == R.id.nav_drawer_home){
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView2);
                    if (currentFragment instanceof MainFragment) {
                        drawerLayout.closeDrawers();
                    }else {
                        bottomNavigationView.setSelectedItemId(R.id.navHome);
                        loadFragment(MainFragment.class);
                    }
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
        myLocationCity = getSharedPreferences("MyLocationCity", Context.MODE_PRIVATE);
        myLocationDistrict = getSharedPreferences("MyLocationDistrict", Context.MODE_PRIVATE);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int itemId = item.getItemId();

                if(itemId == R.id.navHome){

                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView2);
                    if (currentFragment instanceof MainFragment) {

                    }else {
                        loadFragment(MainFragment.class);
                    }

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

        getFCMToken();

        getUpdateListener();

    }

    private void getFCMToken(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> firestore.collection("users").document(userMail).update("fcmToken",task.getResult()));
    }

    private void setLanguage() {
        String getLanguage = language.getString("language","");

        if(getLanguage.equals("english")){

            Locale locale;
            locale = new Locale("en");
            Locale.setDefault(locale);

            Configuration configuration = new Configuration();
            configuration.setLocale(locale);

            getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());

        }else if(getLanguage.equals("turkish")){
            Locale locale;
            locale = new Locale("tr");
            Locale.setDefault(locale);

            Configuration configuration = new Configuration();
            configuration.setLocale(locale);

            getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());
        }else {
            Locale locale;
            locale = new Locale("en");
            Locale.setDefault(locale);

            Configuration configuration = new Configuration();
            configuration.setLocale(locale);

            getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());
        }
    }

    private void signOut(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog_logout, null);
        builder.setView(dialogView);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button logoutButton = dialogView.findViewById(R.id.logoutButton);

        AlertDialog dlg = builder.create();

        cancelButton.setOnClickListener(v2 -> {
            dlg.dismiss();
        });

        logoutButton.setOnClickListener(v3 -> {
            SharedPreferences.Editor editor = nameShared.edit();
            clearShared(editor);
            SharedPreferences.Editor editor1 = imageUrlShared.edit();
            clearShared(editor1);
            SharedPreferences.Editor editor2 = userDone.edit();
            clearShared(editor2);
            SharedPreferences.Editor editor4 = myLocationCity.edit();
            clearShared(editor4);
            SharedPreferences.Editor editor5 = myLocationDistrict.edit();
            clearShared(editor5);
            sharedPreferencesHelper.clear();

            logout();
            drawerLayout.closeDrawers();
        });

        dlg.show();
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

                    sharedPreferencesHelper.saveString("myMail", userMail);

                    String name = documentSnapshot.getString("name");
                    String imageUrl = documentSnapshot.getString("imageUrl");

                    if(name != null && !name.isEmpty()){
                        headerName.setText(name);

                        SharedPreferences.Editor editor = nameShared.edit();
                        editor.putString("name",name);
                        editor.apply();
                    }
                    if(imageUrl != null && !imageUrl.isEmpty()){

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

                Glide.with(this)
                    .load(imageUrlShared.getString("imageUrl",""))
                    .apply(new RequestOptions()
                    .error(R.drawable.person_active_96)
                    .centerCrop())
                    .into(headerImage);
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void getUpdateListener(){
        SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long lastShownTime = sharedPreferences.getLong("last_update_shown_time", 0);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShownTime >= 24 * 60 * 60 * 1000) {
            PackageManager packageManager = getPackageManager();
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);

                firestore.collection("version")
                    .document("missedconnection")
                    .get().addOnSuccessListener(documentSnapshot -> {
                        if(documentSnapshot.exists()){
                            String versionName = documentSnapshot.getString("version");
                            String versionBase = packageInfo.versionName;

                            if(versionName != null){
                                versionName = versionName.replace(".","");
                                versionBase = versionBase.replace(".","");

                                int vN = Integer.parseInt(versionName);
                                int vB = Integer.parseInt(versionBase);

                                if(vN > vB){
                                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                                    View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_layout_play_store, null);
                                    bottomSheetDialog.setContentView(bottomSheetView);

                                    WebView webViewPlayStore = bottomSheetView.findViewById(R.id.webViewPlayStore);
                                    webViewPlayStore.getSettings().setJavaScriptEnabled(true);
                                    webViewPlayStore.loadUrl("https://play.google.com/store/apps/details?id=" + "com.cicekvolkan.missedconnections");

                                    bottomSheetDialog.show();

                                    sharedPreferences.edit().putLong("last_update_shown_time", currentTime).apply();
                                }

                            }

                        }
                    }).addOnFailureListener(e -> {

                    });

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}