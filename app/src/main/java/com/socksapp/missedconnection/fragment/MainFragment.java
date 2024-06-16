package com.socksapp.missedconnection.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
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
import android.text.InputType;
import android.util.Log;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.socksapp.missedconnection.FCM.FCMNotificationSender;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.PostAdapter;
import com.socksapp.missedconnection.databinding.FragmentMainBinding;
import com.socksapp.missedconnection.model.ChatMessage;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.myclass.TimedDataManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
    private static final int LOCATION_REQUEST_CODE = 100;
    public double checkRadius = 0,checkLat = 0,checkLng = 0;
    private DocumentSnapshot lastVisiblePost;
    private final int pageSize = 10;
    private String loadCity,loadDistrict;
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
            String date1 = args.getString("date1","");
            String date2 = args.getString("date2","");
            String time1 = args.getString("time1","");
            String time2 = args.getString("time2","");

            getData(city,district,date1,date2,time1,time2,radius,latitude,longitude);
        }else {
            checkLocationPermission();
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

    private void checkLocationPermission(){
        if(ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireActivity(),Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getUserLocation();
        }else {
            requestForPermission();
        }
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
                        Toast.makeText(requireActivity(),userLocationCity+"/"+userLocationDistrict,Toast.LENGTH_SHORT).show();
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

    private void requestForPermission(){
        ActivityCompat.requestPermissions(requireActivity(),new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == LOCATION_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getUserLocation();
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
        }
    }


    public void dialogShow(View view, String mail, String name, Double lat, Double lng, double radius, DocumentReference documentReference){
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
            args.putDouble("fragment_radius", radius);
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
            HashMap<String,Object> data = new HashMap<>();
            data.put("mail",mail);
            data.put("refId",documentReference.getId());
            data.put("timestamp",new Date());
            firestore.collection("saves").document(userMail).collection(userMail).document(documentReference.getId()).set(data).addOnSuccessListener(documentReference12 -> {
//              mainActivity.refDataAccess.insertRef(documentReference.getId(),mail);
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
            if(!mail.equals(userMail)){
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
            }else {
                showSnackbar(view,getString(R.string.kendinizi_bildiremezsiniz));
                dialog.dismiss();
            }

        });

        if(dialog.getWindow() != null){
            dialog.show();
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }

    private void getData(String cityFind,String districtFind,String date1Find,String date2Find,String time1Find,String time2Find,double radiusFind,double latFind,double lngFind){

        boolean checkDistrict,checkDate,checkTime,checkField;
        checkDistrict = !districtFind.isEmpty();
        checkDate = !date1Find.isEmpty() && !date2Find.isEmpty();
        checkTime = !time1Find.isEmpty() && !time2Find.isEmpty();
        checkField = radiusFind != 0 && latFind != 0 && lngFind != 0;

        Query query = null;

        if (checkDistrict && checkDate && checkTime && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(date_1 != null && date_2 != null && time_1 != null && time_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();
                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection("posts").document(collection).collection(collection)
                            .whereEqualTo("district", districtFind)
                            .whereLessThanOrEqualTo("date1",date2_long)
                            .whereGreaterThanOrEqualTo("date2",date1_long)
                            .whereLessThanOrEqualTo("time1",time2_long)
                            .whereGreaterThanOrEqualTo("time2",time1_long)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(pageSize);
                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkDate && checkTime) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(date_1 != null && date_2 != null && time_1 != null && time_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();
                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection("posts").document(collection).collection(collection)
                            .whereEqualTo("district", districtFind)
                            .whereLessThanOrEqualTo("date1",date2_long)
                            .whereGreaterThanOrEqualTo("date2",date1_long)
                            .whereLessThanOrEqualTo("time1",time2_long)
                            .whereGreaterThanOrEqualTo("time2",time1_long)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(pageSize);

                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkDate && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                if(date_1 != null && date_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();

                    query = firestore.collection("posts").document(collection).collection(collection)
                            .whereEqualTo("district", districtFind)
                            .whereLessThanOrEqualTo("date1",date2_long)
                            .whereGreaterThanOrEqualTo("date2",date1_long)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(pageSize);

                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkTime && checkField) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(time_1 != null && time_2 != null){

                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection("posts").document(collection).collection(collection)
                            .whereEqualTo("district", districtFind)
                            .whereLessThanOrEqualTo("time1",time2_long)
                            .whereGreaterThanOrEqualTo("time2",time1_long)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(pageSize);

                }else {
                }
            }catch (Exception e){
                System.out.println("exception : "+e.getLocalizedMessage());
                e.printStackTrace();
            }

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
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                Date date_1 = formatter_date.parse(date1Find);
                Date date_2 = formatter_date.parse(date2Find);

                if(date_1 != null && date_2 != null){
                    long date1_long = date_1.getTime();
                    long date2_long = date_2.getTime();

                    query = firestore.collection("posts").document(collection).collection(collection)
                            .whereEqualTo("district", districtFind)
                            .whereLessThanOrEqualTo("date1",date2_long)
                            .whereGreaterThanOrEqualTo("date2",date1_long)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(pageSize);

                }else {
                }
            }catch (Exception e){
                System.out.println("error: "+e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        else if (checkDistrict && checkTime) {
            postArrayList.clear();
            String collection = "post" + cityFind;
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                Date time_1 = formatter_time.parse(time1Find);
                Date time_2 = formatter_time.parse(time2Find);

                if(time_1 != null && time_2 != null){
                    long time1_long = time_1.getTime();
                    long time2_long = time_2.getTime();

                    query = firestore.collection("posts").document(collection).collection(collection)
                            .whereEqualTo("district", districtFind)
                            .whereLessThanOrEqualTo("time1",time2_long)
                            .whereGreaterThanOrEqualTo("time2",time1_long)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(pageSize);

                }else {
                }
            }catch (Exception e){
                e.printStackTrace();
            }
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
                    boolean empty = true;
                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                        empty = false;
                        if (isPostWithinRadius(querySnapshot,latFind,lngFind,radiusFind)) {
                            FindPost post = createPostFromSnapshot(querySnapshot);
                            postArrayList.add(post);
                        }
                    }

                    if (empty) {
                        displayNoPostsFoundMessage();
                    } else {
                        binding.shimmerLayout.stopShimmer();
                        binding.shimmerLayout.setVisibility(View.GONE);
                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
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

                    boolean empty = true;

                    for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                        empty = false;
                        FindPost post = createPostFromSnapshot(querySnapshot);
                        postArrayList.add(post);
                    }

                    if (empty) {
                        displayNoPostsFoundMessage();
                    } else {
                        binding.shimmerLayout.stopShimmer();
                        binding.shimmerLayout.setVisibility(View.GONE);
                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
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

        for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
            FindPost post = createPostFromSnapshot(querySnapshot);
            postArrayList.add(post);
        }

        binding.shimmerLayout.stopShimmer();
        binding.shimmerLayout.setVisibility(View.GONE);
        binding.recyclerViewMain.setVisibility(View.VISIBLE);
        postAdapter.notifyDataSetChanged();
    }

    private void handleFailure(Exception e) {
        if (e instanceof FirebaseFirestoreException &&
                Objects.requireNonNull(e.getMessage()).contains("The query requires an index")) {
            displayNoPostsFoundMessage();
        }
    }

    private void displayNoPostsFoundMessage() {
        handler.postDelayed(() -> {
            FindPost post = new FindPost();
            post.viewType = 2;
            postArrayList.add(post);
            binding.shimmerLayout.stopShimmer();
            binding.shimmerLayout.setVisibility(View.GONE);
            binding.recyclerViewMain.setVisibility(View.VISIBLE);
            postAdapter.notifyDataSetChanged();
        }, 1000);
    }


//    private void getData(){
//        firestore.collection("posts").document("post"+userLocationCity).collection("post"+userLocationCity)
//                .whereEqualTo("district",userLocationDistrict)
//                .orderBy("timestamp", Query.Direction.DESCENDING)
//                .limit(pageSize).get().addOnSuccessListener(queryDocumentSnapshots -> {
//            if(!queryDocumentSnapshots.isEmpty()){
//                lastVisiblePost = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
//            }
//            boolean found = false;
//            for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
//                found = true;
//                String imageUrl = querySnapshot.getString("imageUrl");
//                String galleryUrl = querySnapshot.getString("galleryUrl");
//                String name = querySnapshot.getString("name");
//                String mail = querySnapshot.getString("mail");
//                String city = querySnapshot.getString("city");
//                String district = querySnapshot.getString("district");
//                Long time1 = querySnapshot.getLong("time1");
//                Long time2 = querySnapshot.getLong("time2");
//                Long date1 = querySnapshot.getLong("date1");
//                Long date2 = querySnapshot.getLong("date2");
//                String explain = querySnapshot.getString("explain");
//                Double lat = querySnapshot.getDouble("lat");
//                Double lng = querySnapshot.getDouble("lng");
//                Long x = querySnapshot.getLong("radius");
//                double radius = 0;
//                if(x != null){
//                    radius = x;
//                }
//                Timestamp timestamp = querySnapshot.getTimestamp("timestamp");
//                DocumentReference documentReference = querySnapshot.getReference();
//
//                FindPost post = new FindPost();
//                post.viewType = 1;
//                post.imageUrl = imageUrl;
//                post.galleryUrl = galleryUrl;
//                post.name = name;
//                post.mail = mail;
//                post.city = city;
//                post.district = district;
//                if (time1 == null) {
//                    post.time1 = 0;
//                } else {
//                    post.time1 = time1;
//                }
//                if (time2 == null) {
//                    post.time2 = 0;
//                } else {
//                    post.time2 = time2;
//                }
//                if (date1 == null) {
//                    post.date1 = 0;
//                } else {
//                    post.date1 = date1;
//                }
//                if (date2 == null) {
//                    post.date2 = 0;
//                } else {
//                    post.date2 = date2;
//                }
//                post.explain = explain;
//                post.timestamp = timestamp;
//                post.lat = lat;
//                post.lng = lng;
//                post.radius = radius;
//                post.documentReference = documentReference;
//
//                postArrayList.add(post);
//                binding.shimmerLayout.stopShimmer();
//                binding.shimmerLayout.setVisibility(View.GONE);
//                binding.recyclerViewMain.setVisibility(View.VISIBLE);
//            }
//            postAdapter.notifyDataSetChanged();
//            if(!found){
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        FindPost post = new FindPost();
//                        post.viewType = 2;
//
//                        postArrayList.add(post);
//                        postAdapter.notifyDataSetChanged();
//
//                        binding.shimmerLayout.stopShimmer();
//                        binding.shimmerLayout.setVisibility(View.GONE);
//                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
//                    }
//                }, 1000);
//            }
//        }).addOnFailureListener(e -> {
//            if (e instanceof FirebaseFirestoreException &&
//                    Objects.requireNonNull(e.getMessage()).contains("The query requires an index")){
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        FindPost post = new FindPost();
//                        post.viewType = 2;
//
//                        postArrayList.add(post);
//                        postAdapter.notifyDataSetChanged();
//
//                        binding.shimmerLayout.stopShimmer();
//                        binding.shimmerLayout.setVisibility(View.GONE);
//                        binding.recyclerViewMain.setVisibility(View.VISIBLE);
//                    }
//                }, 1000);
//            }
//        });
//    }

    private void loadMorePost() {
        if (lastVisiblePost == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        Query query = createQuery();
        query.get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                binding.progressBar.setVisibility(View.GONE);

                List<FindPost> newPosts = new ArrayList<>();
                for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots) {
                    FindPost post = createPostFromSnapshot(querySnapshot);
                    newPosts.add(post);
                }

                postArrayList.addAll(newPosts);
                postAdapter.notifyItemRangeInserted(postArrayList.size() - newPosts.size(), newPosts.size());

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

    private FindPost createPostFromSnapshot(QueryDocumentSnapshot querySnapshot) {
        String imageUrl = querySnapshot.getString("imageUrl");
        String galleryUrl = querySnapshot.getString("galleryUrl");
        String name = querySnapshot.getString("name");
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
        post.imageUrl = imageUrl;
        post.galleryUrl = galleryUrl;
        post.name = name;
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

        return post;
    }


//    private void loadMorePost(){
//        if (lastVisiblePost != null) {
//            binding.progressBar.setVisibility(View.VISIBLE);
//            Query query;
//            if(!loadCity.isEmpty() && !loadDistrict.isEmpty()){
//                query = firestore.collection("posts").document("post"+loadCity).collection("post"+loadCity)
//                        .whereEqualTo("district",loadDistrict)
//                        .orderBy("timestamp", Query.Direction.DESCENDING)
//                        .startAfter(lastVisiblePost)
//                        .limit(pageSize);
//            }else {
//                query = firestore.collection("posts").document("post"+userLocationCity).collection("post"+userLocationCity)
//                        .whereEqualTo("district",userLocationDistrict)
//                        .orderBy("timestamp", Query.Direction.DESCENDING)
//                        .startAfter(lastVisiblePost)
//                        .limit(pageSize);
//            }
//
//            query.get().addOnSuccessListener(queryDocumentSnapshots -> {
//                binding.progressBar.setVisibility(View.GONE);
//                List<FindPost> newPosts = new ArrayList<>();
//                for (QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots){
//                    String imageUrl = querySnapshot.getString("imageUrl");
//                    String galleryUrl = querySnapshot.getString("galleryUrl");
//                    String name = querySnapshot.getString("name");
//                    String mail = querySnapshot.getString("mail");
//                    String city = querySnapshot.getString("city");
//                    String district = querySnapshot.getString("district");
//                    Long time1 = querySnapshot.getLong("time1");
//                    Long time2 = querySnapshot.getLong("time2");
//                    Long date1 = querySnapshot.getLong("date1");
//                    Long date2 = querySnapshot.getLong("date2");
//                    String explain = querySnapshot.getString("explain");
//                    Double lat = querySnapshot.getDouble("lat");
//                    Double lng = querySnapshot.getDouble("lng");
//                    Long x = querySnapshot.getLong("radius");
//                    double radius = 0;
//                    if(x != null){
//                        radius = x;
//                    }
//                    Timestamp timestamp = querySnapshot.getTimestamp("timestamp");
//                    DocumentReference documentReference = querySnapshot.getReference();
//
//                    FindPost post = new FindPost();
//                    post.viewType = 1;
//                    post.imageUrl = imageUrl;
//                    post.galleryUrl = galleryUrl;
//                    post.name = name;
//                    post.mail = mail;
//                    post.city = city;
//                    post.district = district;
//                    if (time1 == null) {
//                        post.time1 = 0;
//                    } else {
//                        post.time1 = time1;
//                    }
//                    if (time2 == null) {
//                        post.time2 = 0;
//                    } else {
//                        post.time2 = time2;
//                    }
//                    if (date1 == null) {
//                        post.date1 = 0;
//                    } else {
//                        post.date1 = date1;
//                    }
//                    if (date2 == null) {
//                        post.date2 = 0;
//                    } else {
//                        post.date2 = date2;
//                    }
//                    post.explain = explain;
//                    post.timestamp = timestamp;
//                    post.lat = lat;
//                    post.lng = lng;
//                    post.radius = radius;
//                    post.documentReference = documentReference;
//
//                    newPosts.add(post);
//                }
//
//                postArrayList.addAll(postArrayList.size(),newPosts);
//                postAdapter.notifyItemRangeInserted(postArrayList.size(), postArrayList.size());
//
//                if (!queryDocumentSnapshots.isEmpty()) {
//                    lastVisiblePost = queryDocumentSnapshots.getDocuments()
//                            .get(queryDocumentSnapshots.size() - 1);
//                }
//            });
//        }
//    }

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
                            if(getLanguage.equals("english")){
                                String token = documentSnapshot1.getString("fcmToken");
                                FCMNotificationSender fcmNotificationSender = new FCMNotificationSender(token,"",getString(R.string.paylasiminiz)+getString(R.string.tarafindan_goruntulendi)+myUserName,context);
                                fcmNotificationSender.SendNotification();
                            }else {
                                String token = documentSnapshot1.getString("fcmToken");
                                FCMNotificationSender fcmNotificationSender = new FCMNotificationSender(token,"",getString(R.string.paylasiminiz)+myUserName+getString(R.string.tarafindan_goruntulendi),context);
                                fcmNotificationSender.SendNotification();
                            }
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