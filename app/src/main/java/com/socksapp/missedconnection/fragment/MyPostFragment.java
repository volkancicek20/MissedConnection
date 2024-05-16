package com.socksapp.missedconnection.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.MyPostAdapter;
import com.socksapp.missedconnection.adapter.PostAdapter;
import com.socksapp.missedconnection.databinding.FragmentMyPostBinding;
import com.socksapp.missedconnection.model.FindPost;

import java.util.ArrayList;

public class MyPostFragment extends Fragment {

    private FragmentMyPostBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String userMail;
    private MainActivity mainActivity;
    private SharedPreferences nameShared,imageUrlShared;
    public MyPostAdapter myPostAdapter;
    public ArrayList<FindPost> postArrayList;
    private Handler handler;

    public MyPostFragment() {
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
        binding = FragmentMyPostBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.shimmerLayout.startShimmer();

        userMail = user.getEmail();

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.includedLayout.setVisibility(View.VISIBLE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);

        binding.recyclerViewMyPost.setLayoutManager(new LinearLayoutManager(view.getContext()));
        myPostAdapter = new MyPostAdapter(postArrayList,view.getContext(),MyPostFragment.this);
        binding.recyclerViewMyPost.setAdapter(myPostAdapter);
        postArrayList.clear();

        handler = new Handler();

        getData();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    public void dialogShow(View view,String city, Double lat, Double lng, double radius, DocumentReference documentReference,int position){
        final Dialog dialog = new Dialog(view.getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_layout_2);

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

        LinearLayout delete = dialog.findViewById(R.id.layoutDelete);

        delete.setOnClickListener(v ->{

            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setMessage("Paylaşımı silmek istiyor musunuz?");
            builder.setPositiveButton("Sil", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    WriteBatch batch = firestore.batch();

                    batch.delete(firestore.collection(userMail).document(documentReference.getId()));
                    batch.delete(firestore.collection("post"+city).document(documentReference.getId()));

                    batch.commit()
                        .addOnSuccessListener(aVoid -> {
                            mainActivity.refDataAccess.deleteRef(documentReference.getId());
                            myPostAdapter.notifyItemRemoved(position);
                            postArrayList.remove(position);
                            myPostAdapter.notifyDataSetChanged();
                            dialog.dismiss();
                            dialog2.dismiss();
                        })
                        .addOnFailureListener(e -> {

                        });
                }
            });
            builder.setNegativeButton("Geri", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    dialog2.dismiss();
                }
            });

            builder.show();

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
        firestore.collection(userMail).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if(queryDocumentSnapshots.isEmpty()){
                FindPost post = new FindPost();
                post.viewType = 2;

                postArrayList.add(post);
                binding.shimmerLayout.stopShimmer();
                binding.shimmerLayout.setVisibility(View.GONE);
                binding.recyclerViewMyPost.setVisibility(View.VISIBLE);
                myPostAdapter.notifyDataSetChanged();

                return;
            }
            for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
//                    String imageUrl = querySnapshot.getString("imageUrl");
//                    String name = querySnapshot.getString("name");
//                    String mail = querySnapshot.getString("mail");
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
                post.imageUrl = imageUrlShared.getString("imageUrl","");
                post.name = nameShared.getString("name","");
                post.mail = userMail;
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
                binding.recyclerViewMyPost.setVisibility(View.VISIBLE);
                myPostAdapter.notifyDataSetChanged();
            }
        });
    }

    public void goAddPost(){
        AddPostFragment fragment = new AddPostFragment();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
}