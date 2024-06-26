package com.socksapp.missedconnection.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.socksapp.missedconnection.FCM.FCMNotificationSender;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.PostAdapter;
import com.socksapp.missedconnection.databinding.FragmentMainBinding;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.myclass.TimedDataManager;
import com.socksapp.missedconnection.myclass.Utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private SharedPreferences nameShared,imageUrlShared,myLocationCity,myLocationDistrict,language;
    private String userMail,myUserName,myImageUrl,userLocationCity,userLocationDistrict;
    private MainActivity mainActivity;
    public PostAdapter postAdapter;
    public ArrayList<FindPost> postArrayList;
    private Handler handler;
    private TimedDataManager timedDataManager;
    private FusedLocationProviderClient fusedLocationClient;
    public double checkRadius = 0,checkLat = 0,checkLng = 0;
    private DocumentSnapshot lastVisiblePost;
    private final int pageSize = 10;
    private String loadCity,loadDistrict;
    private Menu menu;
    private MenuItem menuItem;
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

        language = requireActivity().getSharedPreferences("Language",Context.MODE_PRIVATE);
        nameShared = requireActivity().getSharedPreferences("Name", Context.MODE_PRIVATE);
        imageUrlShared = requireActivity().getSharedPreferences("ImageUrl", Context.MODE_PRIVATE);
        myLocationCity = requireActivity().getSharedPreferences("MyLocationCity", Context.MODE_PRIVATE);
        myLocationDistrict = requireActivity().getSharedPreferences("MyLocationDistrict", Context.MODE_PRIVATE);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater,container,false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

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

        menu = mainActivity.navigationView.getMenu();
        menuItem = menu.findItem(R.id.nav_drawer_home);
        menuItem.setIcon(R.drawable.home_active_96);

        userMail = user.getEmail();

        timedDataManager = new TimedDataManager(view.getContext());

        myUserName = nameShared.getString("name","");
        myImageUrl = nameShared.getString("imageUrl","");
        userLocationCity = myLocationCity.getString("myLocationCity","");
        userLocationDistrict = myLocationDistrict.getString("myLocationDistrict","");

        binding.recyclerViewMain.setLayoutManager(new LinearLayoutManager(view.getContext()));
        postAdapter = new PostAdapter(postArrayList,view.getContext(),MainFragment.this);
        binding.recyclerViewMain.setAdapter(postAdapter);

        postArrayList.clear();

        lastVisiblePost = null;

        handler = new Handler();

        loadCity = "";
        loadDistrict = "";
        Bundle args = getArguments();
        if (args != null) {
            String city = args.getString("city","");
            String district = args.getString("district","");
            loadCity = city;
            loadDistrict = district;
            double radius = args.getDouble("radius",0);
            double latitude = args.getDouble("latitude",0);
            double longitude = args.getDouble("longitude",0);
            checkRadius = radius;
            checkLat = latitude;
            checkLng = longitude;
            long date1 = args.getLong("date1",0);
            long date2 = args.getLong("date2",0);
            long time1 = args.getLong("time1",0);
            long time2 = args.getLong("time2",0);

            mainActivity.bottomNavigationView.setSelectedItemId(R.id.navHome);

            getData(city,district,date1,date2,time1,time2,radius,latitude,longitude);
        }else {
            requestForPermission();
        }

        binding.recyclerViewMain.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int totalItemCount = Objects.requireNonNull(layoutManager).getItemCount();
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();

                    if (lastVisibleItem >= totalItemCount - 1) {
                        loadMorePost();
                    }
                }
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                System.exit(0);
            }
        });

    }

    private void getUserLocation(){
        if(ActivityCompat.checkSelfPermission(requireActivity(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        Task<Location> task = fusedLocationClient.getLastLocation();

        task.addOnSuccessListener(location -> {
            if(location != null){
                Geocoder geocoder = new Geocoder(requireActivity(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        userLocationCity = addresses.get(0).getAdminArea();
                        SharedPreferences.Editor editor = myLocationCity.edit();
                        editor.putString("myLocationCity",userLocationCity);
                        editor.apply();

                        userLocationDistrict = addresses.get(0).getSubAdminArea();
                        SharedPreferences.Editor editor2 = myLocationDistrict.edit();
                        editor2.putString("myLocationDistrict",userLocationDistrict);
                        editor2.apply();
                        getData();

//                        Toast.makeText(requireActivity(),userLocationCity+"/"+userLocationDistrict,Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else {
                userLocationCity = "İstanbul";
                SharedPreferences.Editor editor = myLocationCity.edit();
                editor.putString("myLocationCity",userLocationCity);
                editor.apply();

                userLocationDistrict = "Fatih";
                SharedPreferences.Editor editor2 = myLocationDistrict.edit();
                editor2.putString("myLocationDistrict",userLocationDistrict);
                editor2.apply();

                getData();
            }
        });
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false))) {
                    getUserLocation();
                } else if (Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false))) {
                    getUserLocation();
                } else {
                    userLocationCity = "İstanbul";
                    SharedPreferences.Editor editor = myLocationCity.edit();
                    editor.putString("myLocationCity",userLocationCity);
                    editor.apply();

                    userLocationDistrict = "Fatih";
                    SharedPreferences.Editor editor2 = myLocationDistrict.edit();
                    editor2.putString("myLocationDistrict",userLocationDistrict);
                    editor2.apply();

                    getData();
                }
            });

    private void requestForPermission() {
        requestPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        });
    }

    public void dialogShow(View view, String mail, String name, Double lat, Double lng, double radius, DocumentReference documentReference){
        BottomSheetDialog dialog = new BottomSheetDialog(view.getContext(),R.style.BottomSheetDialog);
        View bottomSheetView = LayoutInflater.from(view.getContext()).inflate(R.layout.bottom_sheet_layout, null);
        dialog.setContentView(bottomSheetView);

        LinearLayout map = bottomSheetView.findViewById(R.id.layoutMap);
        LinearLayout save = bottomSheetView.findViewById(R.id.layoutSave);
        LinearLayout message = bottomSheetView.findViewById(R.id.layoutMessage);
        LinearLayout report = bottomSheetView.findViewById(R.id.layoutReport);
        LinearLayout layoutLine = bottomSheetView.findViewById(R.id.layoutLine);

        if(userMail.equals(mail)){
            save.setVisibility(View.GONE);
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
            HashMap<String,Object> data = new HashMap<>();
            data.put("mail",mail);
            data.put("refId",documentReference.getId());
            data.put("timestamp",new Date());
            firestore.collection("saves").document(userMail).collection(userMail).document(documentReference.getId()).set(data).addOnSuccessListener(documentReference12 -> {
                showSnackbar(view,getString(R.string.kaydedildi));
                dialog.dismiss();
            }).addOnFailureListener(e -> {

            });
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

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
        bottomSheetBehavior.setPeekHeight(300);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        dialog.show();

    }

    private void getData(String cityFind,String districtFind,long date1Find,long date2Find,long time1Find,long time2Find,double radiusFind,double latFind,double lngFind){

        boolean checkDistrict,checkDate,checkTime,checkField;
        checkDistrict = !districtFind.isEmpty();
        checkDate = date1Find != 0 && date2Find != 0;
        checkTime = time1Find != 0 && time2Find != 0;
        checkField = radiusFind != 0 && latFind != 0 && lngFind != 0;

        Query query = null;

        if (checkDistrict && checkDate && checkTime && checkField) {
            postArrayList.clear();

            String collection = "post" + cityFind;
            query = firestore.collection("posts").document(collection).collection(collection)
                    .whereEqualTo("district", districtFind)
                    .whereLessThanOrEqualTo("date1",date2Find)
                    .whereGreaterThanOrEqualTo("date2",date1Find)
                    .whereLessThanOrEqualTo("time1",time2Find)
                    .whereGreaterThanOrEqualTo("time2",time1Find)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize);
        }
        else if (checkDistrict && checkDate && checkTime) {
            postArrayList.clear();

            String collection = "post" + cityFind;
            query = firestore.collection("posts").document(collection).collection(collection)
                    .whereEqualTo("district", districtFind)
                    .whereLessThanOrEqualTo("date1",date2Find)
                    .whereGreaterThanOrEqualTo("date2",date1Find)
                    .whereLessThanOrEqualTo("time1",time2Find)
                    .whereGreaterThanOrEqualTo("time2",time1Find)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize);
        }
        else if (checkDistrict && checkDate && checkField) {
            postArrayList.clear();

            String collection = "post" + cityFind;
            query = firestore.collection("posts").document(collection).collection(collection)
                    .whereEqualTo("district", districtFind)
                    .whereLessThanOrEqualTo("date1",date2Find)
                    .whereGreaterThanOrEqualTo("date2",date1Find)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize);
        }
        else if (checkDistrict && checkTime && checkField) {
            postArrayList.clear();

            String collection = "post" + cityFind;
            query = firestore.collection("posts").document(collection).collection(collection)
                    .whereEqualTo("district", districtFind)
                    .whereLessThanOrEqualTo("time1",time2Find)
                    .whereGreaterThanOrEqualTo("time2",time1Find)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize);
        }
        else if (checkDistrict && checkField) {
            postArrayList.clear();

            String collection = "post" + cityFind;
            query = firestore.collection("posts").document(collection).collection(collection)
                    .whereEqualTo("district", districtFind)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize);

        }
        else if (checkDistrict && checkDate) {
            postArrayList.clear();

            String collection = "post" + cityFind;
            query = firestore.collection("posts").document(collection).collection(collection)
                    .whereEqualTo("district", districtFind)
                    .whereLessThanOrEqualTo("date1",date2Find)
                    .whereGreaterThanOrEqualTo("date2",date1Find)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize);
        }
        else if (checkDistrict && checkTime) {
            postArrayList.clear();

            String collection = "post" + cityFind;
            query = firestore.collection("posts").document(collection).collection(collection)
                    .whereEqualTo("district", districtFind)
                    .whereLessThanOrEqualTo("time1",time2Find)
                    .whereGreaterThanOrEqualTo("time2",time1Find)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize);
        }
        else if (checkDistrict) {
            postArrayList.clear();

            String collection = "post" + cityFind;
            query = firestore.collection("posts").document(collection).collection(collection)
                    .whereEqualTo("district", districtFind)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(pageSize);
        }
        else {
//            postArrayList.clear();
//            String collection = "post" + cityFind;
//            query = firestore.collection(collection);
        }

        if(query != null){
            if(checkField){
                query.get().addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        displayNoPostsFoundMessage();
                        return;
                    }

                    lastVisiblePost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                    List<CompletableFuture<Void>> futures = new ArrayList<>();

                    boolean empty = true;

                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                        if (isPostWithinRadius(querySnapshot,latFind,lngFind,radiusFind)) {
                            empty = false;
                            CompletableFuture<Void> future = createPostFromSnapshot(querySnapshot)
                                    .thenAccept(post -> postArrayList.add(post));
                            futures.add(future);
//                            FindPost post = createPostFromSnapshot(querySnapshot);
//                            postArrayList.add(post);
                        }
                    }

                    if (empty) {
                        displayNoPostsFoundMessage();
                    } else {
                        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                        allFutures.thenRun(() -> {
                            binding.shimmerLayout.stopShimmer();
                            binding.shimmerLayout.setVisibility(View.GONE);
                            binding.recyclerViewMain.setVisibility(View.VISIBLE);
                        });
//                        binding.shimmerLayout.stopShimmer();
//                        binding.shimmerLayout.setVisibility(View.GONE);
//                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
                    }
                    postAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {

                });
            }
            else {
                query.get().addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        displayNoPostsFoundMessage();
                        return;
                    }

                    lastVisiblePost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                    List<CompletableFuture<Void>> futures = new ArrayList<>();

                    boolean empty = true;

                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                        empty = false;
                        CompletableFuture<Void> future = createPostFromSnapshot(querySnapshot)
                                .thenAccept(post -> postArrayList.add(post));
                        futures.add(future);
//                        FindPost post = createPostFromSnapshot(querySnapshot);
//                        postArrayList.add(post);
                    }

                    if (empty) {
                        displayNoPostsFoundMessage();
                    } else {
                        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                        allFutures.thenRun(() -> {
                            binding.shimmerLayout.stopShimmer();
                            binding.shimmerLayout.setVisibility(View.GONE);
                            binding.recyclerViewMain.setVisibility(View.VISIBLE);
                        });
//                        binding.shimmerLayout.stopShimmer();
//                        binding.shimmerLayout.setVisibility(View.GONE);
//                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
                    }

                    postAdapter.notifyDataSetChanged();

                })
                .addOnFailureListener(e -> {

                });
            }
        }else {
            System.out.println("null");
        }

    }

    private boolean isPostWithinRadius(QueryDocumentSnapshot querySnapshot,double latFind,double lngFind,double radiusFind) {
        Double lat = querySnapshot.getDouble("lat");
        Double lng = querySnapshot.getDouble("lng");
        Long radiusLong = querySnapshot.getLong("radius");

        if (lat == null || lng == null || radiusLong == null) {
            return false;
        }

        double radius = radiusLong;
        double distance = CalculationByDistance(latFind, lngFind, lat, lng);
        double radiusSum = radiusFind / 1000 + radius / 1000;

        return distance <= radiusSum;
    }

    private void getData() {
        firestore.collection("posts")
            .document("post" + userLocationCity)
            .collection("post" + userLocationCity)
            .whereEqualTo("district", userLocationDistrict)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize)
            .get()
            .addOnSuccessListener(this::handleSuccess)
            .addOnFailureListener(this::handleFailure);
    }

    private void handleSuccess(QuerySnapshot queryDocumentSnapshots) {
        if(queryDocumentSnapshots.isEmpty()){
            displayNoPostsFoundMessage();
            return;
        }

        lastVisiblePost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
            CompletableFuture<Void> future = createPostFromSnapshot(querySnapshot)
                    .thenAccept(post -> postArrayList.add(post));
            futures.add(future);
//            FindPost post = createPostFromSnapshot(querySnapshot);
//            postArrayList.add(post);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.thenRun(() -> {
            binding.shimmerLayout.stopShimmer();
            binding.shimmerLayout.setVisibility(View.GONE);
            binding.recyclerViewMain.setVisibility(View.VISIBLE);
            postAdapter.notifyDataSetChanged();
        });
    }

    private void handleFailure(Exception e) {
        if (e instanceof FirebaseFirestoreException &&
                Objects.requireNonNull(e.getMessage()).contains("The query requires an index")) {
            displayNoPostsFoundMessage();
        }
    }

    private void displayNoPostsFoundMessage() {
        FindPost post = new FindPost();
        post.viewType = 2;
        postArrayList.add(post);
        binding.shimmerLayout.stopShimmer();
        binding.shimmerLayout.setVisibility(View.GONE);
        binding.recyclerViewMain.setVisibility(View.VISIBLE);
        postAdapter.notifyDataSetChanged();
    }

    private void loadMorePost() {
        if (lastVisiblePost == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        Query query = createQuery();
        query.get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                binding.progressBar.setVisibility(View.GONE);

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                List<FindPost> newPosts = new ArrayList<>();
                for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                    CompletableFuture<Void> future = createPostFromSnapshot(querySnapshot)
                            .thenAccept(post -> newPosts.add(post));
                    futures.add(future);
//                    FindPost post = createPostFromSnapshot(querySnapshot);
//                    newPosts.add(post);
                }

                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allFutures.thenRun(() -> {
                    postArrayList.addAll(newPosts);
                    postAdapter.notifyItemRangeInserted(postArrayList.size() - newPosts.size(), newPosts.size());
                });

//                postArrayList.addAll(newPosts);
//                postAdapter.notifyItemRangeInserted(postArrayList.size() - newPosts.size(), newPosts.size());

                if(!queryDocumentSnapshots.isEmpty()){
                    lastVisiblePost = queryDocumentSnapshots.getDocuments()
                            .get(queryDocumentSnapshots.size() - 1);
                }
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
            });
    }

    private Query createQuery() {
        if (!loadCity.isEmpty() && !loadDistrict.isEmpty()) {
            return firestore.collection("posts")
                .document("post" + loadCity)
                .collection("post" + loadCity)
                .whereEqualTo("district", loadDistrict)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisiblePost)
                .limit(pageSize);
        } else {
            return firestore.collection("posts")
                .document("post" + userLocationCity)
                .collection("post" + userLocationCity)
                .whereEqualTo("district", userLocationDistrict)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisiblePost)
                .limit(pageSize);
        }
    }

    private CompletableFuture<FindPost> createPostFromSnapshot(QueryDocumentSnapshot querySnapshot) {
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

    public void setActivityNotification(String mail, DocumentReference ref,Context context){

        boolean checkField = checkRadius != 0 && checkLat != 0 && checkLng != 0;

        if(!myUserName.isEmpty() && checkField){
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
                    firestore.collection("users").document(mail).get().addOnSuccessListener(documentSnapshot1 -> {
                        if(documentSnapshot1.exists()){
                            String getLanguage = language.getString("language","");
                            String token = documentSnapshot1.getString("fcmToken");
                            FCMNotificationSender fcmNotificationSender;
                            if(getLanguage.equals("english")){
                                fcmNotificationSender = new FCMNotificationSender(token, "", getString(R.string.paylasiminiz) + getString(R.string.tarafindan_goruntulendi) + myUserName, context,"");
                            }else {
                                fcmNotificationSender = new FCMNotificationSender(token, "", getString(R.string.paylasiminiz) + myUserName + getString(R.string.tarafindan_goruntulendi), context,"");
                            }
                            fcmNotificationSender.SendNotification();
                        }
                    });
                });
            }else {
                // Veri bulundu ve süresi dolmamış
            }
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

    private void showSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        snackbar.setBackgroundTint(Color.rgb(48, 44, 44));

        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);

        snackbar.show();
    }
}