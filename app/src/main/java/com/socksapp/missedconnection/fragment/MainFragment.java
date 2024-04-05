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
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.PostAdapter;
import com.socksapp.missedconnection.databinding.FragmentFindBinding;
import com.socksapp.missedconnection.databinding.FragmentMainBinding;
import com.socksapp.missedconnection.model.FindPost;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

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

        userMail = user.getEmail();

        binding.recyclerViewMain.setLayoutManager(new LinearLayoutManager(view.getContext()));
        postAdapter = new PostAdapter(postArrayList,view.getContext(),MainFragment.this);
        binding.recyclerViewMain.setAdapter(postAdapter);
        postArrayList.clear();
        getData();
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                shutdown(view);
            }
        });

    }

    public void dialogShow(View view,String mail,String name,Double lat,Double lng,int radius,DocumentReference documentReference){
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

    private void getData(){
        firestore.collection("post"+"İstanbul").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
                    String imageUrl = querySnapshot.getString("imageUrl");
                    String name = querySnapshot.getString("name");
                    String city = querySnapshot.getString("city");
                    String district = querySnapshot.getString("district");
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
                    post.city = city;
                    post.district = district;
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
            }
        });
    }

    private void shutdown(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setMessage("Uygulamadan çıkış yapılsın mı?");
        builder.setPositiveButton("Çık", (dialog, which) -> {
            System.exit(0);
        });
        builder.setNegativeButton("Hayır", (dialog, which) -> {

        });
        builder.show();
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
}