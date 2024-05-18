package com.socksapp.missedconnection.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.PostAdapter;
import com.socksapp.missedconnection.databinding.FragmentMainBinding;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.myclass.TimedDataManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private SharedPreferences nameShared,imageUrlShared;
    private String userMail,myUserName,myImageUrl;
    private MainActivity mainActivity;
    public PostAdapter postAdapter;
    public ArrayList<FindPost> postArrayList;
    private Handler handler;
    private TimedDataManager timedDataManager;
    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        postArrayList = new ArrayList<>();

        nameShared = requireActivity().getSharedPreferences("Name", Context.MODE_PRIVATE);
        imageUrlShared = requireActivity().getSharedPreferences("ImageUrl", Context.MODE_PRIVATE);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
        mainActivity.includedLayout.setVisibility(View.VISIBLE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mainActivity.fragmentContainerView.getLayoutParams();
        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        mainActivity.fragmentContainerView.setLayoutParams(layoutParams);

        binding.shimmerLayout.startShimmer();

        userMail = user.getEmail();

        timedDataManager = new TimedDataManager(view.getContext());

        myUserName = nameShared.getString("name","");
        myImageUrl = nameShared.getString("imageUrl","");

        binding.recyclerViewMain.setLayoutManager(new LinearLayoutManager(view.getContext()));
        postAdapter = new PostAdapter(postArrayList,view.getContext(),MainFragment.this);
        binding.recyclerViewMain.setAdapter(postAdapter);
        postArrayList.clear();

        handler = new Handler();

        Bundle args = getArguments();
        if (args != null) {
            String city = args.getString("city","");
            String district = args.getString("district","");
            String place = args.getString("place","");
            double radius = args.getDouble("radius",0);
            double latitude = args.getDouble("latitude",0);
            double longitude = args.getDouble("longitude",0);
            String date1 = args.getString("date1","");
            String date2 = args.getString("date2","");
            String time1 = args.getString("time1","");
            String time2 = args.getString("time2","");

            getData(city,district,place,date1,date2,time1,time2,radius,latitude,longitude);
        }else {
            getData();
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                System.exit(0);
            }
        });

    }

    public void dialogShow(View view, String mail, String name, Double lat, Double lng, double radius, DocumentReference documentReference){
        final Dialog dialog = new Dialog(view.getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_layout);

        LinearLayout map = dialog.findViewById(R.id.layoutMap);


        if(lat == 0.0 && lng == 0.0){
            map.setVisibility(View.GONE);
        }

        map.setOnClickListener(v -> {
            dialog.dismiss();
            Bundle args = new Bundle();
            args.putString("fragment_type", "main");
            args.putDouble("fragment_lat", lat);
            args.putDouble("fragment_lng", lng);
            args.putDouble("fragment_radius", radius);
            GoogleMapsFragment myFragment = new GoogleMapsFragment();
            myFragment.setArguments(args);
            FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragmentContainerView2,myFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        LinearLayout save = dialog.findViewById(R.id.layoutSave);
        LinearLayout message = dialog.findViewById(R.id.layoutMessage);
        LinearLayout report = dialog.findViewById(R.id.layoutReport);

        save.setOnClickListener(v ->{
            HashMap<String,Object> data = new HashMap<>();
            data.put(documentReference.getId(),mail);
            firestore.collection("saves").document(userMail).set(data,SetOptions.merge()).addOnSuccessListener(unused -> {
//                mainActivity.refDataAccess.insertRef(documentReference.getId(),mail);
                Toast.makeText(view.getContext(), "Kaydedildi", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }).addOnFailureListener(e -> {

            });
        });

        message.setOnClickListener(v ->{
            if(!myUserName.isEmpty()){
                Bundle args = new Bundle();
                args.putString("anotherMail", mail);
                ChatFragment fragment = new ChatFragment();
                fragment.setArguments(args);
                FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                dialog.dismiss();
            }else {
                Toast.makeText(v.getContext(),"Mesaj göndermek için profilinizi tamamlamalısınız.",Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }

        });

        report.setOnClickListener(v ->{

        });

        if(dialog.getWindow() != null){
            dialog.show();
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }

    private void getData(String cityFind,String districtFind,String placeFind,String date1Find,String date2Find,String time1Find,String time2Find,double radiusFind,double latFind,double lngFind){

        boolean checkDistrict,checkPlace,checkDate,checkTime,checkField;
        checkDistrict = !districtFind.isEmpty();
        checkPlace = !placeFind.isEmpty();
        checkDate = !date1Find.isEmpty() && !date2Find.isEmpty();
        checkTime = !time1Find.isEmpty() && !time2Find.isEmpty();
        checkField = radiusFind != 0 && latFind != 0 && lngFind != 0;

        Query query = null;

        if(checkDistrict && checkPlace && checkDate && checkTime && checkField){
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(date_1 != null && date_2 != null && time_1 != null && time_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();
                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereEqualTo("place",placeFind)
                        .whereLessThanOrEqualTo("date1",date2_long)
                        .whereGreaterThanOrEqualTo("date2",date1_long)
                        .whereLessThanOrEqualTo("time1",time2_long)
                        .whereGreaterThanOrEqualTo("time2",time1_long);
                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkPlace && checkDate && checkTime) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(date_1 != null && date_2 != null && time_1 != null && time_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();
                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereEqualTo("place",placeFind)
                        .whereLessThanOrEqualTo("date1",date2_long)
                        .whereGreaterThanOrEqualTo("date2",date1_long)
                        .whereLessThanOrEqualTo("time1",time2_long)
                        .whereGreaterThanOrEqualTo("time2",time1_long);
                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkPlace && checkDate && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                if(date_1 != null && date_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereEqualTo("place",placeFind)
                        .whereLessThanOrEqualTo("date1",date2_long)
                        .whereGreaterThanOrEqualTo("date2",date1_long);
                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkPlace && checkTime && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(time_1 != null && time_2 != null){

                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereEqualTo("place",placeFind)
                        .whereLessThanOrEqualTo("time1",time2_long)
                        .whereGreaterThanOrEqualTo("time2",time1_long);

                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkDate && checkTime && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(date_1 != null && date_2 != null && time_1 != null && time_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();
                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereLessThanOrEqualTo("date1",date2_long)
                        .whereGreaterThanOrEqualTo("date2",date1_long)
                        .whereLessThanOrEqualTo("time1",time2_long)
                        .whereGreaterThanOrEqualTo("time2",time1_long);
                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkPlace && checkDate) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);


                if(date_1 != null && date_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereEqualTo("place",placeFind)
                        .whereLessThanOrEqualTo("date1",date2_long)
                        .whereGreaterThanOrEqualTo("date2",date1_long);

                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkPlace && checkTime) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(time_1 != null && time_2 != null){
                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereEqualTo("place",placeFind)
                        .whereLessThanOrEqualTo("time1",time2_long)
                        .whereGreaterThanOrEqualTo("time2",time1_long);

                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkDate && checkTime) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(date_1 != null && date_2 != null && time_1 != null && time_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();
                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereLessThanOrEqualTo("date1",date2_long)
                        .whereGreaterThanOrEqualTo("date2",date1_long)
                        .whereLessThanOrEqualTo("time1",time2_long)
                        .whereGreaterThanOrEqualTo("time2",time1_long);

                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkPlace && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            query = firestore.collection(collection)
                .whereEqualTo("district", districtFind)
                .whereEqualTo("place",placeFind);

        }
        else if (checkDistrict && checkDate && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                if(date_1 != null && date_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereLessThanOrEqualTo("date1",date2_long)
                        .whereGreaterThanOrEqualTo("date2",date1_long);

                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkTime && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(time_1 != null && time_2 != null){

                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereLessThanOrEqualTo("time1",time2_long)
                        .whereGreaterThanOrEqualTo("time2",time1_long);

                }else {
                }
            }catch (Exception e){
                System.out.println("exception : "+e.getLocalizedMessage());
                e.printStackTrace();
            }

        }
        else if (checkDistrict && checkPlace) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            query = firestore.collection(collection)
                .whereEqualTo("district", districtFind)
                .whereEqualTo("place", placeFind);

        }
        else if (checkDistrict && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            query = firestore.collection(collection)
                .whereEqualTo("district", districtFind);

        }
        else if (checkDistrict && checkDate) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                if(date_1 != null && date_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereLessThanOrEqualTo("date1",date2_long)
                        .whereGreaterThanOrEqualTo("date2",date1_long);

                }else {
                }
            }catch (Exception e){
                System.out.println("error: "+e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkTime) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(time_1 != null && time_2 != null){
                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection(collection)
                        .whereEqualTo("district", districtFind)
                        .whereLessThanOrEqualTo("time1",time2_long)
                        .whereGreaterThanOrEqualTo("time2",time1_long);

                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            query = firestore.collection(collection)
                .whereEqualTo("district", districtFind);

        }
        else {
            postArrayList.clear();
            String collection = "post" + cityFind;
            query = firestore.collection(collection);

        }

        if(query != null){
            if(checkField){
                query.get().addOnSuccessListener(queryDocumentSnapshots -> {
                    if(queryDocumentSnapshots.isEmpty()){
                        FindPost post = new FindPost();
                        post.viewType = 2;

                        postArrayList.add(post);
                        binding.shimmerLayout.stopShimmer();
                        binding.shimmerLayout.setVisibility(View.GONE);
                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
                        postAdapter.notifyDataSetChanged();
                        mainActivity.bottomNavigationView.setSelectedItemId(R.id.navHome);
                        return;
                    }
                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){

                        double lat = 0,lng = 0;
                        double radius = 0;
                        Double lat_x = querySnapshot.getDouble("lat");
                        Double lng_y = querySnapshot.getDouble("lng");
                        Long x = querySnapshot.getLong("radius");
                        if(lat_x != null && lng_y != null && x != null){
                            lat = lat_x;
                            lng = lng_y;
                            radius = x;
                        }

                        boolean check = lat != 0 && lng != 0 && radius != 0;

                        if(check){

                            double distance = CalculationByDistance(latFind,lngFind,lat,lng);
                            double radiusSum = radiusFind/1000 + radius/1000;
                            boolean isIntersecting = distance <= radiusSum;
                            if (isIntersecting) {
                                String imageUrl = querySnapshot.getString("imageUrl");
                                String name = querySnapshot.getString("name");
                                String mail = querySnapshot.getString("mail");
                                String city = querySnapshot.getString("city");
                                String district = querySnapshot.getString("district");
                                Long time1 = querySnapshot.getLong("time1");
                                Long time2 = querySnapshot.getLong("time2");
                                Long date1 = querySnapshot.getLong("date1");
                                Long date2 = querySnapshot.getLong("date2");
                                String place = querySnapshot.getString("place");
                                String explain = querySnapshot.getString("explain");
                                Timestamp timestamp = querySnapshot.getTimestamp("timestamp");
                                DocumentReference documentReference = querySnapshot.getReference();

                                FindPost post = new FindPost();
                                post.viewType = 1;
                                post.imageUrl = imageUrl;
                                post.name = name;
                                post.mail = mail;
                                post.city = city;
                                post.district = district;
                                if (time1 == null) {
                                    post.time1 = 0;
                                } else {
                                    post.time1 = time1;
                                }
                                if (time2 == null) {
                                    post.time2 = 0;
                                } else {
                                    post.time2 = time2;
                                }
                                if (date1 == null) {
                                    post.date1 = 0;
                                } else {
                                    post.date1 = date1;
                                }
                                if (date2 == null) {
                                    post.date2 = 0;
                                } else {
                                    post.date2 = date2;
                                }
                                post.place = place;
                                post.explain = explain;
                                post.timestamp = timestamp;
                                post.lat = lat;
                                post.lng = lng;
                                post.radius = radius;
                                post.documentReference = documentReference;

                                postArrayList.add(post);
                                binding.shimmerLayout.stopShimmer();
                                binding.shimmerLayout.setVisibility(View.GONE);
                                binding.recyclerViewMain.setVisibility(View.VISIBLE);
                                postAdapter.notifyDataSetChanged();
                                mainActivity.bottomNavigationView.setSelectedItemId(R.id.navHome);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("error : "+e.getLocalizedMessage());
                    e.printStackTrace();
                });
            }
            else {
                query.get().addOnSuccessListener(queryDocumentSnapshots -> {
                    if(queryDocumentSnapshots.isEmpty()){
                        FindPost post = new FindPost();
                        post.viewType = 2;

                        postArrayList.add(post);
                        binding.shimmerLayout.stopShimmer();
                        binding.shimmerLayout.setVisibility(View.GONE);
                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
                        postAdapter.notifyDataSetChanged();
                        mainActivity.bottomNavigationView.setSelectedItemId(R.id.navHome);
                        return;
                    }
                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
                        String imageUrl = querySnapshot.getString("imageUrl");
                        String name = querySnapshot.getString("name");
                        String mail = querySnapshot.getString("mail");
                        String city = querySnapshot.getString("city");
                        String district = querySnapshot.getString("district");
                        Long time1 = querySnapshot.getLong("time1");
                        Long time2 = querySnapshot.getLong("time2");
                        Long date1 = querySnapshot.getLong("date1");
                        Long date2 = querySnapshot.getLong("date2");
                        String place = querySnapshot.getString("place");
                        String explain = querySnapshot.getString("explain");
                        Double lat = querySnapshot.getDouble("lat");
                        Double lng = querySnapshot.getDouble("lng");
                        Long x = querySnapshot.getLong("radius");
                        double radius = 0;
                        if(x != null){
                            radius = x;
                        }
                        Timestamp timestamp = querySnapshot.getTimestamp("timestamp");
                        DocumentReference documentReference = querySnapshot.getReference();

                        FindPost post = new FindPost();
                        post.viewType = 1;
                        post.imageUrl = imageUrl;
                        post.name = name;
                        post.mail = mail;
                        post.city = city;
                        post.district = district;
                        if (time1 == null) {
                            post.time1 = 0;
                        } else {
                            post.time1 = time1;
                        }
                        if (time2 == null) {
                            post.time2 = 0;
                        } else {
                            post.time2 = time2;
                        }
                        if (date1 == null) {
                            post.date1 = 0;
                        } else {
                            post.date1 = date1;
                        }
                        if (date2 == null) {
                            post.date2 = 0;
                        } else {
                            post.date2 = date2;
                        }
                        post.place = place;
                        post.explain = explain;
                        post.timestamp = timestamp;
                        post.lat = lat;
                        post.lng = lng;
                        post.radius = radius;
                        post.documentReference = documentReference;

                        postArrayList.add(post);
                        binding.shimmerLayout.stopShimmer();
                        binding.shimmerLayout.setVisibility(View.GONE);
                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
                        postAdapter.notifyDataSetChanged();
                        mainActivity.bottomNavigationView.setSelectedItemId(R.id.navHome);
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("error : "+e.getLocalizedMessage());
                    e.printStackTrace();
                });
            }
        }else {
            System.out.println("null");
        }

    }

    private void getData(){
        firestore.collection("post"+"İstanbul").get().addOnSuccessListener(queryDocumentSnapshots -> {
            boolean found = false;
            for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
                found = true;
                String imageUrl = querySnapshot.getString("imageUrl");
                String name = querySnapshot.getString("name");
                String mail = querySnapshot.getString("mail");
                String city = querySnapshot.getString("city");
                String district = querySnapshot.getString("district");
                Long time1 = querySnapshot.getLong("time1");
                Long time2 = querySnapshot.getLong("time2");
                Long date1 = querySnapshot.getLong("date1");
                Long date2 = querySnapshot.getLong("date2");
                String place = querySnapshot.getString("place");
                String explain = querySnapshot.getString("explain");
                Double lat = querySnapshot.getDouble("lat");
                Double lng = querySnapshot.getDouble("lng");
                Long x = querySnapshot.getLong("radius");
                double radius = 0;
                if(x != null){
                    radius = x;
                }
                Timestamp timestamp = querySnapshot.getTimestamp("timestamp");
                DocumentReference documentReference = querySnapshot.getReference();

                FindPost post = new FindPost();
                post.viewType = 1;
                post.imageUrl = imageUrl;
                post.name = name;
                post.mail = mail;
                post.city = city;
                post.district = district;
                if (time1 == null) {
                    post.time1 = 0;
                } else {
                    post.time1 = time1;
                }
                if (time2 == null) {
                    post.time2 = 0;
                } else {
                    post.time2 = time2;
                }
                if (date1 == null) {
                    post.date1 = 0;
                } else {
                    post.date1 = date1;
                }
                if (date2 == null) {
                    post.date2 = 0;
                } else {
                    post.date2 = date2;
                }
                post.place = place;
                post.explain = explain;
                post.timestamp = timestamp;
                post.lat = lat;
                post.lng = lng;
                post.radius = radius;
                post.documentReference = documentReference;

                postArrayList.add(post);
                binding.shimmerLayout.stopShimmer();
                binding.shimmerLayout.setVisibility(View.GONE);
                binding.recyclerViewMain.setVisibility(View.VISIBLE);
                postAdapter.notifyDataSetChanged();
            }
            if(!found){
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        FindPost post = new FindPost();
                        post.viewType = 2;

                        postArrayList.add(post);
                        postAdapter.notifyDataSetChanged();

                        binding.shimmerLayout.stopShimmer();
                        binding.shimmerLayout.setVisibility(View.GONE);
                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
                    }
                }, 1000);
            }
        });
    }

    public void setActivityNotification(String mail, DocumentReference ref){

        String refId = ref.getId();
        String value = timedDataManager.getData(refId,"");

        if(value.isEmpty()){
            timedDataManager.saveData(refId, "view");

            Map<String, Object> viewData = new HashMap<>();
            viewData.put("name", myUserName);
            viewData.put("refId", refId);
            viewData.put("type", "view");
            viewData.put("timestamp", FieldValue.serverTimestamp());

            DocumentReference documentReference = firestore.collection("views").document(mail).collection(mail).document();

            documentReference.set(viewData,SetOptions.merge()).addOnSuccessListener(unused -> {
                Toast.makeText(requireContext(),"Bildirildi",Toast.LENGTH_SHORT).show();
            });
        }else {
            // Veri bulundu ve süresi dolmamış
        }
    }

    public void goFind(){
        FindFragment fragment = new FindFragment();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mainActivity.bottomNavigationView.setSelectedItemId(R.id.navFind);
    }

    public double CalculationByDistance(double initialLat, double initialLong,
                                        double finalLat, double finalLong){
        int R = 6371;
        double dLat = toRadians(finalLat-initialLat);
        double dLon = toRadians(finalLong-initialLong);
        initialLat = toRadians(initialLat);
        finalLat = toRadians(finalLat);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(initialLat) * Math.cos(finalLat);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    public double toRadians(double deg) {
        return deg * (Math.PI/180);
    }


    @Override
    public void onResume() {
        super.onResume();
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
        mainActivity.includedLayout.setVisibility(View.VISIBLE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
}