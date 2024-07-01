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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TimePicker;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentAddPostBinding;
import com.socksapp.missedconnection.myclass.Utils;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private Uri imageData;
    private String uniqueID;
    private Menu menu;
    private MenuItem menuItem;
    private String lastText;

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

        binding.mapView.getMapAsync(googleMap -> {
            disableMapInteractions(googleMap);

            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.dark_map));

            LatLng location = new LatLng(41.008240, 28.978359);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));

            googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull LatLng latLng) {
                    String city = binding.cityCompleteText.getText().toString();
                    String district = binding.districtCompleteText.getText().toString();
                    if(!city.isEmpty() && !district.isEmpty()){
                        Bundle args = new Bundle();
                        args.putString("fragment_type", "find_post");
                        args.putString("fragment_city", city);
                        args.putString("fragment_district", district);
                        GoogleMapsFragment myFragment = new GoogleMapsFragment();
                        myFragment.setArguments(args);
                        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.fragmentContainerView2,myFragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }else {
                        showSnackbar(requireView(),getString(R.string.l_ve_il_eyi_girmeniz_gerekmektedir));
                    }
                }
            });
        });

        if(lat != 0.0 && lng != 0.0 && rad != 0.0){
            setMarked(view);
        }

        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
        mainActivity.includedLayout.setVisibility(View.VISIBLE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        getAllCities(view);

        menu = mainActivity.navigationView.getMenu();
        menuItem = menu.findItem(R.id.nav_drawer_home);
        menuItem.setIcon(R.drawable.home_default_96);

        userMail = user.getEmail();

        imageData = null;

        date1_long = 0;
        date2_long = 0;
        time1_long = 0;
        time2_long = 0;

        registerLauncher(view);

        binding.galleryImage.setOnClickListener(v -> setImage(view));
        binding.uploadImage.setOnClickListener(v -> setImage(view));

        myUserName = nameShared.getString("name","");
        myImageUrl = imageUrlShared.getString("imageUrl","");

        binding.cityCompleteText.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedCity = parent.getItemAtPosition(position).toString();
            binding.districtCompleteText.setText("");
            binding.districtCompleteText.setAdapter(null);
            getAllDistricts(view,selectedCity);
            lat = 0.0;
            lng = 0.0;
            rad = 0.0;
            binding.markedMapView.setText("");
        });

        binding.districtCompleteText.setOnItemClickListener((parent, view12, position, id) -> {
            lat = 0.0;
            lng = 0.0;
            rad = 0.0;
            binding.markedMapView.setText("");
        });

        binding.topDatePicker.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int touchX = (int) event.getX();

                int layoutWidth = binding.topDatePicker.getWidth();

                if (touchX <= layoutWidth / 2) {
                    int checkVisible = binding.visibleDatePicker.getVisibility();
                    if (checkVisible == View.GONE) {
                        binding.visibleDatePicker.setVisibility(View.VISIBLE);
                    } else {
                        binding.visibleDatePicker.setVisibility(View.GONE);
                    }
                }
            }

            return true;
        });

        binding.topImageLinear.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int touchX = (int) event.getX();

                int layoutWidth = binding.topImageLinear.getWidth();

                if (touchX <= layoutWidth / 2) {
                    int checkVisible = binding.galleryImage.getVisibility();
                    if (checkVisible == View.GONE) {
                        binding.galleryImage.setVisibility(View.VISIBLE);
                        if(imageData == null){
                            binding.uploadImage.setVisibility(View.VISIBLE);
                        }
                    } else {
                        binding.galleryImage.setVisibility(View.GONE);
                        if(imageData == null){
                            binding.uploadImage.setVisibility(View.GONE);
                        }
                    }
                }
            }

            return true;
        });

        binding.topMapLinear.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int touchX = (int) event.getX();

                int layoutWidth = binding.topMapLinear.getWidth();

                if (touchX <= layoutWidth / 2) {
                    int checkVisible = binding.mapViewRadius.getVisibility();
                    if (checkVisible == View.GONE) {
                        binding.mapViewRadius.setVisibility(View.VISIBLE);
                    } else {
                        binding.mapViewRadius.setVisibility(View.GONE);
                    }
                }
            }

            return true;
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
                if (binding.explain.getLineCount() > 15) {
                    showSnackbar(view,getString(R.string.maksimum_10_sat_r_girebilirsiniz));
                    binding.explain.setText(s.subSequence(0, start + count - 1));
                    binding.explain.setSelection(start + count - 1);
                }
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

                getCoordinatesFromAddress(view,city,district);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.markedMapView.setText(address);

        binding.dateRangeInputLayout.setEndIconOnClickListener(this::showDatePickerDialogs);

        binding.timeRangeInputLayout.setEndIconOnClickListener(this::showTimePickerDialogs);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToMainFragment();
            }
        });

    }

    private void getCoordinatesFromAddress(View view,String city,String district){
        LatLng latLng = handleManualDistricts(city,district);
        if(latLng != null){
            binding.mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull GoogleMap googleMap) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                }
            });
        }else {
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
//                        Toast.makeText(view.getContext(),"No location found for the given address.",Toast.LENGTH_SHORT).show();
                    System.out.println("No location found for the given address.");
                }
            } catch (Exception e) {
                System.out.println("exception: "+e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    private void getAllDistricts(View view, String selectedCity) {
        String json = Utils.loadJSONFromAsset(view.getContext(), "city_district.json");
        if (json == null) {
            // Handle error if JSON is null
            return;
        }

        Type listType = new TypeToken<Utils.CityData>() {}.getType();
        Utils.CityData cityData = new Gson().fromJson(json, listType);

        if(cityData != null){
            List<Utils.City> cities = cityData.getData();
            for (Utils.City city : cities) {
                if (city.getIl_adi().equalsIgnoreCase(selectedCity)) {
                    List<String> districtNamesList = new ArrayList<>();
                    for (Utils.District district : city.getIlceler()) {
                        districtNamesList.add(district.getIlce_adi());
                    }
                    districtNames = districtNamesList.toArray(new String[0]);
                    districtAdapter = new ArrayAdapter<>(requireContext(),R.layout.list_item,districtNames);
                    districtCompleteTextView = binding.getRoot().findViewById(R.id.district_complete_text);
                    districtCompleteTextView.setAdapter(districtAdapter);
                    return;
                }
            }
        }
    }

    private void getAllCities(View view) {
        String json = Utils.loadJSONFromAsset(view.getContext(), "city_district.json");
        if (json == null) {
            // JSON dosyası okunamadı, hatayı loglayın veya kullanıcıya bildirin
            Log.e("MainActivity", "JSON dosyası okunamadı");
            return;
        }

        Type listType = new TypeToken<Utils.CityData>() {}.getType();
        Utils.CityData cityData = new Gson().fromJson(json, listType);

        if (cityData != null) {
            List<Utils.City> cities = cityData.getData();

            cityNames = new String[cities.size()]; // Initialize the array with the correct size

            for (int i = 0; i < cities.size(); i++) {
                cityNames[i] = cities.get(i).getIl_adi();
            }
        }

        cityAdapter = new ArrayAdapter<>(requireContext(), R.layout.list_item,cityNames);
        cityCompleteTextView = binding.getRoot().findViewById(R.id.city_complete_text);
        cityCompleteTextView.setAdapter(cityAdapter);
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
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.forLanguageTag("en"));

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
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.forLanguageTag("en"));

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
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.forLanguageTag("en"));

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
                        formatter_time = new SimpleDateFormat("h:mm a", new Locale("en"));
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
                    showSnackbar(v,getString(R.string._2_se_ti_iniz_saat_1_se_ti_iniz_saatten_k_k_olamaz));
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

        if(binding.datetimeRange.getText().toString().isEmpty()){
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
        }else {
            startDatePicker.setMaxDate(calendar.getTimeInMillis());
            endDatePicker.setMaxDate(calendar.getTimeInMillis());

            String dateRangeText = binding.datetimeRange.getText().toString();
            String[] rangeText = dateRangeText.split("-");
            String firstRangeText = rangeText[0].trim();
            String secondRangeText = rangeText[1].trim();

            startDateText.setText(firstRangeText);
            endDateText.setText(secondRangeText);

            String getLanguage = language.getString("language","");
            if(getLanguage.equals("turkish")){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("tr", "TR"));

                LocalDate localDate = LocalDate.parse(firstRangeText, formatter);

                int year = localDate.getYear();
                int month = localDate.getMonthValue() - 1;
                int dayOfMonth = localDate.getDayOfMonth();

                startDatePicker.updateDate(year, month, dayOfMonth);

                LocalDate localDate2 = LocalDate.parse(secondRangeText, formatter);

                int year2 = localDate2.getYear();
                int month2 = localDate2.getMonthValue() - 1;
                int dayOfMonth2 = localDate2.getDayOfMonth();

                endDatePicker.updateDate(year2, month2, dayOfMonth2);
            }else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("en", "US"));

                LocalDate localDate = LocalDate.parse(firstRangeText, formatter);

                int year = localDate.getYear();
                int month = localDate.getMonthValue() - 1;
                int dayOfMonth = localDate.getDayOfMonth();

                startDatePicker.updateDate(year, month, dayOfMonth);

                LocalDate localDate2 = LocalDate.parse(secondRangeText, formatter);

                int year2 = localDate2.getYear();
                int month2 = localDate2.getMonthValue() - 1;
                int dayOfMonth2 = localDate2.getDayOfMonth();

                endDatePicker.updateDate(year2, month2, dayOfMonth2);
            }

        }

        startDatePicker.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String getLanguage = language.getString("language","");
                SimpleDateFormat dateFormat;
                if(getLanguage.equals("turkish")){
                    dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
                }else {
                    dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("en"));
                }
                calendar.set(year, monthOfYear, dayOfMonth);
                String formattedDate = dateFormat.format(calendar.getTime());
                startDateText.setText(formattedDate);
            }
        });

        endDatePicker.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String getLanguage = language.getString("language","");
                SimpleDateFormat dateFormat;
                if(getLanguage.equals("turkish")){
                    dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
                }else {
                    dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("en"));
                }
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
                    showSnackbar(v,getString(R.string._2_se_ti_iniz_tarih_1_se_ti_iniz_tarihten_k_k_olamaz));
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

    private void setMarked(View view){
        binding.mapView.getMapAsync(googleMap -> {

            disableMapInteractions(googleMap);

            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.dark_map));

            LatLng location = new LatLng(lat, lng);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
            googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(address)
            );

            googleMap.setOnMapClickListener(latLng -> {
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
                    showSnackbar(view,getString(R.string.l_ve_il_eyi_girmeniz_gerekmektedir));
                }
            });
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
//                            post.put("name",myUserName);
//                            post.put("imageUrl",myImageUrl);
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
                                    refreshFragment();
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
//                post.put("name",myUserName);
//                post.put("imageUrl",myImageUrl);
                post.put("galleryUrl","");
                post.put("mail",userMail);

                WriteBatch batch = firestore.batch();

                DocumentReference newPostRef = firestore.collection("posts").document("post"+city).collection("post"+city).document();
                batch.set(newPostRef, post);

                DocumentReference newPostRef2 = firestore.collection("myPosts").document(userMail).collection(userMail).document(newPostRef.getId());
                batch.set(newPostRef2, post);

                batch.commit().addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    refreshFragment();
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

    public void refreshFragment() {
        AddPostFragment fragment = new AddPostFragment();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
//        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private boolean compareDates(String dateText1, String dateText2) {
        String getLanguage = language.getString("language","");
        if(getLanguage.equals("turkish")){
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("tr"));
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
        }else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("en"));
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
        }

        return false;
    }

    private boolean compareTimes(String time1, String time2) {
        String getLanguage = language.getString("language","");
        if(getLanguage.equals("turkish")){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", new Locale("tr"));
                Date date1 = sdf.parse(time1);
                Date date2 = sdf.parse(time2);

                if (date1 != null && date2 != null) {
                    return date1.compareTo(date2) <= 0;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", new Locale("en"));
                Date date1 = sdf.parse(time1);
                Date date2 = sdf.parse(time2);

                if (date1 != null && date2 != null) {
                    return date1.compareTo(date2) <= 0;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (binding != null && binding.mapView != null) {
            binding.mapView.onStart();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (binding != null && binding.mapView != null) {
            binding.mapView.onStop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && binding.mapView != null) {
            binding.mapView.onResume();
        }

        getAllCities(requireView());

        if(!binding.cityCompleteText.getText().toString().isEmpty()){
            String city = binding.cityCompleteText.getText().toString();
            binding.districtCompleteText.setAdapter(null);
            getAllDistricts(requireView(),city);
        }
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

                        binding.uploadImage.setVisibility(View.GONE);
                        binding.galleryImage.setAdjustViewBounds(true);

                        Glide.with(view.getContext())
                            .load(imageData)
                            .into(binding.galleryImage);

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

    private LatLng handleManualDistricts(String city, String district) {

        LatLng latLng;
        if (city.equalsIgnoreCase("Ankara") && district.equalsIgnoreCase("Sincan")) {
            latLng = new LatLng(39.966751, 32.584229);
        }else if (city.equalsIgnoreCase("Ankara") && district.equalsIgnoreCase("Bala")){
            latLng = new LatLng(39.553373, 33.123768);
        }else if (city.equalsIgnoreCase("Adana") && district.equalsIgnoreCase("Sarıçam")){
            latLng = new LatLng(37.149987, 35.4903691);
        }else if (city.equalsIgnoreCase("Adana") && district.equalsIgnoreCase("Seyhan")){
            latLng = new LatLng(36.9231821, 35.0583745);
        }else if (city.equalsIgnoreCase("Adana") && district.equalsIgnoreCase("Yüreğir")){
            latLng = new LatLng(36.8675305, 35.2956341);
        }else if (city.equalsIgnoreCase("Antalya") && district.equalsIgnoreCase("Kepez")){
            latLng = new LatLng(36.95276037969528, 30.72425285208392);
        }else if (city.equalsIgnoreCase("Antalya") && district.equalsIgnoreCase("Konyaaltı")){
            latLng = new LatLng(36.87259431450681, 30.6323821484396);
        }else if (city.equalsIgnoreCase("Aydın") && district.equalsIgnoreCase("Efeler")){
            latLng = new LatLng(37.8553307, 27.7680007);
        }else if (city.equalsIgnoreCase("Balıkesir") && district.equalsIgnoreCase("Karesi")){
            latLng = new LatLng(39.65332454003856, 27.890341925257253);
        }else if (city.equalsIgnoreCase("Balıkesir") && district.equalsIgnoreCase("Altıeylül")){
            latLng = new LatLng(39.65332454003856, 27.890341925257253);
        }else if (city.equalsIgnoreCase("Bursa") && district.equalsIgnoreCase("Nilüfer")){
            latLng = new LatLng(40.19897371132959, 28.961897497051787);
        }else if (city.equalsIgnoreCase("Bursa") && district.equalsIgnoreCase("Osmangazi")){
            latLng = new LatLng(40.1630087, 28.964634);
        }else if (city.equalsIgnoreCase("Denizli") && district.equalsIgnoreCase("Merkezefendi")){
            latLng = new LatLng(37.8190879, 28.9346472);
        }else if (city.equalsIgnoreCase("Denizli") && district.equalsIgnoreCase("Pamukkale")){
            latLng = new LatLng(37.9112505, 29.1092805);
        }else if (city.equalsIgnoreCase("Diyarbakır") && district.equalsIgnoreCase("Bağlar")){
            latLng = new LatLng(37.7700903, 40.0644002);
        }else if (city.equalsIgnoreCase("Diyarbakır") && district.equalsIgnoreCase("Dicle")){
            latLng = new LatLng(38.3605791, 40.0691968);
        }else if (city.equalsIgnoreCase("Diyarbakır") && district.equalsIgnoreCase("Kayapınar")){
            latLng = new LatLng(37.9871761, 39.7164185);
        }else if (city.equalsIgnoreCase("Diyarbakır") && district.equalsIgnoreCase("Sur")){
            latLng = new LatLng(38.0398484, 40.2828292);
        }else if (city.equalsIgnoreCase("Diyarbakır") && district.equalsIgnoreCase("Yenişehir")){
            latLng = new LatLng(38.0424344, 39.9358972);
        }else if (city.equalsIgnoreCase("Erzurum") && district.equalsIgnoreCase("Palandöken")){
            latLng = new LatLng(39.8388885, 41.0255199);
        }else if (city.equalsIgnoreCase("Erzurum") && district.equalsIgnoreCase("Yakutiye")){
            latLng = new LatLng(40.0635965, 41.1708436);
        }else if (city.equalsIgnoreCase("Eskişehir") && district.equalsIgnoreCase("Odunpazarı")){
            latLng = new LatLng(39.6442025, 30.4760594);
        }else if (city.equalsIgnoreCase("Eskişehir") && district.equalsIgnoreCase("Tepebaşı")){
            latLng = new LatLng(39.8223131, 30.411723);
        }else if (city.equalsIgnoreCase("Gaziantep") && district.equalsIgnoreCase("Şahinbey")){
            latLng = new LatLng(36.9303511, 37.0954946);
        }else if (city.equalsIgnoreCase("Gaziantep") && district.equalsIgnoreCase("Şehitkamil")){
            latLng = new LatLng(37.170228, 37.1761385);
        }else if (city.equalsIgnoreCase("Mersin") && district.equalsIgnoreCase("Erdemli")){
            latLng = new LatLng(36.60626042592977, 34.30895373786727);
        }else if (city.equalsIgnoreCase("Mersin") && district.equalsIgnoreCase("Mezitli")){
            latLng = new LatLng(36.744212492562255, 34.520372204058816);
        }else if (city.equalsIgnoreCase("Mersin") && district.equalsIgnoreCase("Toroslar")){
            latLng = new LatLng(36.834146262063555, 34.60557442632289);
        }else if (city.equalsIgnoreCase("Kayseri") && district.equalsIgnoreCase("Kocasinan")){
            latLng = new LatLng(38.8934174, 35.1888344);
        }else if (city.equalsIgnoreCase("Kayseri") && district.equalsIgnoreCase("Melikgazi")){
            latLng = new LatLng(38.7019793, 35.4033288);
        }else if (city.equalsIgnoreCase("Kocaeli") && district.equalsIgnoreCase("Başiskele")){
            latLng = new LatLng(40.6309694, 29.8987104);
        }else if (city.equalsIgnoreCase("Konya") && district.equalsIgnoreCase("Karatay")){
            latLng = new LatLng(37.9578696, 32.6315798);
        }else if (city.equalsIgnoreCase("Konya") && district.equalsIgnoreCase("Meram")){
            latLng = new LatLng(37.699902, 32.1710677);
        }else if (city.equalsIgnoreCase("Konya") && district.equalsIgnoreCase("Selçuklu")){
            latLng = new LatLng(38.0898126, 32.2011773);
        }else if (city.equalsIgnoreCase("Malatya") && district.equalsIgnoreCase("Battalgazi")){
            latLng = new LatLng(38.4138138, 38.347237);
        }else if (city.equalsIgnoreCase("Manisa") && district.equalsIgnoreCase("Şehzadeler")){
            latLng = new LatLng(38.617564033396384, 27.442383786940475);
        }else if (city.equalsIgnoreCase("Manisa") && district.equalsIgnoreCase("Yunusemre")){
            latLng = new LatLng(38.61925748289873, 27.406333904167436);
        }else if (city.equalsIgnoreCase("Kahramanmaraş") && district.equalsIgnoreCase("Dulkadiroğlu")){
            latLng = new LatLng(37.578089138798255, 36.940395831581796);
        }else if (city.equalsIgnoreCase("Kahramanmaraş") && district.equalsIgnoreCase("Onikişubat")){
            latLng = new LatLng(37.583614011477636, 36.89981070359613);
        }else if (city.equalsIgnoreCase("Sakarya") && district.equalsIgnoreCase("Arifiye")){
            latLng = new LatLng(40.71398339711735, 30.361793459324307);
        }else if (city.equalsIgnoreCase("Sakarya") && district.equalsIgnoreCase("Erenler")){
            latLng = new LatLng(40.75120007675775, 30.41439176629662);
        }else if (city.equalsIgnoreCase("Sakarya") && district.equalsIgnoreCase("Serdivan")){
            latLng = new LatLng(40.73782201230848, 30.350621904447568);
        }else if (city.equalsIgnoreCase("Samsun") && district.equalsIgnoreCase("Atakum")){
            latLng = new LatLng(41.33143333513191, 36.27171692808721);
        }else if (city.equalsIgnoreCase("Samsun") && district.equalsIgnoreCase("Canik")){
            latLng = new LatLng(41.26087772801601, 36.36034400106044);
        }else if (city.equalsIgnoreCase("Samsun") && district.equalsIgnoreCase("İlkadım")){
            latLng = new LatLng(41.2810500466861, 36.331000409610866);
        }else if (city.equalsIgnoreCase("Ardahan") && district.equalsIgnoreCase("Hanak")){
            latLng = new LatLng(41.233576873058084, 42.84802156855797);
        }else if (city.equalsIgnoreCase("Yalova") && district.equalsIgnoreCase("Çiftlikköy")){
            latLng = new LatLng(40.66261378791248, 29.315379477973885);
        }else if (city.equalsIgnoreCase("Balıkesir") && district.equalsIgnoreCase("Balıkesir Merkez")){
            latLng = new LatLng(39.65332454003856, 27.890341925257253);
        }else {
            latLng = null;
        }

        return latLng;
    }

}