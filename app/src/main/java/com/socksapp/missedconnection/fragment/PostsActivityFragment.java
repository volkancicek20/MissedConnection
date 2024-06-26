package com.socksapp.missedconnection.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.PostNotificationAdapter;
import com.socksapp.missedconnection.databinding.FragmentPostsActivityBinding;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.model.PostNotification;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PostsActivityFragment extends Fragment {

    private FragmentPostsActivityBinding binding;
    private MainActivity mainActivity;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String myMail;
    private PostNotificationAdapter postNotificationAdapter;
    public ArrayList<PostNotification> postNotificationArrayList;
    private DocumentSnapshot lastVisibleActivityPost;
    private final int pageSize = 10;

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

        lastVisibleActivityPost = null;

        getData();

        binding.recyclerViewPostActivity.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int totalItemCount = Objects.requireNonNull(layoutManager).getItemCount();
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();

                    if (lastVisibleItem >= totalItemCount - 1) {
                        loadMoreActivityPost();
                    }
                }
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

//    private void getData(){
//        CollectionReference collectionReference = firestore.collection("views").document(myMail).collection(myMail);
//        collectionReference.orderBy("timestamp", Query.Direction.DESCENDING).limit(pageSize).get().addOnSuccessListener(queryDocumentSnapshots -> {
//            if(queryDocumentSnapshots.isEmpty()){
//                PostNotification postNotification = new PostNotification();
//                postNotification.viewType = 2;
//
//                postNotificationArrayList.add(postNotification);
//                postNotificationAdapter.notifyDataSetChanged();
//
//                return;
//            }
//
//            lastVisibleActivityPost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
//
//            for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots){
//                String other_name = queryDocumentSnapshot.getString("name");
//                String action_explain = queryDocumentSnapshot.getString("type");
//                Timestamp timestamp2 = queryDocumentSnapshot.getTimestamp("timestamp");
//                String refId = queryDocumentSnapshot.getString("refId");
//                if(refId != null && !refId.isEmpty()){
//                    firestore.collection("myPosts").document(myMail).collection(myMail).document(refId).get().addOnSuccessListener(documentSnapshot -> {
//                        if(documentSnapshot.exists()){
//                            String imageUrl = documentSnapshot.getString("imageUrl");
//                            String galleryUrl = documentSnapshot.getString("galleryUrl");
//                            String name = documentSnapshot.getString("name");
//                            String mail = documentSnapshot.getString("mail");
//                            String city = documentSnapshot.getString("city");
//                            String district = documentSnapshot.getString("district");
//                            String explain = documentSnapshot.getString("explain");
//                            Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");
//
//                            PostNotification postNotification = new PostNotification();
//                            postNotification.viewType = 1;
//                            postNotification.imageUrl = imageUrl;
//                            postNotification.galleryUrl = galleryUrl;
//                            postNotification.mail = mail;
//                            postNotification.name = name;
//                            postNotification.other_name = other_name;
//                            postNotification.city = city;
//                            postNotification.district = district;
//                            postNotification.action_explain = action_explain;
//                            postNotification.explain = explain;
//                            postNotification.timestamp = timestamp;
//                            postNotification.timestamp2 = timestamp2;
//
//                            postNotificationArrayList.add(postNotification);
//                            binding.shimmerLayout.stopShimmer();
//                            binding.shimmerLayout.setVisibility(View.GONE);
//                            binding.recyclerViewPostActivity.setVisibility(View.VISIBLE);
//                            postNotificationAdapter.notifyDataSetChanged();
//                        }
//                    });
//                }
//            }
//        });
//    }

    private void getData() {
        CollectionReference collectionReference = firestore.collection("views").document(myMail).collection(myMail);
        collectionReference.orderBy("timestamp", Query.Direction.DESCENDING).limit(pageSize).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    PostNotification postNotification = new PostNotification();
                    postNotification.viewType = 2;
                    postNotificationArrayList.add(postNotification);
                    binding.shimmerLayout.stopShimmer();
                    binding.shimmerLayout.setVisibility(View.GONE);
                    binding.recyclerViewPostActivity.setVisibility(View.VISIBLE);
                    postNotificationAdapter.notifyDataSetChanged();
                    return;
                }

                lastVisibleActivityPost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                    String refId = queryDocumentSnapshot.getString("refId");
                    if (refId != null && !refId.isEmpty()) {
                        Task<DocumentSnapshot> task = firestore.collection("myPosts").document(myMail)
                                .collection(myMail).document(refId).get();
                        tasks.add(task);
                    }
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(taskResults -> {
                    List<PostNotification> newNotifications = new ArrayList<>();
                    for (int i = 0; i < taskResults.size(); i++) {
                        DocumentSnapshot documentSnapshot = (DocumentSnapshot) taskResults.get(i);
                        QueryDocumentSnapshot queryDocumentSnapshot = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(i);

                        if (documentSnapshot.exists()) {
                            PostNotification postNotification = createPostNotification(queryDocumentSnapshot, documentSnapshot);
                            newNotifications.add(postNotification);
                        }
                    }

                    postNotificationArrayList.addAll(newNotifications);
                    binding.shimmerLayout.stopShimmer();
                    binding.shimmerLayout.setVisibility(View.GONE);
                    binding.recyclerViewPostActivity.setVisibility(View.VISIBLE);
                    postNotificationAdapter.notifyDataSetChanged();
                }).addOnFailureListener(e -> {
                    // Handle failure
                });
            }).addOnFailureListener(e -> {
                // Handle failure
            });
    }

    private PostNotification createPostNotification(QueryDocumentSnapshot querySnapshot, DocumentSnapshot documentSnapshot) {
        String other_name = querySnapshot.getString("name");
        String action_explain = querySnapshot.getString("type");
        Timestamp timestamp2 = querySnapshot.getTimestamp("timestamp");

        String imageUrl = documentSnapshot.getString("imageUrl");
        String galleryUrl = documentSnapshot.getString("galleryUrl");
        String name = documentSnapshot.getString("name");
        String mail = documentSnapshot.getString("mail");
        String city = documentSnapshot.getString("city");
        String district = documentSnapshot.getString("district");
        String explain = documentSnapshot.getString("explain");
        Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");

        PostNotification postNotification = new PostNotification();
        postNotification.viewType = 1;
        postNotification.imageUrl = imageUrl;
        postNotification.galleryUrl = galleryUrl;
        postNotification.mail = mail;
        postNotification.name = name;
        postNotification.other_name = other_name;
        postNotification.city = city;
        postNotification.district = district;
        postNotification.action_explain = action_explain;
        postNotification.explain = explain;
        postNotification.timestamp = timestamp;
        postNotification.timestamp2 = timestamp2;

        return postNotification;
    }


//    private void loadMoreActivityPost(){
//        if(lastVisibleActivityPost != null){
//            CollectionReference collectionReference = firestore.collection("views").document(myMail).collection(myMail);
//            collectionReference.orderBy("timestamp", Query.Direction.DESCENDING).startAfter(lastVisibleActivityPost).limit(pageSize).get().addOnSuccessListener(queryDocumentSnapshots -> {
//                if(queryDocumentSnapshots.isEmpty()){
//                    PostNotification postNotification = new PostNotification();
//                    postNotification.viewType = 2;
//
//                    postNotificationArrayList.add(postNotification);
//                    postNotificationAdapter.notifyDataSetChanged();
//
//                    return;
//                }
//                if(!queryDocumentSnapshots.isEmpty()){
//                    lastVisibleActivityPost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
//                }
//                List<PostNotification> newPosts = new ArrayList<>();
//                for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots){
//                    String other_name = queryDocumentSnapshot.getString("name");
//                    String action_explain = queryDocumentSnapshot.getString("type");
//                    Timestamp timestamp2 = queryDocumentSnapshot.getTimestamp("timestamp");
//                    String refId = queryDocumentSnapshot.getString("refId");
//                    if(refId != null && !refId.isEmpty()){
//                        firestore.collection(myMail).document(refId).get().addOnSuccessListener(documentSnapshot -> {
//                            if(documentSnapshot.exists()){
//                                String imageUrl = documentSnapshot.getString("imageUrl");
//                                String galleryUrl = documentSnapshot.getString("galleryUrl");
//                                String name = documentSnapshot.getString("name");
//                                String mail = documentSnapshot.getString("mail");
//                                String city = documentSnapshot.getString("city");
//                                String district = documentSnapshot.getString("district");
//                                String explain = documentSnapshot.getString("explain");
//                                Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");
//
//                                PostNotification postNotification = new PostNotification();
//                                postNotification.viewType = 1;
//                                postNotification.imageUrl = imageUrl;
//                                postNotification.galleryUrl = galleryUrl;
//                                postNotification.mail = mail;
//                                postNotification.name = name;
//                                postNotification.other_name = other_name;
//                                postNotification.city = city;
//                                postNotification.district = district;
//                                postNotification.action_explain = action_explain;
//                                postNotification.explain = explain;
//                                postNotification.timestamp = timestamp;
//                                postNotification.timestamp2 = timestamp2;
//
//                                newPosts.add(postNotification);
//                            }
//                        });
//                    }
//                }
//                postNotificationArrayList.addAll(postNotificationArrayList.size(),newPosts);
//                postNotificationAdapter.notifyItemRangeInserted(postNotificationArrayList.size(), postNotificationArrayList.size());
//
//                if (!queryDocumentSnapshots.isEmpty()) {
//                    lastVisibleActivityPost = queryDocumentSnapshots.getDocuments()
//                            .get(queryDocumentSnapshots.size() - 1);
//                }
//            }).addOnFailureListener(e -> {
//
//            });
//        }
//    }

    private void loadMoreActivityPost() {
        if (lastVisibleActivityPost == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        CollectionReference collectionReference = firestore.collection("views").document(myMail).collection(myMail);
        collectionReference.orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastVisibleActivityPost).limit(pageSize).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {

                binding.progressBar.setVisibility(View.GONE);

                if (queryDocumentSnapshots.isEmpty()) return;

                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                    String refId = queryDocumentSnapshot.getString("refId");
                    if (refId != null && !refId.isEmpty()) {
                        Task<DocumentSnapshot> task = firestore.collection("myPosts").document(myMail)
                                .collection(myMail).document(refId).get();
                        tasks.add(task);
                    }
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(taskResults -> {
                    List<PostNotification> newNotifications = new ArrayList<>();
                    for (int i = 0; i < taskResults.size(); i++) {
                        DocumentSnapshot documentSnapshot = (DocumentSnapshot) taskResults.get(i);
                        QueryDocumentSnapshot queryDocumentSnapshot = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(i);

                        if (documentSnapshot.exists()) {
                            PostNotification postNotification = createPostNotification(queryDocumentSnapshot, documentSnapshot);
                            newNotifications.add(postNotification);
                        }
                    }

                    postNotificationArrayList.addAll(newNotifications);
                    postNotificationAdapter.notifyItemRangeInserted(postNotificationArrayList.size() - newNotifications.size(), newNotifications.size());

                    if(!queryDocumentSnapshots.isEmpty()){
                        lastVisibleActivityPost = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                    }

                }).addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                });
            }).addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
            });
    }

    public void goToAddPostFragment(){
        AddPostFragment fragment = new AddPostFragment();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mainActivity.bottomNavigationView.setSelectedItemId(R.id.navAdd);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof MainActivity){
            mainActivity = (MainActivity) context;
        }
    }
}