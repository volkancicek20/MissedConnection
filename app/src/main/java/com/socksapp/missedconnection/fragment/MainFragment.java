package com.socksapp.missedconnection.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.PostAdapter;
import com.socksapp.missedconnection.databinding.FragmentFindBinding;
import com.socksapp.missedconnection.databinding.FragmentMainBinding;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.myclass.RefDataAccess;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private SharedPreferences nameShared,imageUrlShared;
    private String userMail;
    private MainActivity mainActivity;
    public PostAdapter postAdapter;
    public ArrayList<FindPost> postArrayList;
    private static final int SCROLL_THRESHOLD = 200;
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

        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
        mainActivity.includedLayout.setVisibility(View.VISIBLE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mainActivity.fragmentContainerView.getLayoutParams();
        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        mainActivity.fragmentContainerView.setLayoutParams(layoutParams);

        binding.shimmerLayout.startShimmer();

        userMail = user.getEmail();

        binding.recyclerViewMain.setLayoutManager(new LinearLayoutManager(view.getContext()));
        postAdapter = new PostAdapter(postArrayList,view.getContext(),MainFragment.this);
        binding.recyclerViewMain.setAdapter(postAdapter);
        postArrayList.clear();

        Bundle args = getArguments();
        if (args != null) {
            String city = args.getString("city");
            String district = args.getString("district");
            String place = args.getString("place");
            int radius = args.getInt("radius");
            double latitude = args.getDouble("latitude");
            double longitude = args.getDouble("longitude");
            String date1 = args.getString("date1");
            String date2 = args.getString("date2");
            String time1 = args.getString("time1");
            String time2 = args.getString("time2");

            getData(city,district,place,date1,date2,time1,time2,radius,latitude,longitude);
        }else {
            getData();
        }

        final int[] totalScrolledDistance = {0};

        binding.recyclerViewMain.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                totalScrolledDistance[0] += dy;

                if (dy > 0 && totalScrolledDistance[0] >= SCROLL_THRESHOLD) {
                    animateBottomNavigationView(false);

                    totalScrolledDistance[0] = 0;
                }

                if (dy < 0 && totalScrolledDistance[0] <= -SCROLL_THRESHOLD) {
                    animateBottomNavigationView(true);

                    totalScrolledDistance[0] = 0;
                }
            }
        });

    }

    private void animateBottomNavigationView(boolean isVisible) {
        if (isVisible) {
            mainActivity.bottomNavigationView.animate()
                .translationY(0)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                    .withStartAction(() -> {
                        mainActivity.bottomViewLine.setVisibility(View.VISIBLE);
                        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
                    });
        } else {
            mainActivity.bottomNavigationView.animate()
                .translationY(mainActivity.bottomNavigationView.getHeight())
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    mainActivity.bottomViewLine.setVisibility(View.GONE);
                    mainActivity.bottomNavigationView.setVisibility(View.GONE);
                });
        }
    }

    public void dialogShow(View view, String mail, String name, Double lat, Double lng, int radius, DocumentReference documentReference){
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
            args.putInt("fragment_radius", radius);
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
            mainActivity.refDataAccess.insertRef(documentReference.getId(),mail);
            Toast.makeText(view.getContext(), "Kaydedildi", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        message.setOnClickListener(v ->{

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

    private void getData(String cityFind,String districtFind,String placeFind,String date1Find,String date2Find,String time1Find,String time2Find,int radiusFind,double latFind,double lngFind){
        boolean checkDistrict,checkPlace,checkDateAndTime,checkField;
        checkDistrict = !districtFind.isEmpty();
        checkPlace = !placeFind.isEmpty();
        checkDateAndTime = !date1Find.isEmpty() && !date2Find.isEmpty() && !time1Find.isEmpty() && !time2Find.isEmpty();
        checkField = radiusFind != 0 && latFind != 0 && lngFind != 0;

        if(checkDistrict && checkPlace && checkDateAndTime && checkField){

        }else if (checkDistrict && checkPlace && checkField) {

        }else if (checkDistrict && checkDateAndTime && checkField) {

        } else if (checkDistrict && checkPlace && checkDateAndTime) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            Query query = firestore.collection(collection)
                    .whereEqualTo("district", districtFind)
                    .whereEqualTo("place",placeFind)
                    .whereLessThanOrEqualTo("date2",date1Find)
                    .whereGreaterThanOrEqualTo("date1",date2Find);

        } else if (checkDistrict && checkPlace) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            Query query = firestore.collection(collection)
                    .whereEqualTo("district", districtFind)
                    .whereEqualTo("place",placeFind);

        }else if (checkDistrict && checkDateAndTime) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            Query query = firestore.collection(collection)
                    .whereEqualTo("district", districtFind)
                    .whereLessThanOrEqualTo("date2",date1Find)
                    .whereGreaterThanOrEqualTo("date1",date2Find);
        } else if (checkDistrict) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            Query query = firestore.collection(collection)
                    .whereEqualTo("district", districtFind);

            query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if(!queryDocumentSnapshots.isEmpty()){
                        binding.shimmerLayout.stopShimmer();
                        binding.shimmerLayout.setVisibility(View.GONE);
                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
                    }
                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
                        String imageUrl = querySnapshot.getString("imageUrl");
                        String name = querySnapshot.getString("name");
                        String mail = querySnapshot.getString("mail");
                        String city = querySnapshot.getString("city");
                        String district = querySnapshot.getString("district");
                        String time1 = querySnapshot.getString("time1");
                        String time2 = querySnapshot.getString("time2");
                        String date1 = querySnapshot.getString("date1");
                        String date2 = querySnapshot.getString("date2");
                        String place = querySnapshot.getString("place");
                        String explain = querySnapshot.getString("explain");
                        Double lat = querySnapshot.getDouble("lat");
                        Double lng = querySnapshot.getDouble("lng");
                        Long x = querySnapshot.getLong("radius");
                        int radius = 0;
                        if(x != null){
                            radius = x.intValue();
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
                        post.time1 = time1;
                        post.time2 = time2;
                        post.date1 = date1;
                        post.date2 = date2;
                        post.place = place;
                        post.explain = explain;
                        post.timestamp = timestamp;
                        post.lat = lat;
                        post.lng = lng;
                        post.radius = radius;
                        post.documentReference = documentReference;

                        postArrayList.add(post);
                        postAdapter.notifyDataSetChanged();

                    }
                });
        }else {
            postArrayList.clear();
            String collection = "post" + cityFind;
            Query query = firestore.collection(collection);

            query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if(!queryDocumentSnapshots.isEmpty()){
                        binding.shimmerLayout.stopShimmer();
                        binding.shimmerLayout.setVisibility(View.GONE);
                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
                    }
                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
                        String imageUrl = querySnapshot.getString("imageUrl");
                        String name = querySnapshot.getString("name");
                        String mail = querySnapshot.getString("mail");
                        String city = querySnapshot.getString("city");
                        String district = querySnapshot.getString("district");
                        String time1 = querySnapshot.getString("time1");
                        String time2 = querySnapshot.getString("time2");
                        String date1 = querySnapshot.getString("date1");
                        String date2 = querySnapshot.getString("date2");
                        String place = querySnapshot.getString("place");
                        String explain = querySnapshot.getString("explain");
                        Double lat = querySnapshot.getDouble("lat");
                        Double lng = querySnapshot.getDouble("lng");
                        Long x = querySnapshot.getLong("radius");
                        int radius = 0;
                        if(x != null){
                            radius = x.intValue();
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
                        post.time1 = time1;
                        post.time2 = time2;
                        post.date1 = date1;
                        post.date2 = date2;
                        post.place = place;
                        post.explain = explain;
                        post.timestamp = timestamp;
                        post.lat = lat;
                        post.lng = lng;
                        post.radius = radius;
                        post.documentReference = documentReference;

                        postArrayList.add(post);
                        postAdapter.notifyDataSetChanged();

                    }
                });
        }


    }

    private void getData(){
        firestore.collection("post"+"İstanbul").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if(!queryDocumentSnapshots.isEmpty()){
                binding.shimmerLayout.stopShimmer();
                binding.shimmerLayout.setVisibility(View.GONE);
                binding.recyclerViewMain.setVisibility(View.VISIBLE);
            }
            for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
                String imageUrl = querySnapshot.getString("imageUrl");
                String name = querySnapshot.getString("name");
                String mail = querySnapshot.getString("mail");
                String city = querySnapshot.getString("city");
                String district = querySnapshot.getString("district");
                String time1 = querySnapshot.getString("time1");
                String time2 = querySnapshot.getString("time2");
                String date1 = querySnapshot.getString("date1");
                String date2 = querySnapshot.getString("date2");
                String place = querySnapshot.getString("place");
                String explain = querySnapshot.getString("explain");
                Double lat = querySnapshot.getDouble("lat");
                Double lng = querySnapshot.getDouble("lng");
                Long x = querySnapshot.getLong("radius");
                int radius = 0;
                if(x != null){
                    radius = x.intValue();
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
                post.time1 = time1;
                post.time2 = time2;
                post.date1 = date1;
                post.date2 = date2;
                post.place = place;
                post.explain = explain;
                post.timestamp = timestamp;
                post.lat = lat;
                post.lng = lng;
                post.radius = radius;
                post.documentReference = documentReference;

                postArrayList.add(post);
                postAdapter.notifyDataSetChanged();

            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
}