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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SavedPostFragment extends Fragment {

    private FragmentSavedPostBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private MainActivity mainActivity;
    private String userMail,myUserName;
    private SharedPreferences nameShared,language;
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
        savedPostArrayList = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        language = requireActivity().getSharedPreferences("Language",Context.MODE_PRIVATE);
        nameShared = requireActivity().getSharedPreferences("Name", Context.MODE_PRIVATE);
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

        myUserName = nameShared.getString("name","");
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

    private void getData() {
        firestore.collection("saves")
            .document(userMail)
            .collection(userMail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {

                if (queryDocumentSnapshots.isEmpty()) {
                    addEmptyPostView();
                    return;
                }

                lastVisibleSavedPost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                    String mail = querySnapshot.getString("mail");
                    String refId = querySnapshot.getString("refId");

                    if (mail != null && !mail.isEmpty() &&  refId != null && !refId.isEmpty()) {
                        Task<DocumentSnapshot> task = firestore.collection("myPosts")
                            .document(mail)
                            .collection(mail)
                            .document(refId)
                            .get();
                        tasks.add(task);
                    }
                }

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(taskResults -> {
                    for (Object result : taskResults) {
                        if (result instanceof DocumentSnapshot) {
                            DocumentSnapshot documentSnapshot = (DocumentSnapshot) result;
                            if (documentSnapshot.exists()) {
                                CompletableFuture<Void> future = createPostFromSnapshot(documentSnapshot)
                                        .thenAccept(post -> savedPostArrayList.add(post));
                                futures.add(future);
//                                FindPost post = createPostFromDocument(documentSnapshot);
//                                savedPostArrayList.add(post);
                            }else {
                                FindPost post = new FindPost();
                                post.viewType = 3;
                                post.documentReference = documentSnapshot.getReference();
                                savedPostArrayList.add(post);
                            }
                        }
                    }
                    CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                    allFutures.thenRun(() -> {
                        binding.shimmerLayout.stopShimmer();
                        binding.shimmerLayout.setVisibility(View.GONE);
                        binding.recyclerViewSavedPost.setVisibility(View.VISIBLE);
                        savedPostAdapter.notifyDataSetChanged();
                    });
//                    binding.shimmerLayout.stopShimmer();
//                    binding.shimmerLayout.setVisibility(View.GONE);
//                    binding.recyclerViewSavedPost.setVisibility(View.VISIBLE);
//                    savedPostAdapter.notifyDataSetChanged();
                }).addOnFailureListener(e -> {
                    // Hata işleme kodları
                });

            })
            .addOnFailureListener(e -> {
                binding.shimmerLayout.stopShimmer();
                binding.shimmerLayout.setVisibility(View.GONE);
                binding.recyclerViewSavedPost.setVisibility(View.VISIBLE);
                // Hata işleme kodları
            });
    }

    private void loadMoreSavedPost() {
        if (lastVisibleSavedPost == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        firestore.collection("saves")
            .document(userMail)
            .collection(userMail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastVisibleSavedPost)
            .limit(pageSize)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                binding.progressBar.setVisibility(View.GONE);

                if (queryDocumentSnapshots.isEmpty()) return;

                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                    String mail = querySnapshot.getString("mail");
                    String refId = querySnapshot.getString("refId");

                    if (mail != null && refId != null) {
                        tasks.add(fetchPostDetails(mail, refId));
                    }
                }

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(taskResults -> {
                    List<FindPost> newPosts = new ArrayList<>();
                    for (Object result : taskResults) {
                        if (result instanceof DocumentSnapshot) {
                            DocumentSnapshot documentSnapshot = (DocumentSnapshot) result;
                            if (documentSnapshot.exists()) {
                                CompletableFuture<Void> future = createPostFromSnapshot(documentSnapshot)
                                        .thenAccept(post -> newPosts.add(post));
                                futures.add(future);
//                                FindPost post = createPostFromDocument(documentSnapshot);
//                                newPosts.add(post);
                            }else {
                                FindPost post = new FindPost();
                                post.viewType = 3;
                                post.documentReference = documentSnapshot.getReference();
                                newPosts.add(post);
                            }
                        }
                    }

                    CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                    allFutures.thenRun(() -> {
                        savedPostArrayList.addAll(newPosts);
                        savedPostAdapter.notifyItemRangeInserted(savedPostArrayList.size() - newPosts.size(), newPosts.size());
                    });

//                    savedPostArrayList.addAll(newPosts);
//                    savedPostAdapter.notifyItemRangeInserted(savedPostArrayList.size() - newPosts.size(), newPosts.size());

                    if(!queryDocumentSnapshots.isEmpty()){
                        lastVisibleSavedPost = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                    }

                }).addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                });
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
            });
    }

    private Task<DocumentSnapshot> fetchPostDetails(String mail, String refId) {
        return firestore.collection("myPosts")
            .document(mail)
            .collection(mail)
            .document(refId)
            .get();
    }


    private CompletableFuture<FindPost> createPostFromSnapshot(DocumentSnapshot querySnapshot) {
        CompletableFuture<FindPost> future = new CompletableFuture<>();

//        String imageUrl = querySnapshot.getString("imageUrl");
        String galleryUrl = querySnapshot.getString("galleryUrl");
//        String name = querySnapshot.getString("name");
        String mail = querySnapshot.getString("mail");
        String city = querySnapshot.getString("city");
        String district = querySnapshot.getString("district");
        Long time1 = querySnapshot.getLong("time1");
        Long time2 = querySnapshot.getLong("time2");
        Long date1 = querySnapshot.getLong("date1");
        Long date2 = querySnapshot.getLong("date2");
        String explain = querySnapshot.getString("explain");
        Double lat = querySnapshot.getDouble("lat");
        Double lng = querySnapshot.getDouble("lng");
        Long x = querySnapshot.getLong("radius");
        double radius = (x != null) ? x : 0;
        Timestamp timestamp = querySnapshot.getTimestamp("timestamp");
        DocumentReference documentReference = querySnapshot.getReference();

        FindPost post = new FindPost();
        post.viewType = 1;
        post.galleryUrl = galleryUrl;
        post.mail = mail;
        post.city = city;
        post.district = district;
        post.time1 = (time1 != null) ? time1 : 0;
        post.time2 = (time2 != null) ? time2 : 0;
        post.date1 = (date1 != null) ? date1 : 0;
        post.date2 = (date2 != null) ? date2 : 0;
        post.explain = explain;
        post.timestamp = timestamp;
        post.lat = lat;
        post.lng = lng;
        post.radius = radius;
        post.documentReference = documentReference;

        firestore.collection("users").document(mail).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot userSnapshot = task.getResult();
                    String userName = userSnapshot.getString("name");
                    String userImageUrl = userSnapshot.getString("imageUrl");

                    post.name = userName;
                    post.imageUrl = userImageUrl;
                }

                future.complete(post);
            }
        });

        return future;
    }

    private void addEmptyPostView() {
        FindPost post = new FindPost();
        post.viewType = 2;
        savedPostArrayList.add(post);
        binding.shimmerLayout.stopShimmer();
        binding.shimmerLayout.setVisibility(View.GONE);
        binding.recyclerViewSavedPost.setVisibility(View.VISIBLE);
        savedPostAdapter.notifyDataSetChanged();
    }

    public void goMain(){
        MainFragment fragment = new MainFragment();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mainActivity.bottomNavigationView.setSelectedItemId(R.id.navHome);
    }

    public void removeSaved(View view,String ref,int position){

        DocumentReference docRef = firestore.collection("saves").document(userMail).collection(userMail).document(ref);

        docRef.delete().addOnSuccessListener(unused -> {
            savedPostArrayList.remove(position);
            savedPostAdapter.notifyItemRemoved(position);
            savedPostAdapter.notifyDataSetChanged();
            showSnackbar(view,getString(R.string.kaydedilenlerden_silindi));
        }).addOnFailureListener(e -> {
            showSnackbar(view,getString(R.string.bir_hata_olu_tu_l_tfen_daha_sonra_tekrar_deneyiniz));
        });
    }

    public void dialogShow(View view, String mail, String name, Double lat, Double lng, double radius, String ref,int position){
        BottomSheetDialog dialog = new BottomSheetDialog(view.getContext(),R.style.BottomSheetDialog);
        View bottomSheetView = LayoutInflater.from(view.getContext()).inflate(R.layout.bottom_sheet_layout, null);
        dialog.setContentView(bottomSheetView);

//        final Dialog dialog = new Dialog(view.getContext());
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        dialog.setContentView(R.layout.bottom_sheet_layout);

        LinearLayout map = bottomSheetView.findViewById(R.id.layoutMap);
        LinearLayout save = bottomSheetView.findViewById(R.id.layoutSave);
        LinearLayout message = bottomSheetView.findViewById(R.id.layoutMessage);
        LinearLayout report = bottomSheetView.findViewById(R.id.layoutReport);
        LinearLayout layoutLine = bottomSheetView.findViewById(R.id.layoutLine);

        TextView saveText = bottomSheetView.findViewById(R.id.save_bottom_sheet_text);
        saveText.setText(getString(R.string.kaydedilenlerden_kald_r));

        if(userMail.equals(mail)){
            message.setVisibility(View.GONE);
            report.setVisibility(View.GONE);
            layoutLine.setVisibility(View.GONE);
        }

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

        save.setOnClickListener(v ->{
            dialog.dismiss();
            removeSaved(view,ref,position);
        });

        message.setOnClickListener(v ->{
            if(!myUserName.isEmpty()){
                if(!mail.equals(userMail)){
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
                    showSnackbar(view,getString(R.string.kendine_mesaj_g_nderemezsiniz));
                    dialog.dismiss();
                }
            }else {
                showSnackbar(view,getString(R.string.mesaj_g_ndermek_i_in_profilinizi_tamamlamal_s_n_z));
                dialog.dismiss();
            }
        });

        report.setOnClickListener(v ->{
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());

            View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog_report_user, null);
            builder.setView(dialogView);

            Button cancelButton = dialogView.findViewById(R.id.cancelButton);
            Button reportButton = dialogView.findViewById(R.id.reportButton);
            TextView title = dialogView.findViewById(R.id.dialogTitle);
            EditText explain = dialogView.findViewById(R.id.explain);

            AlertDialog dlg = builder.create();

            String getLanguage = language.getString("language","");
            String txt;
            if(getLanguage.equals("english")){
                txt = "You are reporting the user named " + name;
            }else {
                txt = name + " adlı kullanıcıyı bildiriyorsunuz!";
            }
            title.setText(txt);

            cancelButton.setOnClickListener(v2 -> {
                dlg.dismiss();
                dialog.dismiss();
            });

            reportButton.setOnClickListener(v3 -> {
                String text = explain.getText().toString().trim();
                if(!text.isEmpty()){
                    Map<String,Object> data = new HashMap<>();
                    data.put("report",text);
                    firestore.collection("report").document(mail).collection(mail).add(data).addOnSuccessListener(documentReference1 -> {
                        dlg.dismiss();
                        dialog.dismiss();
                        showSnackbar(view,getString(R.string.kullanici_bildirildi));
                    }).addOnFailureListener(e -> {
                        dlg.dismiss();
                        dialog.dismiss();
                        showSnackbar(view,getString(R.string.bir_hata_olu_tu_l_tfen_daha_sonra_tekrar_deneyiniz));
                    });
                }else {
                    dialog.dismiss();
                    dlg.dismiss();
                }
            });

            dlg.show();
        });

        // (Opsiyonel) BottomSheetDialog'un davranışını özelleştirme
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
        bottomSheetBehavior.setPeekHeight(300); // Başlangıç yüksekliği (piksel cinsinden)
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED); // Başlangıç durumu

        dialog.show(); // Dialog'u göster

//        if(dialog.getWindow() != null){
//            dialog.show();
//            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
//            dialog.getWindow().setGravity(Gravity.BOTTOM);
//        }
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