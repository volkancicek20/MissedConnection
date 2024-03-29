package com.socksapp.missedconnection.fragment;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentGoogleMapsBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GoogleMapsFragment extends Fragment implements OnMapReadyCallback {

    private FragmentGoogleMapsBinding binding;
    private FirebaseFirestore firestore;
    private GoogleMap mMap;
    private String type,city,district;
    private MainActivity mainActivity;
    private LatLng customLatLng;
    private Circle currentCircle;

    public GoogleMapsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGoogleMapsBinding.inflate(inflater,container,false);
        Bundle args = getArguments();
        if (args != null) {
            type = args.getString("fragment_type");
            city = args.getString("fragment_city");
            district = args.getString("fragment_district");
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);

        binding.slider.setLabelFormatter(value -> {
            int intValue = Math.round(value);
            return String.valueOf(intValue);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        binding.mapSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = binding.mapSearch.getQuery().toString();
                List<Address> addressList = null;
                Geocoder geocoder = new Geocoder(requireActivity());
                try {
                    addressList = geocoder.getFromLocationName(location,1);
                }catch (Exception e){
                    e.printStackTrace();
                }
                if (addressList != null && !addressList.isEmpty()) {
                    Address address = addressList.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(),address.getLongitude());
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(location));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,10));
                }else {
                    Toast.makeText(requireContext(),"Yer bulunamadı.",Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        loadCoordinate();

        mMap.setOnMapClickListener(latLng -> {
            customLatLng = new LatLng(latLng.latitude,latLng.longitude);
            binding.slider.setValue(100);
            binding.slider.setVisibility(View.VISIBLE);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(latLng.latitude+" KG " + latLng.longitude);
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
            mMap.addMarker(markerOptions);

            if (currentCircle != null) {
                currentCircle.remove();
            }

            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(latLng);
            circleOptions.radius(100);
            circleOptions.strokeColor(Color.BLACK);
            circleOptions.fillColor(0x30ff0000);
            circleOptions.strokeWidth(2);
//            mMap.addCircle(circleOptions);
            currentCircle = mMap.addCircle(circleOptions);

            binding.saveLocation.setOnClickListener(v ->{

            });
            saveLocationWithRadius(latLng, 500);
        });

        binding.slider.addOnChangeListener((slider, value, fromUser) -> {
            if (currentCircle != null && fromUser) {
                currentCircle.setRadius(value);
            }
        });
    }

    private void loadCoordinate(){

        Geocoder geocoder = new Geocoder(requireContext());
        try {
            List<Address> addressList = geocoder.getFromLocationName(city + ", " + district, 1);
            if (addressList != null && addressList.size() > 0) {
                double latitude = addressList.get(0).getLatitude();
                double longitude = addressList.get(0).getLongitude();

                LatLng turkey = new LatLng(latitude, longitude);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(turkey, 13));
            } else {
                System.out.println("No location found for the given address.");
            }
        } catch (Exception e) {
            System.out.println("exception: "+e.getLocalizedMessage());
            e.printStackTrace();
        }

    }


    private void saveLocationWithRadius(LatLng center, double radius) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", center.latitude);
        locationData.put("longitude", center.longitude);
        locationData.put("radius", radius);

        if(type.equals("find_post")){
            FindFragment.lat = center.latitude;
            FindFragment.lng = center.longitude;
        } else if (type.equals("add_post")) {

        }else {

        }

//        firestore.collection("savedLocations")
//            .add(locationData)
//            .addOnSuccessListener(documentReference -> {
//
//            })
//            .addOnFailureListener(e -> {
//
//            });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

}