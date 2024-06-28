package com.socksapp.missedconnection.fragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentGoogleMapsBinding;

import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class GoogleMapsFragment extends Fragment implements OnMapReadyCallback {

    private FragmentGoogleMapsBinding binding;
    private FirebaseFirestore firestore;
    private GoogleMap mMap;
    private String type,city,district,streetAddress;
    private MainActivity mainActivity;
    private LatLng customLatLng;
    private Circle currentCircle;
    private Double mainLat,mainLng;
    private int radius,mainRadius;
    private static final Map<Character, Character> TURKCE_INGILIZCE_ESLEME = new HashMap<>();

    static {
        TURKCE_INGILIZCE_ESLEME.put('ç', 'c');
        TURKCE_INGILIZCE_ESLEME.put('Ç', 'C');
        TURKCE_INGILIZCE_ESLEME.put('ğ', 'g');
        TURKCE_INGILIZCE_ESLEME.put('Ğ', 'G');
        TURKCE_INGILIZCE_ESLEME.put('ı', 'i');
        TURKCE_INGILIZCE_ESLEME.put('İ', 'I');
        TURKCE_INGILIZCE_ESLEME.put('ö', 'o');
        TURKCE_INGILIZCE_ESLEME.put('Ö', 'O');
        TURKCE_INGILIZCE_ESLEME.put('ş', 's');
        TURKCE_INGILIZCE_ESLEME.put('Ş', 'S');
        TURKCE_INGILIZCE_ESLEME.put('ü', 'u');
        TURKCE_INGILIZCE_ESLEME.put('Ü', 'U');
        for (char c = 'a'; c <= 'z'; c++) {
            TURKCE_INGILIZCE_ESLEME.put(c, c);
            TURKCE_INGILIZCE_ESLEME.put(Character.toUpperCase(c), Character.toUpperCase(c));
        }
    }

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

        streetAddress = "";

        if(type.equals("main")){
            binding.slider.setVisibility(View.GONE);
            binding.mapSearch.setVisibility(View.INVISIBLE);
            binding.saveLocation.setVisibility(View.GONE);
        }

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.includedLayout.setVisibility(View.GONE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        binding.slider.setLabelFormatter(value -> {
            int intValue = Math.round(value);
            return String.valueOf(intValue);
        });

        changeSearchViewIconColor(view,binding.mapSearch,R.color.white);

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
                    showSnackbar(view,getString(R.string.aradiginiz_yer_bulunamadi));
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

        binding.backSpace.setOnClickListener(v ->{
            requireActivity().onBackPressed();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.dark_map));
        if(type.equals("main")){
            LatLng location = new LatLng(mainLat, mainLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(location);

            mMap.addMarker(markerOptions);

            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(location);
            circleOptions.radius(mainRadius);
            circleOptions.strokeColor(Color.RED);
            circleOptions.fillColor(0x30ff0000);
            circleOptions.strokeWidth(1);
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


                        if(areStringsEqual(city, cityFind) && areStringsEqual(district, districtFind)){
                            radius = 100;
                            customLatLng = new LatLng(latLng.latitude,latLng.longitude);
                            binding.slider.setValue(100);
                            binding.slider.setVisibility(View.VISIBLE);
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(latLng);

                            try {
                                geocoder = new Geocoder(requireContext(), Locale.getDefault());
                                addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                                streetAddress = "";
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
                            circleOptions.strokeColor(Color.RED);
                            circleOptions.fillColor(0x30ff0000);
                            circleOptions.strokeWidth(1);
                            currentCircle = mMap.addCircle(circleOptions);
                        }else {
                            showSnackbar(requireView(),city+","+district+getString(R.string.alanin_disina_cikamazsiniz) + getString(R.string.se_ti_iniz_konum)+cityFind+","+districtFind);
                           // Toast.makeText(requireContext(),city+","+district+getString(R.string.alanin_disina_cikamazsiniz),Toast.LENGTH_SHORT).show();
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
                saveLocationWithRadius(customLatLng, (int) binding.slider.getValue(),streetAddress);
            } else {
                showSnackbar(v,getString(R.string.haritada_isaretleme_yapmadiniz));
            }
        });
    }

    public static boolean areStringsEqual(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return Objects.equals(str1, str2); // İkisi de null ise eşit kabul et
        }

        // Normalleştirme (NFD) ve küçük harfe çevirme
        str1 = normalizeAndRemoveDiacritics(str1).toLowerCase(Locale.ROOT);
        str2 = normalizeAndRemoveDiacritics(str2).toLowerCase(Locale.ROOT);

        // Karakterleri eşleme tablosuna göre dönüştürme
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < str1.length(); i++) {
            sb1.append(TURKCE_INGILIZCE_ESLEME.getOrDefault(str1.charAt(i), str1.charAt(i)));
        }
        for (int i = 0; i < str2.length(); i++) {
            sb2.append(TURKCE_INGILIZCE_ESLEME.getOrDefault(str2.charAt(i), str2.charAt(i)));
        }

        // Dönüştürülmüş metinleri karşılaştırma
        return sb1.toString().equals(sb2.toString());
    }

    public static String normalizeAndRemoveDiacritics(String str) {
        // Normalize to NFD (Normalization Form D)
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        // Remove diacritical marks
        return normalized.replaceAll("\\p{M}", "");
    }

    private void loadCoordinate(){

        LatLng latLng = handleManualDistricts(city,district);

        if(latLng != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        }else {
            Geocoder geocoder = new Geocoder(requireContext());
            try {
                List<Address> addressList = geocoder.getFromLocationName(city + ", " + district, 1);
                if (addressList != null && addressList.size() > 0) {
                    double latitude = addressList.get(0).getLatitude();
                    double longitude = addressList.get(0).getLongitude();

                    LatLng location = new LatLng(latitude, longitude);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));

                } else {
//                System.out.println("No location found for the given address.");
                }
            } catch (Exception e) {
//            System.out.println("exception: "+e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

    }


    private void saveLocationWithRadius(LatLng center, double radius,String address) {

        if(type.equals("find_post")){
            FindFragment.rad = radius;
            FindFragment.lat = center.latitude;
            FindFragment.lng = center.longitude;
            FindFragment.address = address;
        } else if (type.equals("add_post")) {
            AddPostFragment.rad = radius;
            AddPostFragment.lat = center.latitude;
            AddPostFragment.lng = center.longitude;
            AddPostFragment.address = address;
        }

        requireActivity().getSupportFragmentManager().popBackStack();

    }

    private void changeSearchViewIconColor(View view, SearchView searchView, int color) {
        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);

        searchIcon.setColorFilter(ContextCompat.getColor(view.getContext(), color), PorterDuff.Mode.SRC_IN);

        ImageView clearIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);

        clearIcon.setColorFilter(ContextCompat.getColor(view.getContext(), color), PorterDuff.Mode.SRC_IN);

        SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchAutoComplete.setTextColor(Color.WHITE);
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