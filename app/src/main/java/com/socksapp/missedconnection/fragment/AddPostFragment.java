package com.socksapp.missedconnection.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentAddPostBinding;
import com.socksapp.missedconnection.model.FindPost;

import java.io.InputStream;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddPostFragment extends Fragment {

    private FragmentAddPostBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private String[] cityNames,districtNames;
    private ArrayAdapter<String> cityAdapter,districtAdapter;
    private AutoCompleteTextView cityCompleteTextView,districtCompleteTextView;
    public static Double lat,lng;
    public static Double rad;
    public static String address;
    private SharedPreferences nameShared,imageUrlShared;
    private String myUserName,myImageUrl,userMail;
    private DatePickerDialog datePickerDialog,datePickerDialog2;
    private TimePickerDialog timePickerDialog,timePickerDialog2;
    private int mYear,mMonth,mDay;
    private MainActivity mainActivity;
    public ActivityResultLauncher<Intent> activityResultLauncher;
    public ActivityResultLauncher<String> permissionLauncher;
    private Bitmap selectedBitmap;
    private Uri imageData;
    private String uniqueID;

    public static MapView mapView;

    public AddPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        lat = 0.0;
        lng = 0.0;
        rad = 0.0;
        address = "";

        nameShared = requireActivity().getSharedPreferences("Name",Context.MODE_PRIVATE);
        imageUrlShared = requireActivity().getSharedPreferences("ImageUrl",Context.MODE_PRIVATE);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddPostBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.mapView.onCreate(savedInstanceState);

        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
        mainActivity.includedLayout.setVisibility(View.VISIBLE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        cityNames = getResources().getStringArray(R.array.city_names);
        cityAdapter = new ArrayAdapter<>(requireContext(), R.layout.list_item,cityNames);
        cityCompleteTextView = binding.getRoot().findViewById(R.id.city_complete_text);
        cityCompleteTextView.setAdapter(cityAdapter);

        userMail = user.getEmail();

        imageData = null;

        registerLauncher(view);

        binding.galleryImage.setOnClickListener(v -> setImage(view));

        myUserName = nameShared.getString("name","");
        myImageUrl = imageUrlShared.getString("imageUrl","");

        binding.cityCompleteText.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedCity = parent.getItemAtPosition(position).toString();
            binding.districtCompleteText.setText("");
            binding.districtCompleteText.setAdapter(null);
            selectDistrict(selectedCity);
        });

        binding.topDatePicker.setOnTouchListener((v, event) -> {
            int checkVisible = binding.visibleDatePicker.getVisibility();
            if(checkVisible == View.GONE){
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }else {
                binding.visibleDatePicker.setVisibility(View.GONE);
            }
            return false;
        });

        binding.topImageLinear.setOnTouchListener((v, event) -> {
            int checkVisible = binding.galleryImage.getVisibility();
            if(checkVisible == View.GONE){
                binding.galleryImage.setVisibility(View.VISIBLE);
            }else {
                binding.galleryImage.setVisibility(View.GONE);
            }

            return false;
        });

        binding.topMapLinear.setOnTouchListener((v, event) -> {
            int checkVisible = binding.mapView.getVisibility();
            if(checkVisible == View.GONE){
                binding.mapView.setVisibility(View.VISIBLE);
            }else {
                binding.mapView.setVisibility(View.GONE);
            }

            return false;
        });

        binding.dateEditText1.setOnTouchListener((v, event) -> {
            showCustomDateDialog1(v);
            return false;
        });

        binding.timeEditText1.setOnTouchListener((v, event) -> {
            showCustomTimeDialog1(v);
            return false;
        });

        binding.dateEditText2.setOnTouchListener((v, event) -> {
            showCustomDateDialog2(v);
            return false;
        });

        binding.timeEditText2.setOnTouchListener((v, event) -> {
            showCustomTimeDialog2(v);
            return false;
        });

        binding.mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {

                disableMapInteractions(googleMap);

                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.dark_map));

                LatLng location = new LatLng(41.008240, 28.978359);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));
//                googleMap.addMarker(new MarkerOptions().position(location).title("İstanbul"));

                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng latLng) {
                        String city = binding.cityCompleteText.getText().toString();
                        String district = binding.districtCompleteText.getText().toString();
                        if(!city.isEmpty() && !district.isEmpty()){
                            Bundle args = new Bundle();
                            args.putString("fragment_type", "add_post");
                            args.putString("fragment_city", city);
                            args.putString("fragment_district", district);
                            GoogleMapsFragment myFragment = new GoogleMapsFragment();
                            myFragment.setArguments(args);
                            FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
                            fragmentTransaction.replace(R.id.fragmentContainerView2,myFragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                        }else {
                            Toast.makeText(requireContext(),"İl ve ilçeyi girmeniz gerekmektedir",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        binding.addPost.setOnClickListener(v ->{
            if(!myUserName.isEmpty()){
                addData(view);
            }else {
                showToastShort("Profilinizi tamamlayınız");
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToMainFragment();
            }
        });

        binding.explain.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                binding.explainTextInput.setError(null);

                int maxLength = 500;
                int currentLength = s.length();
                ColorStateList red,gray,white;
                red = AppCompatResources.getColorStateList(view.getContext(),R.color.red);
                gray = AppCompatResources.getColorStateList(view.getContext(),R.color.gray_with_alpha);
                white = AppCompatResources.getColorStateList(view.getContext(),R.color.white);

                if (currentLength == maxLength) {
                    binding.explainTextInput.setCounterTextColor(red);
                } else {
                    if(currentLength > 0){
                        binding.explainTextInput.setCounterTextColor(white);
                    }else {
                        binding.explainTextInput.setCounterTextColor(gray);
                    }
                }
            }
        });

        binding.cityCompleteText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.cityTextInput.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.districtCompleteText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                binding.districtTextInput.setError(null);

                String city = binding.cityCompleteText.getText().toString();
                String district = s.toString();

                Geocoder geocoder = new Geocoder(requireContext());
                try {
                    List<Address> addressList = geocoder.getFromLocationName(city + ", " + district, 1);
                    if (addressList != null && addressList.size() > 0) {
                        double latitude = addressList.get(0).getLatitude();
                        double longitude = addressList.get(0).getLongitude();

                        LatLng location = new LatLng(latitude, longitude);
                        binding.mapView.getMapAsync(new OnMapReadyCallback() {
                            @Override
                            public void onMapReady(@NonNull GoogleMap googleMap) {
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));
                            }
                        });
                    } else {
                        System.out.println("No location found for the given address.");
                    }
                } catch (Exception e) {
                    System.out.println("exception: "+e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.markedMapView.setText(address);

        if(lat != 0.0 && lng != 0.0 && rad != 0.0){
            setMarked();
        }

    }

    private void setMarked(){
        binding.mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {

                disableMapInteractions(googleMap);

                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.dark_map));

//                LatLng location = new LatLng(lat, lng);
//                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));
//                googleMap.addMarker(new MarkerOptions().position(location).title(address));

                LatLng location = new LatLng(lat, lng);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_location_mark_100);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false); // 50x50 boyutunda ikon
                googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(address)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap))
                );
            }
        });
    }

    private void disableMapInteractions(GoogleMap googleMap) {
        if (googleMap != null) {
            UiSettings uiSettings = googleMap.getUiSettings();
            uiSettings.setZoomControlsEnabled(true);
            uiSettings.setCompassEnabled(false);
            uiSettings.setScrollGesturesEnabled(true);
            uiSettings.setZoomGesturesEnabled(false);
            uiSettings.setTiltGesturesEnabled(false);
            uiSettings.setRotateGesturesEnabled(false);
            uiSettings.setMyLocationButtonEnabled(false);
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    private void goToMainFragment(){

        mainActivity.bottomNavigationView.setSelectedItemId(R.id.navHome);

        MainFragment myFragment = new MainFragment();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,myFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void addData(View view){
        Double latitude = lat;
        Double longitude = lng;
        Double radius = rad;
        String city = binding.cityCompleteText.getText().toString();
        String district = binding.districtCompleteText.getText().toString();
        String date1 = binding.dateEditText1.getText().toString();
        String time1 = binding.timeEditText1.getText().toString();
        String date2 = binding.dateEditText2.getText().toString();
        String time2 = binding.timeEditText2.getText().toString();
        String explain = binding.explain.getText().toString();

        boolean checkCity = !city.isEmpty();
        boolean checkDistrict = !district.isEmpty();
        boolean checkExplain = !explain.isEmpty();

        boolean hasDate1 = !date1.isEmpty();
        boolean hasTime1 = !time1.isEmpty();
        boolean hasDate2 = !date2.isEmpty();
        boolean hasTime2 = !time2.isEmpty();

        boolean checkFormatDate1,checkFormatDate2,checkFormatTime1,checkFormatTime2;

        if(checkCity && checkDistrict && checkExplain){

            if(hasDate1 && hasDate2 && hasTime1 && hasTime2){
                checkFormatDate1 = isValidDateFormat(binding.dateEditText1.getText().toString());
                checkFormatDate2 = isValidDateFormat(binding.dateEditText2.getText().toString());

                checkFormatTime1 = isValidTimeFormat(binding.timeEditText1.getText().toString());
                checkFormatTime2 = isValidTimeFormat(binding.timeEditText2.getText().toString());

                if(checkFormatDate1 && checkFormatDate2 && checkFormatTime1 && checkFormatTime2){
                    boolean checkComparesDate,checkComparesTime;

                    checkComparesDate = compareDates(binding.dateEditText1.getText().toString(),binding.dateEditText2.getText().toString());
                    checkComparesTime = compareTimes(binding.timeEditText1.getText().toString(),binding.timeEditText2.getText().toString());

                    if(checkComparesDate && checkComparesTime){
                        try {
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                            Date date_1 = formatter_date.parse(date1);
                            Date date_2 = formatter_date.parse(date2);

                            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                            Date time_1 = formatter_time.parse(time1);
                            Date time_2 = formatter_time.parse(time2);

                            if(date_1 != null && date_2 != null && time_1 != null && time_2 != null){

                                long date1_long = date_1.getTime();
                                long date2_long = date_2.getTime();
                                long time1_long = time_1.getTime();
                                long time2_long = time_2.getTime();

                                if(imageData != null){
                                    ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                                    progressDialog.setMessage("Gönderiniz paylaşılıyor..");
                                    progressDialog.setCancelable(false);
                                    progressDialog.setInverseBackgroundForced(false);
                                    progressDialog.show();

                                    uniqueID = UUID.randomUUID().toString();
                                    storageReference.child("postsPhoto").child(userMail).child(uniqueID).putFile(imageData)
                                            .addOnSuccessListener(taskSnapshot -> {
                                                Task<Uri> downloadUrlTask = taskSnapshot.getStorage().getDownloadUrl();
                                                downloadUrlTask.addOnCompleteListener(task -> {
                                                    String galleryUrl = task.getResult().toString();

                                                    HashMap<String,Object> post = new HashMap<>();
                                                    post.put("city",city);
                                                    post.put("district",district);
                                                    post.put("date1",date1_long);
                                                    post.put("time1",time1_long);
                                                    post.put("time2",time2_long);
                                                    post.put("date2",date2_long);
                                                    post.put("lat",latitude);
                                                    post.put("lng",longitude);
                                                    post.put("radius",radius);
                                                    post.put("explain",explain);
                                                    post.put("timestamp",new Date());
                                                    post.put("name",myUserName);
                                                    post.put("imageUrl",myImageUrl);
                                                    post.put("galleryUrl",galleryUrl);
                                                    post.put("mail",userMail);

                                                    WriteBatch batch = firestore.batch();

                                                    DocumentReference newPostRef = firestore.collection("post" + city).document();
                                                    batch.set(newPostRef, post);

                                                    DocumentReference newPostRef2 = firestore.collection(userMail).document(newPostRef.getId());
                                                    batch.set(newPostRef2, post);

                                                    batch.commit()
                                                        .addOnSuccessListener(aVoid -> {
                                                            progressDialog.dismiss();
                                                            resetAction();
                                                            showToastShort("Eklendi");
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            progressDialog.dismiss();
                                                            showToastShort(e.getLocalizedMessage());
                                                        });
                                                }).addOnFailureListener(e -> {
                                                    progressDialog.dismiss();
                                                    showToastShort(e.getLocalizedMessage());
                                                });
                                            })
                                            .addOnFailureListener(e -> {
                                                progressDialog.dismiss();
                                                showToastShort(e.getLocalizedMessage());
                                            });
                                }else {
                                    ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                                    progressDialog.setMessage("Gönderiniz paylaşılıyor..");
                                    progressDialog.setCancelable(false);
                                    progressDialog.setInverseBackgroundForced(false);
                                    progressDialog.show();

                                    HashMap<String,Object> post = new HashMap<>();
                                    post.put("city",city);
                                    post.put("district",district);
                                    post.put("date1",date1_long);
                                    post.put("time1",time1_long);
                                    post.put("time2",time2_long);
                                    post.put("date2",date2_long);
                                    post.put("lat",latitude);
                                    post.put("lng",longitude);
                                    post.put("radius",radius);
                                    post.put("explain",explain);
                                    post.put("timestamp",new Date());
                                    post.put("name",myUserName);
                                    post.put("imageUrl",myImageUrl);
                                    post.put("galleryUrl","");
                                    post.put("mail",userMail);

                                    WriteBatch batch = firestore.batch();

                                    DocumentReference newPostRef = firestore.collection("post" + city).document();
                                    batch.set(newPostRef, post);

                                    DocumentReference newPostRef2 = firestore.collection(userMail).document(newPostRef.getId());
                                    batch.set(newPostRef2, post);

                                    batch.commit().addOnSuccessListener(aVoid -> {
                                        progressDialog.dismiss();
                                        resetAction();
                                        showToastShort("Eklendi");
                                    }).addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        showToastShort(e.getLocalizedMessage());
                                    });
                                }

                            }else {

                            }

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    else {
                        if(!checkComparesDate){
                            binding.errorDate1Text.setText("2. girdiğiniz tarihten büyük olamaz");
                            binding.dateEditText1.setTextColor(Color.RED);
                            binding.visibleDatePicker.setVisibility(View.VISIBLE);
                        }else {
                            binding.dateEditText1.setError(null);
                            binding.errorDate1Text.setText("");
                            binding.dateEditText1.setTextColor(Color.WHITE);
                        }
                        if(!checkComparesTime){
                            binding.errorTime2Text.setText("");
                            binding.errorTime1Text.setText("2. girdiğiniz saatten büyük olamaz");
                            binding.timeEditText1.setTextColor(Color.RED);
                            binding.visibleDatePicker.setVisibility(View.VISIBLE);
                        }else {
                            binding.timeEditText1.setError(null);
                            binding.errorTime1Text.setText("");
                            binding.timeEditText1.setTextColor(Color.WHITE);
                        }
                    }

                }
                else {
                    if(!checkFormatDate1){
                        binding.errorDate1Text.setText("Gün/Ay/Yıl formatına uygun giriniz");
                        binding.dateEditText1.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }else {
                        binding.dateEditText1.setError(null);
                    }
                    if(!checkFormatDate2){
                        binding.errorDate2Text.setText("Gün/Ay/Yıl formatına uygun giriniz");
                        binding.dateEditText2.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }else {
                        binding.dateEditText2.setError(null);
                    }
                    if(!checkFormatTime1){
                        binding.errorTime1Text.setText("Saat:Dakika formatına uygun giriniz");
                        binding.timeEditText1.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }else {
                        binding.timeEditText1.setError(null);
                    }
                    if(!checkFormatTime2){
                        binding.errorTime2Text.setText("Saat:Dakika formatına uygun giriniz");
                        binding.timeEditText2.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }else {
                        binding.timeEditText2.setError(null);
                    }
                }
            }


            // error

            if(hasDate1 && hasDate2 && hasTime1 && !hasTime2){
                binding.errorDate1Text.setText("");
                binding.errorDate2Text.setText("");
                binding.errorTime1Text.setText("");
                binding.dateEditText1.setTextColor(Color.WHITE);
                binding.dateEditText2.setTextColor(Color.WHITE);
                binding.timeEditText1.setTextColor(Color.WHITE);

                binding.errorTime2Text.setText("Saat aralığını eksiksiz giriniz");
                binding.timeEditText2.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(hasDate1 && hasDate2 && !hasTime1 && hasTime2){
                binding.errorDate1Text.setText("");
                binding.errorDate2Text.setText("");
                binding.errorTime2Text.setText("");
                binding.dateEditText1.setTextColor(Color.WHITE);
                binding.dateEditText2.setTextColor(Color.WHITE);
                binding.timeEditText2.setTextColor(Color.WHITE);


                binding.errorTime1Text.setText("Saat aralığını eksiksiz giriniz");
                binding.timeEditText1.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(hasDate1 && !hasDate2 && hasTime1 && hasTime2){
                binding.errorDate1Text.setText("");
                binding.errorTime1Text.setText("");
                binding.errorTime2Text.setText("");
                binding.dateEditText1.setTextColor(Color.WHITE);
                binding.timeEditText1.setTextColor(Color.WHITE);
                binding.timeEditText2.setTextColor(Color.WHITE);

                binding.errorDate2Text.setText("Tarih aralığını eksiksiz giriniz");
                binding.dateEditText2.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(!hasDate1 && hasDate2 && hasTime1 && hasTime2){
                binding.errorDate2Text.setText("");
                binding.errorTime1Text.setText("");
                binding.errorTime2Text.setText("");
                binding.dateEditText2.setTextColor(Color.WHITE);
                binding.timeEditText1.setTextColor(Color.WHITE);
                binding.timeEditText2.setTextColor(Color.WHITE);

                binding.errorDate1Text.setText("Tarih aralığını eksiksiz giriniz");
                binding.dateEditText1.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(hasDate1 && !hasDate2 && !hasTime1 && !hasTime2){
                binding.errorDate1Text.setText("");
                binding.errorTime1Text.setText("");
                binding.errorTime2Text.setText("");
                binding.timeEditText1.setHintTextColor(Color.GRAY);
                binding.timeEditText2.setHintTextColor(Color.GRAY);
                binding.dateEditText1.setTextColor(Color.WHITE);

                binding.errorDate2Text.setText("Tarih aralığını eksiksiz giriniz");
                binding.dateEditText2.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(!hasDate1 && hasDate2 && !hasTime1 && !hasTime2){
                binding.errorDate2Text.setText("");
                binding.errorTime1Text.setText("");
                binding.errorTime2Text.setText("");
                binding.timeEditText1.setHintTextColor(Color.GRAY);
                binding.timeEditText2.setHintTextColor(Color.GRAY);
                binding.dateEditText2.setTextColor(Color.WHITE);

                binding.errorDate1Text.setText("Tarih aralığını eksiksiz giriniz");
                binding.dateEditText1.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(!hasDate1 && !hasDate2 && hasTime1 && !hasTime2){
                binding.errorDate1Text.setText("");
                binding.errorDate2Text.setText("");
                binding.errorTime1Text.setText("");
                binding.dateEditText1.setHintTextColor(Color.GRAY);
                binding.dateEditText2.setHintTextColor(Color.GRAY);
                binding.timeEditText1.setTextColor(Color.WHITE);

                binding.errorTime2Text.setText("Saat aralığını eksiksiz giriniz");
                binding.timeEditText2.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(!hasDate1 && !hasDate2 && !hasTime1 && hasTime2){
                binding.errorDate1Text.setText("");
                binding.errorDate2Text.setText("");
                binding.errorTime2Text.setText("");
                binding.dateEditText1.setHintTextColor(Color.GRAY);
                binding.dateEditText2.setHintTextColor(Color.GRAY);
                binding.timeEditText2.setTextColor(Color.WHITE);

                binding.errorTime1Text.setText("Saat aralığını eksiksiz giriniz");
                binding.timeEditText1.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(hasDate1 && !hasDate2 && !hasTime1 && hasTime2){
                binding.errorDate1Text.setText("");
                binding.errorTime2Text.setText("");
                binding.dateEditText1.setTextColor(Color.WHITE);
                binding.timeEditText2.setTextColor(Color.WHITE);

                binding.errorDate2Text.setText("Tarih aralığını eksiksiz giriniz");
                binding.dateEditText2.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);

                binding.errorTime1Text.setText("Saat aralığını eksiksiz giriniz");
                binding.timeEditText1.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(hasDate1 && !hasDate2 && hasTime1 && !hasTime2){
                binding.errorDate1Text.setText("");
                binding.errorTime1Text.setText("");
                binding.dateEditText1.setTextColor(Color.WHITE);
                binding.timeEditText1.setTextColor(Color.WHITE);

                binding.errorDate2Text.setText("Tarih aralığını eksiksiz giriniz");
                binding.dateEditText2.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);

                binding.errorTime2Text.setText("Saat aralığını eksiksiz giriniz");
                binding.timeEditText2.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(!hasDate1 && hasDate2 && !hasTime1 && hasTime2){
                binding.errorDate2Text.setText("");
                binding.errorTime2Text.setText("");
                binding.dateEditText2.setTextColor(Color.WHITE);
                binding.timeEditText2.setTextColor(Color.WHITE);

                binding.errorDate1Text.setText("Tarih aralığını eksiksiz giriniz");
                binding.dateEditText1.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);

                binding.errorTime1Text.setText("Saat aralığını eksiksiz giriniz");
                binding.timeEditText1.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            if(!hasDate1 && hasDate2 && hasTime1 && !hasTime2){
                binding.errorDate2Text.setText("");
                binding.errorTime1Text.setText("");
                binding.dateEditText2.setTextColor(Color.WHITE);
                binding.timeEditText1.setTextColor(Color.WHITE);

                binding.errorDate1Text.setText("Tarih aralığını eksiksiz giriniz");
                binding.dateEditText1.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);

                binding.errorTime2Text.setText("Saat aralığını eksiksiz giriniz");
                binding.timeEditText2.setHintTextColor(Color.RED);
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }

            //error


            if(hasDate1 && hasDate2 && !hasTime1 && !hasTime2){
                checkFormatDate1 = isValidDateFormat(binding.dateEditText1.getText().toString());
                checkFormatDate2 = isValidDateFormat(binding.dateEditText2.getText().toString());

                if(checkFormatDate1 && checkFormatDate2){
                    boolean checkComparesDate;

                    checkComparesDate = compareDates(binding.dateEditText1.getText().toString(),binding.dateEditText2.getText().toString());

                    if(checkComparesDate){
                        binding.dateEditText1.setError(null);
                        binding.errorDate1Text.setText("");
                        binding.dateEditText1.setTextColor(Color.WHITE);
                        try {
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_date = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr"));
                            Date date_1 = formatter_date.parse(date1);
                            Date date_2 = formatter_date.parse(date2);

                            if(date_1 != null && date_2 != null){

                                long date1_long = date_1.getTime();
                                long date2_long = date_2.getTime();


                                if(imageData != null){
                                    ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                                    progressDialog.setMessage("Gönderiniz paylaşılıyor..");
                                    progressDialog.setCancelable(false);
                                    progressDialog.setInverseBackgroundForced(false);
                                    progressDialog.show();

                                    uniqueID = UUID.randomUUID().toString();
                                    storageReference.child("postsPhoto").child(userMail).child(uniqueID).putFile(imageData)
                                        .addOnSuccessListener(taskSnapshot -> {
                                            Task<Uri> downloadUrlTask = taskSnapshot.getStorage().getDownloadUrl();
                                            downloadUrlTask.addOnCompleteListener(task -> {
                                                String galleryUrl = task.getResult().toString();

                                                HashMap<String,Object> post = new HashMap<>();
                                                post.put("city",city);
                                                post.put("district",district);
                                                post.put("date1",date1_long);
                                                post.put("time1",0);
                                                post.put("time2",0);
                                                post.put("date2",date2_long);
                                                post.put("lat",latitude);
                                                post.put("lng",longitude);
                                                post.put("radius",radius);
                                                post.put("explain",explain);
                                                post.put("timestamp",new Date());
                                                post.put("name",myUserName);
                                                post.put("imageUrl",myImageUrl);
                                                post.put("galleryUrl",galleryUrl);
                                                post.put("mail",userMail);

                                                WriteBatch batch = firestore.batch();

                                                DocumentReference newPostRef = firestore.collection("post" + city).document();
                                                batch.set(newPostRef, post);

                                                DocumentReference newPostRef2 = firestore.collection(userMail).document(newPostRef.getId());
                                                batch.set(newPostRef2, post);

                                                batch.commit()
                                                    .addOnSuccessListener(aVoid -> {
                                                        progressDialog.dismiss();
                                                        resetAction();
                                                        showToastShort("Eklendi");
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        progressDialog.dismiss();
                                                        showToastShort(e.getLocalizedMessage());
                                                    });
                                            }).addOnFailureListener(e -> {
                                                progressDialog.dismiss();
                                                showToastShort(e.getLocalizedMessage());
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            progressDialog.dismiss();
                                            showToastShort(e.getLocalizedMessage());
                                        });
                                }else {
                                    ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                                    progressDialog.setMessage("Gönderiniz paylaşılıyor..");
                                    progressDialog.setCancelable(false);
                                    progressDialog.setInverseBackgroundForced(false);
                                    progressDialog.show();

                                    HashMap<String,Object> post = new HashMap<>();
                                    post.put("city",city);
                                    post.put("district",district);
                                    post.put("date1",date1_long);
                                    post.put("time1",0);
                                    post.put("time2",0);
                                    post.put("date2",date2_long);
                                    post.put("lat",latitude);
                                    post.put("lng",longitude);
                                    post.put("radius",radius);
                                    post.put("explain",explain);
                                    post.put("timestamp",new Date());
                                    post.put("name",myUserName);
                                    post.put("imageUrl",myImageUrl);
                                    post.put("galleryUrl","");
                                    post.put("mail",userMail);

                                    WriteBatch batch = firestore.batch();

                                    DocumentReference newPostRef = firestore.collection("post" + city).document();
                                    batch.set(newPostRef, post);

                                    DocumentReference newPostRef2 = firestore.collection(userMail).document(newPostRef.getId());
                                    batch.set(newPostRef2, post);

                                    batch.commit().addOnSuccessListener(aVoid -> {
                                        progressDialog.dismiss();
                                        resetAction();
                                        showToastShort("Eklendi");
                                    }).addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        showToastShort(e.getLocalizedMessage());
                                    });
                                }


                            }else {

                            }

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    else {
                        binding.errorDate1Text.setText("2. girdiğiniz tarihten büyük olamaz");
                        binding.dateEditText1.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }

                }
                else {
                    if(!checkFormatDate1){
                        binding.errorDate1Text.setText("Gün/Ay/Yıl formatına uygun giriniz");
                        binding.dateEditText1.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }else {
                        binding.dateEditText1.setError(null);
                    }
                    if(!checkFormatDate2){
                        binding.errorDate2Text.setText("Gün/Ay/Yıl formatına uygun giriniz");
                        binding.dateEditText2.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }else {
                        binding.dateEditText2.setError(null);
                    }
                }
            }

            if(!hasDate1 && !hasDate2 && hasTime1 && hasTime2){
                checkFormatTime1 = isValidTimeFormat(binding.timeEditText1.getText().toString());
                checkFormatTime2 = isValidTimeFormat(binding.timeEditText2.getText().toString());

                if(checkFormatTime1 && checkFormatTime2){
                    boolean checkComparesTime;

                    checkComparesTime = compareTimes(binding.timeEditText1.getText().toString(),binding.timeEditText2.getText().toString());

                    if(checkComparesTime){
                        binding.timeEditText1.setError(null);
                        binding.errorTime1Text.setText("");
                        binding.timeEditText1.setTextColor(Color.WHITE);
                        try {

                            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr"));
                            Date time_1 = formatter_time.parse(time1);
                            Date time_2 = formatter_time.parse(time2);

                            if(time_1 != null && time_2 != null){

                                long time1_long = time_1.getTime();
                                long time2_long = time_2.getTime();

                                if(imageData != null){
                                    ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                                    progressDialog.setMessage("Gönderiniz paylaşılıyor..");
                                    progressDialog.setCancelable(false);
                                    progressDialog.setInverseBackgroundForced(false);
                                    progressDialog.show();

                                    uniqueID = UUID.randomUUID().toString();
                                    storageReference.child("postsPhoto").child(userMail).child(uniqueID).putFile(imageData)
                                        .addOnSuccessListener(taskSnapshot -> {
                                            Task<Uri> downloadUrlTask = taskSnapshot.getStorage().getDownloadUrl();
                                            downloadUrlTask.addOnCompleteListener(task -> {
                                                String galleryUrl = task.getResult().toString();

                                                HashMap<String,Object> post = new HashMap<>();
                                                post.put("city",city);
                                                post.put("district",district);
                                                post.put("date1",0);
                                                post.put("time1",time1_long);
                                                post.put("time2",time2_long);
                                                post.put("date2",0);
                                                post.put("lat",latitude);
                                                post.put("lng",longitude);
                                                post.put("radius",radius);
                                                post.put("explain",explain);
                                                post.put("timestamp",new Date());
                                                post.put("name",myUserName);
                                                post.put("imageUrl",myImageUrl);
                                                post.put("galleryUrl",galleryUrl);
                                                post.put("mail",userMail);

                                                WriteBatch batch = firestore.batch();

                                                DocumentReference newPostRef = firestore.collection("post" + city).document();
                                                batch.set(newPostRef, post);

                                                DocumentReference newPostRef2 = firestore.collection(userMail).document(newPostRef.getId());
                                                batch.set(newPostRef2, post);

                                                batch.commit()
                                                    .addOnSuccessListener(aVoid -> {
                                                        progressDialog.dismiss();
                                                        resetAction();
                                                        showToastShort("Eklendi");
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        progressDialog.dismiss();
                                                        showToastShort(e.getLocalizedMessage());
                                                    });
                                            }).addOnFailureListener(e -> {
                                                progressDialog.dismiss();
                                                showToastShort(e.getLocalizedMessage());
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            progressDialog.dismiss();
                                            showToastShort(e.getLocalizedMessage());
                                        });
                                }else {
                                    ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                                    progressDialog.setMessage("Gönderiniz paylaşılıyor..");
                                    progressDialog.setCancelable(false);
                                    progressDialog.setInverseBackgroundForced(false);
                                    progressDialog.show();

                                    HashMap<String,Object> post = new HashMap<>();
                                    post.put("city",city);
                                    post.put("district",district);
                                    post.put("date1",0);
                                    post.put("time1",time1_long);
                                    post.put("time2",time2_long);
                                    post.put("date2",0);
                                    post.put("lat",latitude);
                                    post.put("lng",longitude);
                                    post.put("radius",radius);
                                    post.put("explain",explain);
                                    post.put("timestamp",new Date());
                                    post.put("name",myUserName);
                                    post.put("imageUrl",myImageUrl);
                                    post.put("galleryUrl","");
                                    post.put("mail",userMail);

                                    WriteBatch batch = firestore.batch();

                                    DocumentReference newPostRef = firestore.collection("post" + city).document();
                                    batch.set(newPostRef, post);

                                    DocumentReference newPostRef2 = firestore.collection(userMail).document(newPostRef.getId());
                                    batch.set(newPostRef2, post);

                                    batch.commit().addOnSuccessListener(aVoid -> {
                                        progressDialog.dismiss();
                                        resetAction();
                                        showToastShort("Eklendi");
                                    }).addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        showToastShort(e.getLocalizedMessage());
                                    });
                                }

                            }else {

                            }

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    else {
                        binding.errorTime1Text.setText("2. girdiğiniz saatten büyük olamaz");
                        binding.timeEditText1.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }

                }
                else {
                    if(!checkFormatTime1){
                        binding.errorTime1Text.setText("Saat:Dakika formatına uygun giriniz");
                        binding.timeEditText1.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }else {
                        binding.timeEditText1.setError(null);
                        binding.errorTime1Text.setText("");
                        binding.timeEditText1.setTextColor(Color.WHITE);
                    }
                    if(!checkFormatTime2){
                        binding.errorTime2Text.setText("Saat:Dakika formatına uygun giriniz");
                        binding.timeEditText2.setTextColor(Color.RED);
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    }else {
                        binding.timeEditText2.setError(null);
                        binding.timeEditText2.setTextColor(Color.WHITE);
                    }
                }
            }

            if(!hasDate1 && !hasDate2 && !hasTime1 && !hasTime2){

                if(imageData != null){
                    ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                    progressDialog.setMessage("Gönderiniz paylaşılıyor..");
                    progressDialog.setCancelable(false);
                    progressDialog.setInverseBackgroundForced(false);
                    progressDialog.show();

                    uniqueID = UUID.randomUUID().toString();
                    storageReference.child("postsPhoto").child(userMail).child(uniqueID).putFile(imageData)
                        .addOnSuccessListener(taskSnapshot -> {
                            Task<Uri> downloadUrlTask = taskSnapshot.getStorage().getDownloadUrl();
                            downloadUrlTask.addOnCompleteListener(task -> {
                                String galleryUrl = task.getResult().toString();

                                HashMap<String,Object> post = new HashMap<>();
                                post.put("city",city);
                                post.put("district",district);
                                post.put("date1",0);
                                post.put("time1",0);
                                post.put("time2",0);
                                post.put("date2",0);
                                post.put("lat",latitude);
                                post.put("lng",longitude);
                                post.put("radius",radius);
                                post.put("explain",explain);
                                post.put("timestamp",new Date());
                                post.put("name",myUserName);
                                post.put("imageUrl",myImageUrl);
                                post.put("galleryUrl",galleryUrl);
                                post.put("mail",userMail);

                                WriteBatch batch = firestore.batch();

                                DocumentReference newPostRef = firestore.collection("post" + city).document();
                                batch.set(newPostRef, post);

                                DocumentReference newPostRef2 = firestore.collection(userMail).document(newPostRef.getId());
                                batch.set(newPostRef2, post);

                                batch.commit()
                                    .addOnSuccessListener(aVoid -> {
                                        progressDialog.dismiss();
                                        resetAction();
                                        showToastShort("Eklendi");
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        showToastShort(e.getLocalizedMessage());
                                    });
                            }).addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                showToastShort(e.getLocalizedMessage());
                            });
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            showToastShort(e.getLocalizedMessage());
                        });
                }else {

                    ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                    progressDialog.setMessage("Gönderiniz paylaşılıyor..");
                    progressDialog.setCancelable(false);
                    progressDialog.setInverseBackgroundForced(false);
                    progressDialog.show();

                    HashMap<String,Object> post = new HashMap<>();
                    post.put("city",city);
                    post.put("district",district);
                    post.put("date1",0);
                    post.put("time1",0);
                    post.put("time2",0);
                    post.put("date2",0);
                    post.put("lat",latitude);
                    post.put("lng",longitude);
                    post.put("radius",radius);
                    post.put("explain",explain);
                    post.put("timestamp",new Date());
                    post.put("name",myUserName);
                    post.put("imageUrl",myImageUrl);
                    post.put("galleryUrl","");
                    post.put("mail",userMail);

                    WriteBatch batch = firestore.batch();

                    DocumentReference newPostRef = firestore.collection("post" + city).document();
                    batch.set(newPostRef, post);

                    DocumentReference newPostRef2 = firestore.collection(userMail).document(newPostRef.getId());
                    batch.set(newPostRef2, post);

                    batch.commit().addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        resetAction();
                        showToastShort("Eklendi");
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        showToastShort(e.getLocalizedMessage());
                    });
                }

            }

        }
        else {
            if(!checkCity){
                binding.cityTextInput.setError("İl boş bırakılamaz");
                binding.cityTextInput.setErrorIconDrawable(null);
            }else {
                binding.cityTextInput.setError(null);
                binding.cityTextInput.setErrorIconDrawable(null);
            }
            if(!checkDistrict){
                binding.districtTextInput.setError("İlçe boş bırakılamaz");
                binding.districtTextInput.setErrorIconDrawable(null);
            }else {
                binding.districtTextInput.setError(null);
                binding.districtTextInput.setErrorIconDrawable(null);
            }
            if(!checkExplain){
                binding.explainTextInput.setError("Açıklama boş bırakılamaz");
                binding.explainTextInput.setErrorIconDrawable(null);
            }else {
                binding.explainTextInput.setError(null);
                binding.explainTextInput.setErrorIconDrawable(null);
            }
        }

    }

    private int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    private void resetAction(){
        binding.mapView.setVisibility(View.GONE);
        binding.visibleDatePicker.setVisibility(View.GONE);

        binding.cityCompleteText.setText("");

        binding.districtCompleteText.setText("");

        imageData = null;
        binding.galleryImage.setImageResource(R.drawable.add_gallery);

        binding.explain.setText("");

        binding.dateEditText1.setText("");
        binding.dateEditText2.setText("");

        binding.timeEditText1.setText("");
        binding.timeEditText2.setText("");

        binding.markedMapView.setText("");

        binding.errorTime1Text.setText("");
        binding.errorTime2Text.setText("");
        binding.errorDate1Text.setText("");
        binding.errorDate2Text.setText("");

        binding.timeEditText1.setHintTextColor(Color.GRAY);
        binding.timeEditText2.setHintTextColor(Color.GRAY);
        binding.dateEditText1.setHintTextColor(Color.GRAY);
        binding.dateEditText2.setHintTextColor(Color.GRAY);

        lat = 0.0;
        lng = 0.0;
        rad = 0.0;
        address = "";

//        binding.checkBoxContact.setChecked(false);

    }
    private void showCustomTimeDialog1(View view) {
        if(timePickerDialog == null){
            final Calendar currentTime = Calendar.getInstance();
            int hour = currentTime.get(Calendar.HOUR_OF_DAY);
            int minute = currentTime.get(Calendar.MINUTE);

            timePickerDialog = new TimePickerDialog(
                view.getContext(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
                        String timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                        binding.timeEditText1.setText(timeString);
                        binding.timeEditText1.setTextColor(Color.WHITE);
                        binding.errorTime1Text.setText("");
                    }
                },
                hour,
                minute,
                true
            );
        }


        timePickerDialog.show();
    }
    private void showCustomDateDialog1(View view) {
        if(datePickerDialog == null){
            final Calendar calendar = Calendar.getInstance();
            mYear = calendar.get(Calendar.YEAR);
            mMonth = calendar.get(Calendar.MONTH);
            mDay = calendar.get(Calendar.DAY_OF_MONTH);
            datePickerDialog = new DatePickerDialog(view.getContext(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    String timeString = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
                    binding.dateEditText1.setText(timeString);
                    binding.dateEditText1.setTextColor(Color.WHITE);
                    binding.errorDate1Text.setText("");
                }
            },mYear,mMonth,mDay);
            datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        }

        datePickerDialog.show();
    }
    private void showCustomTimeDialog2(View view) {
        if(timePickerDialog2 == null){
            final Calendar currentTime = Calendar.getInstance();
            int hour = currentTime.get(Calendar.HOUR_OF_DAY);
            int minute = currentTime.get(Calendar.MINUTE);

            timePickerDialog2 = new TimePickerDialog(
                view.getContext(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
                        String timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                        binding.timeEditText2.setText(timeString);
                        binding.timeEditText2.setTextColor(Color.WHITE);
                        binding.errorTime2Text.setText("");
                    }
                },
                hour,
                minute,
                true
            );
        }

        timePickerDialog2.show();
    }
    private void showCustomDateDialog2(View view) {
        if(datePickerDialog2 == null){
            final Calendar calendar = Calendar.getInstance();
            mYear = calendar.get(Calendar.YEAR);
            mMonth = calendar.get(Calendar.MONTH);
            mDay = calendar.get(Calendar.DAY_OF_MONTH);
            datePickerDialog2 = new DatePickerDialog(view.getContext(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    String timeString = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
                    binding.dateEditText2.setText(timeString);
                    binding.dateEditText2.setTextColor(Color.WHITE);
                    binding.errorDate2Text.setText("");
                }
            },mYear,mMonth,mDay);
            datePickerDialog2.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        }
        datePickerDialog2.show();
    }

    private boolean isValidDateFormat(String input) {
        Pattern pattern = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
        Matcher matcher = pattern.matcher(input);

        return matcher.matches();
    }
    private boolean isValidTimeFormat(String timeString) {
        String regexPattern = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$";

        return timeString.matches(regexPattern);
    }
    private boolean compareDates(String dateText1, String dateText2) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date date1 = null;
        Date date2 = null;

        try {
            date1 = sdf.parse(dateText1);
            date2 = sdf.parse(dateText2);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (date1 != null && date2 != null) {
            return date1.compareTo(date2) <= 0;
        }

        return false;
    }
    private boolean compareTimes(String time1, String time2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date1 = sdf.parse(time1);
            Date date2 = sdf.parse(time2);

            if (date1 != null && date2 != null) {
                return date1.compareTo(date2) <= 0;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return false;
    }


    private void selectDistrict(String selectedCity){
        switch (selectedCity){
            case "İstanbul":
                districtNames = getResources().getStringArray(R.array.district_istanbul);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Ankara":
                districtNames = getResources().getStringArray(R.array.district_ankara);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "İzmir":
                districtNames = getResources().getStringArray(R.array.district_izmir);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Adana":
                districtNames = getResources().getStringArray(R.array.district_adana);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Adıyaman":
                districtNames = getResources().getStringArray(R.array.district_adiyaman);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Afyonkarahisar":
                districtNames = getResources().getStringArray(R.array.district_afyonkarahisar);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Ağrı":
                districtNames = getResources().getStringArray(R.array.district_agri);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Amasya":
                districtNames = getResources().getStringArray(R.array.district_amasya);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Antalya":
                districtNames = getResources().getStringArray(R.array.district_antalya);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Artvin":
                districtNames = getResources().getStringArray(R.array.district_artvin);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Aydın":
                districtNames = getResources().getStringArray(R.array.district_aydin);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Balıkesir":
                districtNames = getResources().getStringArray(R.array.district_balikesir);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Bilecik":
                districtNames = getResources().getStringArray(R.array.district_bilecik);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Bingöl":
                districtNames = getResources().getStringArray(R.array.district_bingol);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Bitlis":
                districtNames = getResources().getStringArray(R.array.district_bitlis);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Bolu":
                districtNames = getResources().getStringArray(R.array.district_bolu);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Burdur":
                districtNames = getResources().getStringArray(R.array.district_burdur);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Bursa":
                districtNames = getResources().getStringArray(R.array.district_bursa);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Çanakkale":
                districtNames = getResources().getStringArray(R.array.district_canakkale);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Çankırı":
                districtNames = getResources().getStringArray(R.array.district_cankiri);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Çorum":
                districtNames = getResources().getStringArray(R.array.district_corum);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Denizli":
                districtNames = getResources().getStringArray(R.array.district_denizli);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Diyarbakır":
                districtNames = getResources().getStringArray(R.array.district_diyarbakir);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Edirne":
                districtNames = getResources().getStringArray(R.array.district_edirne);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Elazığ":
                districtNames = getResources().getStringArray(R.array.district_elazig);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Erzincan":
                districtNames = getResources().getStringArray(R.array.district_erzincan);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Erzurum":
                districtNames = getResources().getStringArray(R.array.district_erzurum);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Eskişehir":
                districtNames = getResources().getStringArray(R.array.district_eskisehir);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Gaziantep":
                districtNames = getResources().getStringArray(R.array.district_gaziantep);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Giresun":
                districtNames = getResources().getStringArray(R.array.district_giresun);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Gümüşhane":
                districtNames = getResources().getStringArray(R.array.district_gumushane);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Hakkari":
                districtNames = getResources().getStringArray(R.array.district_hakkari);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Hatay":
                districtNames = getResources().getStringArray(R.array.district_hatay);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Isparta":
                districtNames = getResources().getStringArray(R.array.district_isparta);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Mersin":
                districtNames = getResources().getStringArray(R.array.district_mersin);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kars":
                districtNames = getResources().getStringArray(R.array.district_kars);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kastamonu":
                districtNames = getResources().getStringArray(R.array.district_kastamonu);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kayseri":
                districtNames = getResources().getStringArray(R.array.district_kayseri);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kırklareli":
                districtNames = getResources().getStringArray(R.array.district_kirklareli);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kırşehir":
                districtNames = getResources().getStringArray(R.array.district_kirsehir);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kocaeli":
                districtNames = getResources().getStringArray(R.array.district_kocaeli);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Konya":
                districtNames = getResources().getStringArray(R.array.district_konya);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kütahya":
                districtNames = getResources().getStringArray(R.array.district_kutahya);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Malatya":
                districtNames = getResources().getStringArray(R.array.district_malatya);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Manisa":
                districtNames = getResources().getStringArray(R.array.district_manisa);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kahramanmaraş":
                districtNames = getResources().getStringArray(R.array.district_kahramanmaras);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Mardin":
                districtNames = getResources().getStringArray(R.array.district_mardin);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Muğla":
                districtNames = getResources().getStringArray(R.array.district_mugla);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Muş":
                districtNames = getResources().getStringArray(R.array.district_mus);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Nevşehir":
                districtNames = getResources().getStringArray(R.array.district_nevsehir);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Niğde":
                districtNames = getResources().getStringArray(R.array.district_nigde);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Ordu":
                districtNames = getResources().getStringArray(R.array.district_ordu);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Rize":
                districtNames = getResources().getStringArray(R.array.district_rize);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Sakarya":
                districtNames = getResources().getStringArray(R.array.district_sakarya);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Samsun":
                districtNames = getResources().getStringArray(R.array.district_samsun);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Siirt":
                districtNames = getResources().getStringArray(R.array.district_siirt);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Sinop":
                districtNames = getResources().getStringArray(R.array.district_sinop);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Sivas":
                districtNames = getResources().getStringArray(R.array.district_sivas);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Tekirdağ":
                districtNames = getResources().getStringArray(R.array.district_tekirdag);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Tokat":
                districtNames = getResources().getStringArray(R.array.district_tokat);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Trabzon":
                districtNames = getResources().getStringArray(R.array.district_trabzon);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Tunceli":
                districtNames = getResources().getStringArray(R.array.district_tunceli);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Şanlıurfa":
                districtNames = getResources().getStringArray(R.array.district_sanliurfa);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Uşak":
                districtNames = getResources().getStringArray(R.array.district_usak);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Van":
                districtNames = getResources().getStringArray(R.array.district_van);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Yozgat":
                districtNames = getResources().getStringArray(R.array.district_yozgat);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Zonguldak":
                districtNames = getResources().getStringArray(R.array.district_zonguldak);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Aksaray":
                districtNames = getResources().getStringArray(R.array.district_aksaray);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Bayburt":
                districtNames = getResources().getStringArray(R.array.district_bayburt);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Karaman":
                districtNames = getResources().getStringArray(R.array.district_karaman);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kırıkkale":
                districtNames = getResources().getStringArray(R.array.district_kirikkale);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Batman":
                districtNames = getResources().getStringArray(R.array.district_batman);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Şırnak":
                districtNames = getResources().getStringArray(R.array.district_sirnak);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Bartın":
                districtNames = getResources().getStringArray(R.array.district_bartin);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Ardahan":
                districtNames = getResources().getStringArray(R.array.district_ardahan);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Iğdır":
                districtNames = getResources().getStringArray(R.array.district_igdir);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Yalova":
                districtNames = getResources().getStringArray(R.array.district_yalova);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Karabük":
                districtNames = getResources().getStringArray(R.array.district_karabuk);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Kilis":
                districtNames = getResources().getStringArray(R.array.district_kilis);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Osmaniye":
                districtNames = getResources().getStringArray(R.array.district_osmaniye);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            case "Düzce":
                districtNames = getResources().getStringArray(R.array.district_duzce);
                districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                districtCompleteTextView.setAdapter(districtAdapter);
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.mapView.onResume();

        cityNames = getResources().getStringArray(R.array.city_names);
        cityAdapter = new ArrayAdapter<>(requireContext(), R.layout.list_item,cityNames);
        cityCompleteTextView = binding.getRoot().findViewById(R.id.city_complete_text);
        cityCompleteTextView.setAdapter(cityAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.mapView.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.mapView.onSaveInstanceState(outState);
    }

    private void setImage(View view) {
        String[] permissions;
        String rationaleMessage;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        }else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        rationaleMessage = "Galeriye gitmek için izin gerekli";

        if (ContextCompat.checkSelfPermission(view.getContext(), permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permissions[0])) {
                Snackbar.make(view, rationaleMessage, Snackbar.LENGTH_INDEFINITE).setAction("İzin ver", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionLauncher.launch(permissions[0]);
                    }
                }).show();
            } else {
                permissionLauncher.launch(permissions[0]);
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intent);
        }
    }

    private void registerLauncher(View view){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent intentForResult = result.getData();
                    if(intentForResult != null){
                        imageData = intentForResult.getData();
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            try {
//                                ImageDecoder.Source source = ImageDecoder.createSource(view.getContext().getContentResolver(),imageData);
//                                selectedBitmap = ImageDecoder.decodeBitmap(source);
//                                binding.galleryImage.setImageBitmap(selectedBitmap);

                                int screenWidth = getScreenWidth(view.getContext());

                                Glide.with(view.getContext())
                                    .load(imageData)
                                    .apply(new RequestOptions()
                                    .error(R.drawable.icon_loading)
                                    .fitCenter()
                                    .centerCrop())
                                    .override(screenWidth, 500)
                                    .into(binding.galleryImage);

                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }else {
                            try {
//                                InputStream inputStream = view.getContext().getContentResolver().openInputStream(imageData);
//                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                                binding.galleryImage.setImageBitmap(bitmap);
//                                if (inputStream != null) {
//                                    inputStream.close();
//                                }

                                int screenWidth = getScreenWidth(view.getContext());

                                Glide.with(view.getContext())
                                    .load(imageData)
                                    .apply(new RequestOptions()
                                    .error(R.drawable.icon_loading)
                                    .fitCenter()
                                    .centerCrop())
                                    .override(screenWidth, 500)
                                    .into(binding.galleryImage);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
            }
        });
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intent);
                }else{
                    showToastShort("İzinleri aktif etmeniz gerekiyor");
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
    public void showToastShort(String message){
        Toast.makeText(requireActivity().getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }
    public void showToastLong(String message){
        Toast.makeText(requireActivity().getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }
    private void showErrorMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

}