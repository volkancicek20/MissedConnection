package com.socksapp.missedconnection.fragment;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;
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
    private Double mainLat,mainLng;
    private int radius,mainRadius;

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
            mainLat = args.getDouble("fragment_lat");
            mainLng = args.getDouble("fragment_lng");
            mainRadius = args.getInt("fragment_radius");
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(type.equals("main")){
            binding.slider.setVisibility(View.GONE);
            binding.mapSearch.setVisibility(View.GONE);
            binding.saveLocation.setVisibility(View.GONE);
        }

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.includedLayout.setVisibility(View.GONE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

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
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.dark_map));
        if(type.equals("main")){
            LatLng turkey = new LatLng(mainLat, mainLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(turkey, 15));

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(turkey);

            mMap.addMarker(markerOptions);

            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(turkey);
            circleOptions.radius(mainRadius);
            circleOptions.strokeColor(Color.BLACK);
            circleOptions.fillColor(0x30ff0000);
            circleOptions.strokeWidth(2);
            mMap.addCircle(circleOptions);
            mMap.setOnMapClickListener(null);
        }else {
            loadCoordinate();

            mMap.setOnMapClickListener(latLng -> {

                try {
                    Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                    if (addresses != null && addresses.size() > 0) {
                        String cityFind = addresses.get(0).getAdminArea();
                        String districtFind = addresses.get(0).getSubAdminArea();

                        if(city.equals(cityFind) && district.equals(districtFind)){
                            radius = 100;
                            customLatLng = new LatLng(latLng.latitude,latLng.longitude);
                            binding.slider.setValue(100);
                            binding.slider.setVisibility(View.VISIBLE);
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(latLng);

                            try {
                                geocoder = new Geocoder(requireContext(), Locale.getDefault());
                                addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                                String streetAddress = "";
                                if (addresses != null && addresses.size() > 0) {
                                    Address address = addresses.get(0);
                                    streetAddress = address.getAddressLine(0);
                                }
                                markerOptions.title(streetAddress);
                            }catch (Exception e){
                                markerOptions.title(latLng.latitude + " / " + latLng.longitude);
                                e.printStackTrace();
                            }

                            mMap.clear();
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
                            mMap.addMarker(markerOptions);

                            if (currentCircle != null) {
                                currentCircle.remove();
                            }

                            CircleOptions circleOptions = new CircleOptions();
                            circleOptions.center(latLng);
                            circleOptions.radius(radius);
                            circleOptions.strokeColor(Color.BLACK);
                            circleOptions.fillColor(0x30ff0000);
                            circleOptions.strokeWidth(2);
                            currentCircle = mMap.addCircle(circleOptions);
                        }else {
                            Toast.makeText(requireContext(),city+","+district+" alanının dışına çıkamazsınız",Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        // Coğrafi bilgi bulunamadıysa uygun bir işlem yapın
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }


        binding.slider.addOnChangeListener((slider, value, fromUser) -> {
            if (currentCircle != null && fromUser) {
                radius = (int) value;
                currentCircle.setRadius(value);
            }
        });

        binding.saveLocation.setOnClickListener(v ->{
            if (customLatLng != null) {
                saveLocationWithRadius(customLatLng, (int) binding.slider.getValue());
            } else {
                Toast.makeText(requireContext(), "Haritaya tıklama yapmadınız.", Toast.LENGTH_SHORT).show();
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

                LatLngBounds curScreen = mMap.getProjection()
                        .getVisibleRegion().latLngBounds;
                System.out.println("A: "+curScreen.northeast);
                System.out.println("B: "+curScreen.southwest);
                System.out.println("C: "+curScreen.getCenter());

            } else {
                System.out.println("No location found for the given address.");
            }
        } catch (Exception e) {
            System.out.println("exception: "+e.getLocalizedMessage());
            e.printStackTrace();
        }

    }


    private void saveLocationWithRadius(LatLng center, double radius) {

        if(type.equals("find_post")){
            FindFragment.rad = radius;
            FindFragment.lat = center.latitude;
            FindFragment.lng = center.longitude;
        } else if (type.equals("add_post")) {
            AddPostFragment.rad = radius;
            AddPostFragment.lat = center.latitude;
            AddPostFragment.lng = center.longitude;
        }

        requireActivity().getSupportFragmentManager().popBackStack();

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

}