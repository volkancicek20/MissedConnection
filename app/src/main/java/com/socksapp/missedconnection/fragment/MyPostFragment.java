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
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.MyPostAdapter;
import com.socksapp.missedconnection.databinding.FragmentMyPostBinding;
import com.socksapp.missedconnection.model.FindPost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private DocumentSnapshot lastVisibleMyPost;
    private final int pageSize = 10;

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

        lastVisibleMyPost = null;

        handler = new Handler();

        getData();


        binding.recyclerViewMyPost.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int totalItemCount = Objects.requireNonNull(layoutManager).getItemCount();
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();

                    if (lastVisibleItem >= totalItemCount - 1) {
                        loadMoreMyPost();
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

    public void dialogShow(View view,String city, Double lat, Double lng, double radius, DocumentReference documentReference,int position){
        BottomSheetDialog dialog = new BottomSheetDialog(view.getContext(),R.style.BottomSheetDialog);
        View bottomSheetView = LayoutInflater.from(view.getContext()).inflate(R.layout.bottom_sheet_layout_2, null);
        dialog.setContentView(bottomSheetView);

        LinearLayout map = bottomSheetView.findViewById(R.id.layoutMap);
        LinearLayout line = bottomSheetView.findViewById(R.id.layoutLine);

        if(lat == 0.0 && lng == 0.0){
            map.setVisibility(View.GONE);
            line.setVisibility(View.GONE);
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

            View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog_delete_post, null);
            builder.setView(dialogView);

            Button cancelButton = dialogView.findViewById(R.id.cancelButton);
            Button deleteButton = dialogView.findViewById(R.id.deleteButton);

            AlertDialog dlg = builder.create();

            cancelButton.setOnClickListener(v2 -> {
                dlg.dismiss();
                dialog.dismiss();
            });

            deleteButton.setOnClickListener(v3 -> {
                WriteBatch batch = firestore.batch();

                batch.delete(firestore.collection("myPosts").document(userMail).collection(userMail).document(documentReference.getId()));
                batch.delete(firestore.collection("posts").document("post"+city).collection("post"+city).document(documentReference.getId()));

                CollectionReference subCollectionRef = firestore.collection("views").document(userMail).collection(userMail);
                Query query = subCollectionRef.whereEqualTo("refId", documentReference.getId());

                query.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            batch.delete(document.getReference());
                        }
                        batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                myPostAdapter.notifyItemRemoved(position);
                                postArrayList.remove(position);
                                myPostAdapter.notifyDataSetChanged();
                                dialog.dismiss();
                                dlg.dismiss();
                                showSnackbar(view,getString(R.string.gonderiniz_silindi));
                            })
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                dlg.dismiss();
                                showSnackbar(view,getString(R.string.gonderi_silinemedi_l_tfen_internet_ba_lant_n_z_kontrol_edin));
                            });
                    } else {
                        dialog.dismiss();
                        dlg.dismiss();
                        showSnackbar(view,getString(R.string.gonderi_silinemedi_l_tfen_internet_ba_lant_n_z_kontrol_edin));
                    }
                });
            });

            dlg.show();
        });

        // (Opsiyonel) BottomSheetDialog'un davranışını özelleştirme
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
        bottomSheetBehavior.setPeekHeight(300); // Başlangıç yüksekliği (piksel cinsinden)
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Başlangıç durumu

        dialog.show();
    }

    private void getData() {
        firestore.collection("myPosts")
            .document(userMail)
            .collection(userMail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize)
            .get()
            .addOnSuccessListener(this::handleSuccess);
    }

    private void handleSuccess(QuerySnapshot queryDocumentSnapshots) {

        if (!queryDocumentSnapshots.isEmpty()) {
            lastVisibleMyPost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
        }

        boolean found = false;
        for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
            found = true;
            FindPost post = createPostFromSnapshot(querySnapshot);
            postArrayList.add(post);
        }

        if (found) {
            binding.shimmerLayout.stopShimmer();
            binding.shimmerLayout.setVisibility(View.GONE);
            binding.recyclerViewMyPost.setVisibility(View.VISIBLE);
        } else {
            displayNoPostsFoundMessage();
        }

        myPostAdapter.notifyDataSetChanged();
    }

    private void displayNoPostsFoundMessage() {
        FindPost post = new FindPost();
        post.viewType = 2;
        postArrayList.add(post);
        binding.shimmerLayout.stopShimmer();
        binding.shimmerLayout.setVisibility(View.GONE);
        binding.recyclerViewMyPost.setVisibility(View.VISIBLE);
        myPostAdapter.notifyDataSetChanged();
    }

    private void loadMoreMyPost() {
        if (lastVisibleMyPost == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        firestore.collection("myPosts")
            .document(userMail)
            .collection(userMail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastVisibleMyPost)
            .limit(pageSize)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                binding.progressBar.setVisibility(View.GONE);

                if (queryDocumentSnapshots.isEmpty()) return;

                List<FindPost> newPosts = new ArrayList<>();
                for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                    FindPost post = createPostFromSnapshot(querySnapshot);
                    newPosts.add(post);
                }

                postArrayList.addAll(newPosts);
                myPostAdapter.notifyItemRangeInserted(postArrayList.size() - newPosts.size(), newPosts.size());

                if(!queryDocumentSnapshots.isEmpty()){
                    lastVisibleMyPost = queryDocumentSnapshots.getDocuments()
                            .get(queryDocumentSnapshots.size() - 1);
                }
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
            });
    }

    private FindPost createPostFromSnapshot(QueryDocumentSnapshot querySnapshot) {
        String galleryUrl = querySnapshot.getString("galleryUrl");
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
        double radius = (x != null) ? x : 0;
        Timestamp timestamp = querySnapshot.getTimestamp("timestamp");
        DocumentReference documentReference = querySnapshot.getReference();

        FindPost post = new FindPost();
        post.viewType = 1;
        post.imageUrl = imageUrlShared.getString("imageUrl", "");
        post.galleryUrl = galleryUrl;
        post.name = nameShared.getString("name", "");
        post.mail = userMail;
        post.city = city;
        post.district = district;
        post.time1 = (time1 != null) ? time1 : 0;
        post.time2 = (time2 != null) ? time2 : 0;
        post.date1 = (date1 != null) ? date1 : 0;
        post.date2 = (date2 != null) ? date2 : 0;
        post.place = place;
        post.explain = explain;
        post.timestamp = timestamp;
        post.lat = lat;
        post.lng = lng;
        post.radius = radius;
        post.documentReference = documentReference;

        return post;
    }

    public void goAddPost(){
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