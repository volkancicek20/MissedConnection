package com.socksapp.missedconnection.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.PostNotificationAdapter;
import com.socksapp.missedconnection.databinding.FragmentPostsActivityBinding;
import com.socksapp.missedconnection.model.PostNotification;

import java.util.ArrayList;

public class PostsActivityFragment extends Fragment {

    private FragmentPostsActivityBinding binding;
    private MainActivity mainActivity;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String myMail;
    private PostNotificationAdapter postNotificationAdapter;
    public ArrayList<PostNotification> postNotificationArrayList;

    public PostsActivityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        postNotificationArrayList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPostsActivityBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);


        binding.recyclerViewPostActivity.setLayoutManager(new LinearLayoutManager(view.getContext()));
        postNotificationAdapter = new PostNotificationAdapter(postNotificationArrayList,view.getContext(),PostsActivityFragment.this);
        binding.recyclerViewPostActivity.setAdapter(postNotificationAdapter);
        postNotificationArrayList.clear();

        myMail = user.getEmail();

        getData();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void getData(){
        CollectionReference collectionReference = firestore.collection("views").document(myMail).collection(myMail);
        collectionReference.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if(queryDocumentSnapshots.isEmpty()){
                PostNotification postNotification = new PostNotification();
                postNotification.viewType = 2;

                postNotificationArrayList.add(postNotification);
                postNotificationAdapter.notifyDataSetChanged();

                return;
            }
            for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots){
                String name = queryDocumentSnapshot.getString("name");
                String type = queryDocumentSnapshot.getString("type");
                Timestamp timestamp = queryDocumentSnapshot.getTimestamp("timestamp");
                String refId = queryDocumentSnapshot.getString("refId");
                if(refId != null && !refId.isEmpty()){
                    firestore.collection(myMail).document(refId).get().addOnSuccessListener(documentSnapshot -> {
                        if(documentSnapshot.exists()){
                            String city = documentSnapshot.getString("city");
                            String district = documentSnapshot.getString("district");
                            String explainPost = documentSnapshot.getString("explain");

                            PostNotification postNotification = new PostNotification();
                            postNotification.viewType = 1;
                            postNotification.name = name;
                            postNotification.city = city;
                            postNotification.district = district;
                            postNotification.explain = type;
                            postNotification.explain_post = explainPost;
                            postNotification.timestamp = timestamp;

                            postNotificationArrayList.add(postNotification);
                            postNotificationAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        }).addOnFailureListener(e -> {

        });

    }

    public void goToAddPostFragment(){
        AddPostFragment fragment = new AddPostFragment();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof MainActivity){
            mainActivity = (MainActivity) context;
        }
    }
}