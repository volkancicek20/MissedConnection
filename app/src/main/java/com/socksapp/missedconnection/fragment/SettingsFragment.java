package com.socksapp.missedconnection.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentSettingsBinding;

import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private MainActivity mainActivity;
    private SharedPreferences language,myLocationCity,myLocationDistrict,notificationMessage,notificationPost;
    private String userLocationCity,userLocationDistrict;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_REQUEST_CODE = 100;
    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        language = requireActivity().getSharedPreferences("Language",Context.MODE_PRIVATE);
        myLocationCity = requireActivity().getSharedPreferences("MyLocationCity", Context.MODE_PRIVATE);
        myLocationDistrict = requireActivity().getSharedPreferences("MyLocationDistrict", Context.MODE_PRIVATE);
        notificationMessage = requireActivity().getSharedPreferences("NotificationMessage", Context.MODE_PRIVATE);
        notificationPost = requireActivity().getSharedPreferences("NotificationPost", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater,container,false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);

        userLocationCity = myLocationCity.getString("myLocationCity","");
        userLocationDistrict = myLocationDistrict.getString("myLocationDistrict","");

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
                mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        if(userLocationCity.isEmpty()){
            binding.locationText.setText(R.string.konum);
        }else {
            String lct = getString(R.string.konum) + " - "+userLocationCity+"/"+userLocationDistrict;
            binding.locationText.setText(lct);
        }

        binding.languageLinearLayout.setOnClickListener(this::changeLanguage);
        binding.aboutUsLinearLayout.setOnClickListener(v -> goToAboutUsFragment());
        binding.myLocationLinearLayout.setOnClickListener(this::setMyLocation);
//        binding.notificationLinearLayout.setOnClickListener(this::changeNotification);
    }

//    private void changeNotification(View v){
//        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
//
//        LayoutInflater inflater = LayoutInflater.from(v.getContext());
//        View view = inflater.inflate(R.layout.change_notification_layout, null);
//
//        CheckBox checkbox1 = view.findViewById(R.id.checkbox1);
//        CheckBox checkbox2 = view.findViewById(R.id.checkbox2);
//
//        if(notificationMessage.getString("notificationMessage","").isEmpty()){
//            checkbox1.setChecked(true);
//        }else {
//            checkbox1.setChecked(false);
//        }
//
//        if(notificationPost.getString("notificationPost","").isEmpty()){
//            checkbox2.setChecked(true);
//        }else {
//            checkbox2.setChecked(false);
//        }
//
//        builder.setView(view);
//
//        builder.setNegativeButton("İptal", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//
//        builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                boolean isChecked1 = checkbox1.isChecked();
//                boolean isChecked2 = checkbox2.isChecked();
//
//                SharedPreferences.Editor editor = notificationMessage.edit();
//                if (isChecked1) {
//                    editor.clear();
//                }else {
//                    editor.putString("notificationMessage","1");
//                }
//                editor.apply();
//
//                SharedPreferences.Editor editor2 = notificationPost.edit();
//                if (isChecked2) {
//                    editor2.clear();
//                }else {
//                    editor2.putString("notificationPost","1");
//                }
//                editor2.apply();
//
//                dialog.dismiss();
//            }
//        });
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }

    private void setMyLocation(View view){
        checkLocationPermission();
    }

    private void checkLocationPermission(){
        if(ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireActivity(),Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getUserLocation();
        }else {
            requestForPermission();
        }
    }

    private void requestForPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            showPermissionExplanation();
        } else {
            showSettingsDialog();
        }
    }

    private void showPermissionExplanation() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Konum İzni Gerekli")
            .setMessage("Bu uygulama konumunuza erişmek için izne ihtiyaç duyar. Lütfen konum iznini verin.")
            .setPositiveButton("Tamam", (dialog, which) -> ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE))
            .setNegativeButton("İptal", (dialog, which) -> dialog.dismiss())
            .create()
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                        && !ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    showSettingsDialog();
                }
            }
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("İzin Gerekli")
            .setMessage("Konum izni vermediğiniz için bu özellik çalışmıyor. Lütfen izin vermek için ayarlara gidin.")
            .setPositiveButton("Ayarlar", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("İptal", (dialog, which) -> dialog.dismiss())
            .create()
            .show();
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

                        String lct = getString(R.string.konum) + " - "+userLocationCity+"/"+userLocationDistrict;
                        binding.locationText.setText(lct);

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

                String lct = getString(R.string.konum) + " - "+userLocationCity+"/"+userLocationDistrict;
                binding.locationText.setText(lct);
            }
        });
    }

    private void changeLanguage(View view){

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.selection_language, null);
        builder.setView(dialogView);

        RadioGroup radioGroupLanguage = dialogView.findViewById(R.id.language_radio_group);
        RadioButton radioButtonEnglish = dialogView.findViewById(R.id.english_radio_button);

        String getLanguage = language.getString("language","");

        if(getLanguage.equals("english")){
            radioButtonEnglish.setChecked(true);
        }

        builder.setPositiveButton("Seç", (dialog, which) -> {
            int selectedId = radioGroupLanguage.getCheckedRadioButtonId();

            Locale locale;
            SharedPreferences.Editor editor = language.edit();
            if(selectedId == R.id.turkish_radio_button){

                editor.putString("language","turkish");
                editor.apply();

                locale = new Locale("en");
                Locale.setDefault(locale);

                Configuration configuration = new Configuration();
                configuration.setLocale(locale);

                getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());

            }else {

                editor.putString("language","english");
                editor.apply();


                locale = new Locale("tr");
                Locale.setDefault(locale);

                Configuration configuration = new Configuration();
                configuration.setLocale(locale);

                getResources().updateConfiguration(configuration,getResources().getDisplayMetrics());

            }
            requireActivity().recreate();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void goToAboutUsFragment(){
        AboutUsFragment fragment = new AboutUsFragment();
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
}