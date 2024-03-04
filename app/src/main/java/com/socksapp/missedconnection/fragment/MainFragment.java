package com.socksapp.missedconnection.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.FragmentFindBinding;
import com.socksapp.missedconnection.databinding.FragmentMainBinding;

import java.util.ArrayList;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private ArrayList<SlideModel> imageList;
    private ImageSlider imageSlider;
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
        imageList = new ArrayList<>();
        imageSlider = binding.imageSlider;

// imageList.add(new SlideModel("String Url" or R.drawable)
// imageList.add(new SlideModel("String Url" or R.drawable, "title") You can add title

        imageList.add(new SlideModel("https://bit.ly/2YoJ77H", "The animal population decreased by 58 percent in 42 years.", ScaleTypes.CENTER_CROP));
        imageList.add(new SlideModel("https://bit.ly/2BteuF2", "Elephants and tigers may become extinct.",ScaleTypes.CENTER_CROP));
        imageList.add(new SlideModel("https://bit.ly/3fLJf72", "And people do that.",ScaleTypes.CENTER_CROP));

        imageSlider.setImageList(imageList);


        binding.find.setOnClickListener(v ->{
            Navigation.findNavController(v).navigate(R.id.action_mainFragment_to_findFragment);
        });
        binding.add.setOnClickListener(v ->{
            Navigation.findNavController(v).navigate(R.id.action_mainFragment_to_addPostFragment);
        });
        binding.settings.setOnClickListener(v ->{
            Navigation.findNavController(v).navigate(R.id.action_mainFragment_to_settingsFragment);
        });
        binding.profile.setOnClickListener(v ->{
            Navigation.findNavController(v).navigate(R.id.action_mainFragment_to_profileFragment);
        });
    }
}