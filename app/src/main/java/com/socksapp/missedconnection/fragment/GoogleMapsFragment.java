package com.socksapp.missedconnection.fragment;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.FragmentFindBinding;
import com.socksapp.missedconnection.databinding.FragmentGoogleMapsBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleMapsFragment extends Fragment implements OnMapReadyCallback {

    private FragmentGoogleMapsBinding binding;
    private FirebaseFirestore firestore;
    private GoogleMap mMap;
    private String type;

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
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        LatLng turkey = new LatLng(39.9208, 32.8541);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(turkey, 5));

        mMap.setOnMapClickListener(latLng -> {

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(latLng.latitude+" KG " + latLng.longitude);
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
            mMap.addMarker(markerOptions);
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(latLng);
            circleOptions.radius(500);
            circleOptions.strokeColor(Color.BLACK);
            circleOptions.fillColor(0x30ff0000);
            circleOptions.strokeWidth(2);
            mMap.addCircle(circleOptions);

            binding.saveLocation.setOnClickListener(v ->{

            });

            saveLocationWithRadius(latLng, 500);

        });
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
}