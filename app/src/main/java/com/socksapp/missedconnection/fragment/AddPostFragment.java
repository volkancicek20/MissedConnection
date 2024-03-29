package com.socksapp.missedconnection.fragment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentAddPostBinding;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddPostFragment extends Fragment {

    private FragmentAddPostBinding binding;

    private String[] cityNames,districtNames;
    private ArrayAdapter<String> cityAdapter,districtAdapter;
    private AutoCompleteTextView cityCompleteTextView,districtCompleteTextView;
    public static Double lat,lng;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;
    private int mYear,mMonth,mDay;
    private MainActivity mainActivity;

    public AddPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lat = 0.0;
        lng = 0.0;
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

        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);

        cityNames = getResources().getStringArray(R.array.city_names);
        cityAdapter = new ArrayAdapter<>(requireContext(), R.layout.list_item,cityNames);
        cityCompleteTextView = binding.getRoot().findViewById(R.id.city_complete_text);
        cityCompleteTextView.setAdapter(cityAdapter);

        binding.cityCompleteText.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedCity = parent.getItemAtPosition(position).toString();
            binding.districtCompleteText.setText("");
            binding.districtCompleteText.setAdapter(null);
            selectDistrict(selectedCity);
        });

        binding.dateTitleConstraintLayout.setOnTouchListener((v, event) -> {
            int checkVisible = binding.visibleDatePicker.getVisibility();
            if(checkVisible == View.GONE){
                binding.visibleDatePicker.setVisibility(View.VISIBLE);
            }else {
                binding.visibleDatePicker.setVisibility(View.GONE);
            }
            return false;
        });

        binding.mapViewConstraintLayout.setOnTouchListener((v, event) -> {
            int checkVisible = binding.mapView.getVisibility();
            if(checkVisible == View.GONE){
                binding.mapView.setVisibility(View.VISIBLE);
            }else {
                binding.mapView.setVisibility(View.GONE);
            }

            return false;
        });

        binding.dateEditText.setOnTouchListener((v, event) -> {
            showCustomDateDialog(v);
            return false;
        });

        binding.timeEditText.setOnTouchListener((v, event) -> {
            showCustomTimeDialog(v);
            return false;
        });


        binding.mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                LatLng location = new LatLng(40.7128, -74.0060);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10));
                googleMap.addMarker(new MarkerOptions().position(location).title("New York City"));

                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng latLng) {
                        String city = binding.cityCompleteText.getText().toString();
                        String district = binding.districtCompleteText.getText().toString();
                        if(!city.isEmpty() && !district.isEmpty()){
                            Bundle args = new Bundle();
                            args.putString("fragment_type", "add_post");
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

        });
    }

    private void showCustomTimeDialog(View view) {
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
                        binding.timeEditText.setText(timeString);
                    }
                },
                hour,
                minute,
                true
            );
        }


        timePickerDialog.show();
    }
    private void showCustomDateDialog(View view) {
        if(datePickerDialog == null){
            final Calendar calendar = Calendar.getInstance();
            mYear = calendar.get(Calendar.YEAR);
            mMonth = calendar.get(Calendar.MONTH);
            mDay = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    String timeString = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
                    binding.dateEditText.setText(timeString);
                }
            },mYear,mMonth,mDay);
        }

        datePickerDialog.show();
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
}