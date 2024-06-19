package com.socksapp.missedconnection.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.PopupWindow;
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
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
    private SharedPreferences nameShared,imageUrlShared,language;
    private String myUserName,myImageUrl,userMail;
    private long date1_long,date2_long,time1_long,time2_long;
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

        language = requireActivity().getSharedPreferences("Language",Context.MODE_PRIVATE);
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

        date1_long = 0;
        date2_long = 0;
        time1_long = 0;
        time2_long = 0;

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
                            Toast.makeText(requireContext(), getString(R.string.l_ve_il_eyi_girmeniz_gerekmektedir),Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        binding.addPost.setOnClickListener(v ->{
            if(!myUserName.isEmpty()){
                addData(view);
            }else {
                showSnackbar(v,getString(R.string.g_nderi_payla_mak_i_in_profilinizi_tamamlamal_s_n_z));
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
                    System.out.println("city: "+ city);
                    System.out.println("district: "+ district);
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

        binding.dateRangeInputLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialogs(v);
            }
        });

        binding.timeRangeInputLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialogs(v);
            }
        });


        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToMainFragment();
            }
        });

    }

    private void showTimePickerDialogs(View view) {
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        View popupView = inflater.inflate(R.layout.layout_time_range_picker, null);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

        AppCompatButton selectButton = popupView.findViewById(R.id.selectButton);
        AppCompatButton clearButton = popupView.findViewById(R.id.clearButton);

        TimePicker startTimePicker = popupView.findViewById(R.id.startTimePicker);
        TimePicker endTimePicker = popupView.findViewById(R.id.endTimePicker);

        TextView startTimeText = popupView.findViewById(R.id.startTime);
        TextView endTimeText = popupView.findViewById(R.id.endTime);

        Calendar calendar = Calendar.getInstance();

        if(binding.timeRange.getText().toString().isEmpty()){
            String getLanguage = language.getString("language","");
            if(getLanguage.equals("turkish")){
                int startHour = startTimePicker.getCurrentHour();
                int startMinute = startTimePicker.getCurrentMinute();
                String startFormattedTime = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute);
                startTimeText.setText(startFormattedTime);

                int endHour = endTimePicker.getCurrentHour();
                int endMinute = endTimePicker.getCurrentMinute();
                String endFormattedTime = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute);
                endTimeText.setText(endFormattedTime);
            }else {
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.forLanguageTag("en"));

                int startHour = startTimePicker.getCurrentHour();
                int startMinute = startTimePicker.getCurrentMinute();
                calendar.set(Calendar.HOUR_OF_DAY, startHour);
                calendar.set(Calendar.MINUTE, startMinute);
                String startFormattedTime = timeFormat.format(calendar.getTime());
                startTimeText.setText(startFormattedTime);

                int endHour = endTimePicker.getCurrentHour();
                int endMinute = endTimePicker.getCurrentMinute();
                calendar.set(Calendar.HOUR_OF_DAY, endHour);
                calendar.set(Calendar.MINUTE, endMinute);
                String endFormattedTime = timeFormat.format(calendar.getTime());
                endTimeText.setText(endFormattedTime);
            }
        }else {
            String timeRangeText = binding.timeRange.getText().toString();
            String[] rangeText = timeRangeText.split("-");
            String firstRangeText = rangeText[0];
            String secondRangeText = rangeText[1];

            startTimeText.setText(firstRangeText);
            endTimeText.setText(secondRangeText);

            String getLanguage = language.getString("language","");
            if(getLanguage.equals("turkish")){
                String[] parts = firstRangeText.split(":");
                int hour = Integer.parseInt(parts[0].trim());
                int minute = Integer.parseInt(parts[1].trim());
                startTimePicker.setHour(hour);
                startTimePicker.setMinute(minute);

                String[] parts2 = secondRangeText.split(":");
                int hour2 = Integer.parseInt(parts2[0].trim());
                int minute2 = Integer.parseInt(parts2[1].trim());
                endTimePicker.setHour(hour2);
                endTimePicker.setMinute(minute2);
            }else {
                String[] parts1 = firstRangeText.split(":");
                int hour1 = Integer.parseInt(parts1[0].trim());
                int minute1 = Integer.parseInt(parts1[1].substring(0, 2).trim());
                String period1 = firstRangeText.substring(firstRangeText.length() - 2).trim();

                if (period1.equalsIgnoreCase("PM") && hour1 < 12) {
                    hour1 += 12;
                } else if (period1.equalsIgnoreCase("AM") && hour1 == 12) {
                    hour1 = 0;
                }

                startTimePicker.setHour(hour1);
                startTimePicker.setMinute(minute1);
                startTimeText.setText(firstRangeText.trim());

                String[] parts2 = secondRangeText.split(":");
                int hour2 = Integer.parseInt(parts2[0].trim());
                int minute2 = Integer.parseInt(parts2[1].substring(0, 2).trim());
                String period2 = secondRangeText.substring(secondRangeText.length() - 2).trim();

                if (period2.equalsIgnoreCase("PM") && hour2 < 12) {
                    hour2 += 12;
                } else if (period2.equalsIgnoreCase("AM") && hour2 == 12) {
                    hour2 = 0;
                }

                endTimePicker.setHour(hour2);
                endTimePicker.setMinute(minute2);
                endTimeText.setText(secondRangeText.trim());
            }
        }

        startTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                String getLanguage = language.getString("language","");
                if(getLanguage.equals("turkish")){
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    startTimeText.setText(formattedTime);
                }else {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.forLanguageTag("en"));

                    int startHour = startTimePicker.getCurrentHour();
                    int startMinute = startTimePicker.getCurrentMinute();
                    calendar.set(Calendar.HOUR_OF_DAY, startHour);
                    calendar.set(Calendar.MINUTE, startMinute);
                    String startFormattedTime = timeFormat.format(calendar.getTime());
                    startTimeText.setText(startFormattedTime);
                }
            }
        });

        endTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                String getLanguage = language.getString("language","");
                if(getLanguage.equals("turkish")){
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    endTimeText.setText(formattedTime);
                }else {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.forLanguageTag("en"));

                    int endHour = endTimePicker.getCurrentHour();
                    int endMinute = endTimePicker.getCurrentMinute();
                    calendar.set(Calendar.HOUR_OF_DAY, endHour);
                    calendar.set(Calendar.MINUTE, endMinute);
                    String endFormattedTime = timeFormat.format(calendar.getTime());
                    endTimeText.setText(endFormattedTime);
                }

            }
        });

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstTime = startTimeText.getText().toString();
                String secondTime= endTimeText.getText().toString();
                String allTime = firstTime + " - " + secondTime;
                boolean checkComparesTime;
                checkComparesTime = compareTimes(firstTime,secondTime);

                if(checkComparesTime){
                    String getLanguage = language.getString("language","");
                    SimpleDateFormat formatter_time;
                    if(getLanguage.equals("turkish")){
                        formatter_time = new SimpleDateFormat("HH:mm", new Locale("tr"));
                    }else {
                        formatter_time = new SimpleDateFormat("hh:mm a", new Locale("en"));
                    }
                    try {
                        Date time_1 = formatter_time.parse(firstTime);
                        Date time_2 = formatter_time.parse(secondTime);
                        if(time_1 != null && time_2 != null){
                            time1_long = time_1.getTime();
                            time2_long = time_2.getTime();
                            binding.timeRange.setText(allTime);
                            popupWindow.dismiss();
                        }else {
//                            time1_long = 0;
//                            time2_long = 0;
                        }
                    } catch (Exception e) {
//                        time1_long = 0;
//                        time2_long = 0;
                        e.printStackTrace();
                    }
                }else {
                    showSnackbar(v,"2. seçtiğiniz saat 1. seçtiğiniz saatten küçük olamaz");
                }
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time1_long = 0;
                time2_long = 0;
                binding.timeRange.setText("");
                popupWindow.dismiss();
            }
        });

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    private void showDatePickerDialogs(View view) {
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        View popupView = inflater.inflate(R.layout.layout_date_range_picker, null);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

        AppCompatButton selectButton = popupView.findViewById(R.id.selectButton);
        AppCompatButton clearButton = popupView.findViewById(R.id.clearButton);

        DatePicker startDatePicker = popupView.findViewById(R.id.startDatePicker);
        DatePicker endDatePicker = popupView.findViewById(R.id.endDatePicker);

        TextView startDateText = popupView.findViewById(R.id.startDate);
        TextView endDateText = popupView.findViewById(R.id.endDate);

        Calendar calendar = Calendar.getInstance();
        startDatePicker.setMaxDate(calendar.getTimeInMillis());
        endDatePicker.setMaxDate(calendar.getTimeInMillis());

        String getLanguage = language.getString("language","");
        SimpleDateFormat dateFormat;
        if(getLanguage.equals("turkish")){
            dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
        }else {
            dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("en"));
        }

        int startYear = startDatePicker.getYear();
        int startMonth = startDatePicker.getMonth();
        int startDay = startDatePicker.getDayOfMonth();
        calendar.set(startYear, startMonth, startDay);
        String startFormattedDate = dateFormat.format(calendar.getTime());
        startDateText.setText(startFormattedDate);

        int endYear = endDatePicker.getYear();
        int endMonth = endDatePicker.getMonth();
        int endDay = endDatePicker.getDayOfMonth();
        calendar.set(endYear, endMonth, endDay);
        String endFormattedDate = dateFormat.format(calendar.getTime());
        endDateText.setText(endFormattedDate);

        startDatePicker.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(year, monthOfYear, dayOfMonth);
                String formattedDate = dateFormat.format(calendar.getTime());
                startDateText.setText(formattedDate);
            }
        });

        endDatePicker.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(year, monthOfYear, dayOfMonth);
                String formattedDate = dateFormat.format(calendar.getTime());
                endDateText.setText(formattedDate);
            }
        });

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstDate = startDateText.getText().toString();
                String secondDate= endDateText.getText().toString();
                String allDate = firstDate + " - " + secondDate;

                String getLanguage = language.getString("language","");
                SimpleDateFormat dateFormat;
                if(getLanguage.equals("turkish")){
                    dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("tr"));
                }else {
                    dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("en"));
                }

                int startYear = startDatePicker.getYear();
                int startMonth = startDatePicker.getMonth();
                int startDay = startDatePicker.getDayOfMonth();
                calendar.set(startYear, startMonth, startDay);
                String startFormattedDate = dateFormat.format(calendar.getTime());

                int endYear = endDatePicker.getYear();
                int endMonth = endDatePicker.getMonth();
                int endDay = endDatePicker.getDayOfMonth();
                calendar.set(endYear, endMonth, endDay);
                String endFormattedDate = dateFormat.format(calendar.getTime());

                boolean checkComparesDate;

                checkComparesDate = compareDates(startFormattedDate,endFormattedDate);

                if(checkComparesDate){
                    SimpleDateFormat formatter_date;
                    if(getLanguage.equals("turkish")){
                        formatter_date = new SimpleDateFormat("dd/MM/yyyy", new Locale("tr"));
                    }else {
                        formatter_date = new SimpleDateFormat("dd/MM/yyyy", new Locale("en"));
                    }
                    try {
                        Date date_1 = formatter_date.parse(startFormattedDate);
                        Date date_2 = formatter_date.parse(endFormattedDate);
                        if(date_1 != null && date_2 != null){
                            date1_long = date_1.getTime();
                            date2_long = date_2.getTime();
                            binding.datetimeRange.setText(allDate);
                            popupWindow.dismiss();
                        }else {
//                            date1_long = 0;
//                            date2_long = 0;
                        }
                    } catch (Exception e) {
//                        date1_long = 0;
//                        date2_long = 0;
                        e.printStackTrace();
                    }
                }else {
                    showSnackbar(v,"2. seçtiğiniz tarih 1. seçtiğiniz tarihten küçük olamaz");
                }
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.datetimeRange.setText("");
                popupWindow.dismiss();
            }
        });

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
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
        String explain = binding.explain.getText().toString();

        boolean checkCity = !city.isEmpty();
        boolean checkDistrict = !district.isEmpty();
        boolean checkExplain = !explain.isEmpty();

        if(checkCity && checkDistrict && checkExplain){
            if(imageData != null){
                ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                progressDialog.setMessage(getString(R.string.g_nderiniz_payla_l_yor));
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

                            DocumentReference newPostRef = firestore.collection("posts").document("post"+city).collection("post"+city).document();
                            batch.set(newPostRef, post);

                            DocumentReference newPostRef2 = firestore.collection("myPosts").document(userMail).collection(userMail).document(newPostRef.getId());
                            batch.set(newPostRef2, post);

                            batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    resetAction();
                                    showSnackbar(view,getString(R.string.g_nderiniz_payla_ld));
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    showSnackbar(view,getString(R.string.error_post));
                                });
                        }).addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            showSnackbar(view,getString(R.string.error_post));
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        showSnackbar(view,getString(R.string.error_post));
                    });
            }
            else {
                ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                progressDialog.setMessage(getString(R.string.g_nderiniz_payla_l_yor));
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

                DocumentReference newPostRef = firestore.collection("posts").document("post"+city).collection("post"+city).document();
                batch.set(newPostRef, post);

                DocumentReference newPostRef2 = firestore.collection("myPosts").document(userMail).collection(userMail).document(newPostRef.getId());
                batch.set(newPostRef2, post);

                batch.commit().addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    resetAction();
                    showSnackbar(view,getString(R.string.g_nderiniz_payla_ld));
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    showSnackbar(view,getString(R.string.error_post));
                });
            }

        }
        else {
            if(!checkCity){
                String text = getString(R.string.il_bos_birakilamaz);
                binding.cityTextInput.setError(text);
                binding.cityTextInput.setErrorIconDrawable(null);
            }else {
                binding.cityTextInput.setError(null);
                binding.cityTextInput.setErrorIconDrawable(null);
            }
            if(!checkDistrict){
                String text = getString(R.string.ilce_bos_birakilamaz);
                binding.districtTextInput.setError(text);
                binding.districtTextInput.setErrorIconDrawable(null);
            }else {
                binding.districtTextInput.setError(null);
                binding.districtTextInput.setErrorIconDrawable(null);
            }
            if(!checkExplain){
                String text = getString(R.string.aciklama_bos_birakilamaz);
                binding.explainTextInput.setError(text);
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

        binding.markedMapView.setText("");

        lat = 0.0;
        lng = 0.0;
        rad = 0.0;
        address = "";
        time1_long = 0;
        time2_long = 0;
        date1_long = 0;
        date2_long = 0;

//        binding.checkBoxContact.setChecked(false);
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
        System.out.println("time1: "+time1);
        System.out.println("time2: "+time2);
        String getLanguage = language.getString("language","");
        if(getLanguage.equals("turkish")){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date date1 = sdf.parse(time1);
                Date date2 = sdf.parse(time2);

                if (date1 != null && date2 != null) {
                    return date1.compareTo(date2) <= 0; // Saat1, saat2'den büyük veya eşitse true döndürün
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Date date1 = sdf.parse(time1);
                Date date2 = sdf.parse(time2);

                if (date1 != null && date2 != null) {
                    return date1.compareTo(date2) <= 0; // Saat1, saat2'den büyük veya eşitse true döndürün
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
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
        if (binding != null && binding.mapView != null) {
            binding.mapView.onResume();
        }

        cityNames = getResources().getStringArray(R.array.city_names);
        cityAdapter = new ArrayAdapter<>(requireContext(), R.layout.list_item,cityNames);
        cityCompleteTextView = binding.getRoot().findViewById(R.id.city_complete_text);
        cityCompleteTextView.setAdapter(cityAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null && binding.mapView != null) {
            binding.mapView.onPause();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (binding != null && binding.mapView != null) {
            binding.mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (binding != null && binding.mapView != null) {
            binding.mapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null && binding.mapView != null) {
            binding.mapView.onSaveInstanceState(outState);
        }

    }

    private void setImage(View view) {
        String[] permissions;
        String rationaleMessage;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        }else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        rationaleMessage = getString(R.string.galeriye_gitmek_i_in_izin_gerekli);

        if (ContextCompat.checkSelfPermission(view.getContext(), permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permissions[0])) {
                Snackbar.make(view, rationaleMessage, Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.izin_ver), new View.OnClickListener() {
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
                    showSnackbar(view,getString(R.string.izinleri_aktif_etmeniz_gerekiyor));
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

    private void showSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        snackbar.setBackgroundTint(Color.rgb(48, 44, 44));

        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);

        snackbar.show();
    }

}