package com.socksapp.missedconnection.fragment;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.FragmentFindBinding;
import com.socksapp.missedconnection.databinding.FragmentMainBinding;

import java.util.ArrayList;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                shutdown(view);
            }
        });

    }

    private void shutdown(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setMessage("Uygulamadan çıkış yapılsın mı?");
        builder.setPositiveButton("Çık", (dialog, which) -> {
            System.exit(0);
        });
        builder.setNegativeButton("Hayır", (dialog, which) -> {

        });
        builder.show();
    }
}