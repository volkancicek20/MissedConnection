package com.socksapp.missedconnection.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.SavedPostAdapter;
import com.socksapp.missedconnection.databinding.FragmentSavedPostBinding;
import com.socksapp.missedconnection.model.ChatsId;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.model.RefItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SavedPostFragment extends Fragment {

    private FragmentSavedPostBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private MainActivity mainActivity;
    private List<RefItem> refItemList;
    private List<ChatsId> chatsIdList;
    private String userMail;
    public SavedPostAdapter savedPostAdapter;
    public ArrayList<FindPost> savedPostArrayList;
    private Handler handler;
    private DocumentSnapshot lastVisibleSavedPost;
    private final int pageSize = 10;

    public SavedPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refItemList = new ArrayList<>();
        chatsIdList = new ArrayList<>();
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

        binding.shimmerLayout.startShimmer();

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);

        userMail = user.getEmail();

        binding.recyclerViewSavedPost.setLayoutManager(new LinearLayoutManager(view.getContext()));
        savedPostAdapter = new SavedPostAdapter(savedPostArrayList,view.getContext(),SavedPostFragment.this);
        binding.recyclerViewSavedPost.setAdapter(savedPostAdapter);
        savedPostArrayList.clear();

        lastVisibleSavedPost = null;

        handler = new Handler();

        getData();

        binding.recyclerViewSavedPost.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int totalItemCount = Objects.requireNonNull(layoutManager).getItemCount();
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();

                    if (lastVisibleItem >= totalItemCount - 1) {
                        loadMoreSavedPost();
                    }
                }
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    public void removeSaved(View view,String ref,int position){

        DocumentReference docRef = firestore.collection("saves").document(userMail).collection(userMail).document(ref);

        docRef.delete().addOnSuccessListener(unused -> {
            savedPostArrayList.remove(position);
            savedPostAdapter.notifyItemRemoved(position);
            savedPostAdapter.notifyDataSetChanged();
            showSnackbar(view,getString(R.string.kaydedilenlerden_silindi));
        }).addOnFailureListener(e -> {

        });
    }

//    public void removeSaved(View view,DocumentReference ref,int position){
//        mainActivity.refDataAccess.deleteRef(ref.getId());
//        savedPostArrayList.remove(position);
//        savedPostAdapter.notifyItemRemoved(position);
//        savedPostAdapter.notifyDataSetChanged();
//        Toast.makeText(view.getContext(),"Kaydedilenlerden Silindi",Toast.LENGTH_SHORT).show();
//    }

    private void getData(){
        firestore.collection("saves")
            .document(userMail).collection(userMail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize)
            .get().addOnSuccessListener(queryDocumentSnapshots -> {
                if(queryDocumentSnapshots.isEmpty()){
                    FindPost post = new FindPost();
                    post.viewType = 2;

                    savedPostArrayList.add(post);
                    binding.shimmerLayout.stopShimmer();
                    binding.shimmerLayout.setVisibility(View.GONE);
                    binding.recyclerViewSavedPost.setVisibility(View.VISIBLE);
                    savedPostAdapter.notifyDataSetChanged();
                    return;
                }
                if(!queryDocumentSnapshots.isEmpty()){
                    lastVisibleSavedPost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                }
                for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
                    String mail = querySnapshot.getString("mail");
                    String refId = querySnapshot.getString("refId");
                    if(mail != null && refId != null){
                        firestore.collection("myPosts")
                            .document(mail)
                            .collection(mail)
                            .document(refId)
                            .get().addOnSuccessListener(documentSnapshot -> {
                                if(documentSnapshot.exists()){
                                    String imageUrl = documentSnapshot.getString("imageUrl");
                                    String galleryUrl = documentSnapshot.getString("galleryUrl");
                                    String name = documentSnapshot.getString("name");
                                    String city = documentSnapshot.getString("city");
                                    String district = documentSnapshot.getString("district");
                                    Long time1 = documentSnapshot.getLong("time1");
                                    Long time2 = documentSnapshot.getLong("time2");
                                    Long date1 = documentSnapshot.getLong("date1");
                                    Long date2 = documentSnapshot.getLong("date2");
                                    String place = documentSnapshot.getString("place");
                                    String explain = documentSnapshot.getString("explain");
                                    Double lat = documentSnapshot.getDouble("lat");
                                    Double lng = documentSnapshot.getDouble("lng");
                                    Long x = documentSnapshot.getLong("radius");
                                    double radius = 0;
                                    if(x != null){
                                        radius = x;
                                    }
                                    Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");
                                    DocumentReference documentReference = documentSnapshot.getReference();

                                    FindPost post = new FindPost();
                                    post.viewType = 1;
                                    post.imageUrl = imageUrl;
                                    post.galleryUrl = galleryUrl;
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

                                    savedPostArrayList.add(post);
                                    binding.shimmerLayout.stopShimmer();
                                    binding.shimmerLayout.setVisibility(View.GONE);
                                    binding.recyclerViewSavedPost.setVisibility(View.VISIBLE);
                                    savedPostAdapter.notifyDataSetChanged();
                                }else {

                                }
                            }).addOnFailureListener(e -> {

                            });

                    }else {
                        // mail veya refId null
                    }
                }
        }).addOnFailureListener(e -> {

        });
    }

    private void loadMoreSavedPost(){
        if(lastVisibleSavedPost != null){
            binding.progressBar.setVisibility(View.VISIBLE);
            firestore.collection("saves")
                .document(userMail).collection(userMail)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisibleSavedPost)
                .limit(pageSize)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBar.setVisibility(View.GONE);
                    List<FindPost> newPosts = new ArrayList<>();
                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
                        String mail = querySnapshot.getString("mail");
                        String refId = querySnapshot.getString("refId");
                        if(mail != null && refId != null){
                            firestore.collection("myPosts")
                                .document(mail)
                                .collection(mail)
                                .document(refId)
                                .get().addOnSuccessListener(documentSnapshot -> {
                                    if(documentSnapshot.exists()){
                                        String imageUrl = documentSnapshot.getString("imageUrl");
                                        String galleryUrl = documentSnapshot.getString("galleryUrl");
                                        String name = documentSnapshot.getString("name");
                                        String city = documentSnapshot.getString("city");
                                        String district = documentSnapshot.getString("district");
                                        Long time1 = documentSnapshot.getLong("time1");
                                        Long time2 = documentSnapshot.getLong("time2");
                                        Long date1 = documentSnapshot.getLong("date1");
                                        Long date2 = documentSnapshot.getLong("date2");
                                        String place = documentSnapshot.getString("place");
                                        String explain = documentSnapshot.getString("explain");
                                        Double lat = documentSnapshot.getDouble("lat");
                                        Double lng = documentSnapshot.getDouble("lng");
                                        Long x = documentSnapshot.getLong("radius");
                                        double radius = 0;
                                        if(x != null){
                                            radius = x;
                                        }
                                        Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");
                                        DocumentReference documentReference = documentSnapshot.getReference();

                                        FindPost post = new FindPost();
                                        post.viewType = 1;
                                        post.imageUrl = imageUrl;
                                        post.galleryUrl = galleryUrl;
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

                                        newPosts.add(post);
                                    }else {
                                        // gönderi kaldırılmış
                                    }
                                }).addOnFailureListener(e -> {
                                    // gönderiyi alırken hata oluştu
                                });

                        }else {
                            // mail veya refId null
                        }
                    }

                    savedPostArrayList.addAll(savedPostArrayList.size(),newPosts);
                    savedPostAdapter.notifyItemRangeInserted(savedPostArrayList.size(), savedPostArrayList.size());

                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastVisibleSavedPost = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                    }

                }).addOnFailureListener(e -> {

                });
        }
    }

//    private void getData(){
//        refItemList = mainActivity.refDataAccess.getAllRefs();
//
//        if(refItemList.isEmpty()){
//            FindPost post = new FindPost();
//            post.viewType = 2;
//
//            savedPostArrayList.add(post);
//            binding.shimmerLayout.stopShimmer();
//            binding.shimmerLayout.setVisibility(View.GONE);
//            binding.recyclerViewSavedPost.setVisibility(View.VISIBLE);
//            savedPostAdapter.notifyDataSetChanged();
//
//        }else {
//            for (RefItem item : refItemList) {
//                String mail = item.getMail();
//                String ref = item.getRef();
//
//                firestore.collection(mail).document(ref).get().addOnSuccessListener(querySnapshot -> {
//                    if(querySnapshot.exists()){
//                        String imageUrl = querySnapshot.getString("imageUrl");
//                        String name = querySnapshot.getString("name");
//                        String city = querySnapshot.getString("city");
//                        String district = querySnapshot.getString("district");
//                        Long time1 = querySnapshot.getLong("time1");
//                        Long time2 = querySnapshot.getLong("time2");
//                        Long date1 = querySnapshot.getLong("date1");
//                        Long date2 = querySnapshot.getLong("date2");
//                        String place = querySnapshot.getString("place");
//                        String explain = querySnapshot.getString("explain");
//                        Double lat = querySnapshot.getDouble("lat");
//                        Double lng = querySnapshot.getDouble("lng");
//                        Long x = querySnapshot.getLong("radius");
//                        double radius = 0;
//                        if(x != null){
//                            radius = x;
//                        }
//                        Timestamp timestamp = querySnapshot.getTimestamp("timestamp");
//                        DocumentReference documentReference = querySnapshot.getReference();
//
//                        FindPost post = new FindPost();
//                        post.viewType = 1;
//                        post.imageUrl = imageUrl;
//                        post.name = name;
//                        post.mail = mail;
//                        post.city = city;
//                        post.district = district;
//                        if (time1 == null) {
//                            post.time1 = 0;
//                        } else {
//                            post.time1 = time1;
//                        }
//                        if (time2 == null) {
//                            post.time2 = 0;
//                        } else {
//                            post.time2 = time2;
//                        }
//                        if (date1 == null) {
//                            post.date1 = 0;
//                        } else {
//                            post.date1 = date1;
//                        }
//                        if (date2 == null) {
//                            post.date2 = 0;
//                        } else {
//                            post.date2 = date2;
//                        }
//                        post.place = place;
//                        post.explain = explain;
//                        post.timestamp = timestamp;
//                        post.lat = lat;
//                        post.lng = lng;
//                        post.radius = radius;
//                        post.documentReference = documentReference;
//
//                        savedPostArrayList.add(post);
//                        binding.shimmerLayout.stopShimmer();
//                        binding.shimmerLayout.setVisibility(View.GONE);
//                        binding.recyclerViewSavedPost.setVisibility(View.VISIBLE);
//                        savedPostAdapter.notifyDataSetChanged();
//                    }
//                }).addOnFailureListener(e -> {
//                    e.printStackTrace();
//                });
//            }
//        }
//
//    }

    public void goMain(){
        MainFragment fragment = new MainFragment();
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

    private void showSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        snackbar.setBackgroundTint(Color.rgb(48, 44, 44));

        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);

        snackbar.show();
    }
}