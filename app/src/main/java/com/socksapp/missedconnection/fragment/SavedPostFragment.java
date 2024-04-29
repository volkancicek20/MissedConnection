package com.socksapp.missedconnection.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.PostAdapter;
import com.socksapp.missedconnection.adapter.SavedPostAdapter;
import com.socksapp.missedconnection.databinding.FragmentSavedPostBinding;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.model.RefItem;

import java.util.ArrayList;
import java.util.List;

public class SavedPostFragment extends Fragment {

    private FragmentSavedPostBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private MainActivity mainActivity;
    private List<RefItem> arrayList;
    private String userMail;
    public SavedPostAdapter savedPostAdapter;
    public ArrayList<FindPost> savedPostArrayList;

    public SavedPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        arrayList = new ArrayList<>();
        savedPostArrayList = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSavedPostBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);

        userMail = user.getEmail();

        binding.recyclerViewSavedPost.setLayoutManager(new LinearLayoutManager(view.getContext()));
        savedPostAdapter = new SavedPostAdapter(savedPostArrayList,view.getContext(),SavedPostFragment.this);
        binding.recyclerViewSavedPost.setAdapter(savedPostAdapter);
        savedPostArrayList.clear();

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

    public void removeSaved(View view,DocumentReference ref,int position){
        mainActivity.refDataAccess.deleteRef(ref.getId());
        savedPostArrayList.remove(position);
        savedPostAdapter.notifyItemRemoved(position);
        savedPostAdapter.notifyDataSetChanged();
        Toast.makeText(view.getContext(),"Kaydedilenlerden Silindi",Toast.LENGTH_SHORT).show();
    }

    private void getData(){
        arrayList = mainActivity.refDataAccess.getAllRefs();

        if(arrayList.isEmpty()){
            FindPost post = new FindPost();
            post.viewType = 2;
            savedPostArrayList.add(post);
            savedPostAdapter.notifyDataSetChanged();
        }else {
            for (RefItem item : arrayList) {
                String mail = item.getMail();
                String ref = item.getRef();

                firestore.collection(mail).document(ref).get().addOnSuccessListener(querySnapshot -> {
                    if(querySnapshot.exists()){
                        String imageUrl = querySnapshot.getString("imageUrl");
                        String name = querySnapshot.getString("name");
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

                        savedPostArrayList.add(post);
                        savedPostAdapter.notifyDataSetChanged();
                    }
                }).addOnFailureListener(e -> {
                    e.printStackTrace();
                });
            }
        }

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
}