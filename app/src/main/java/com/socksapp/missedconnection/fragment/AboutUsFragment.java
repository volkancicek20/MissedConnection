package com.socksapp.missedconnection.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentAboutUsBinding;

import java.util.Calendar;

public class AboutUsFragment extends Fragment {

    private FragmentAboutUsBinding binding;
    private MainActivity mainActivity;

    public AboutUsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAboutUsBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);


        setVersionName(view);
        setCopyright();

        binding.mailLinear.setOnClickListener(this::goMail);
        binding.playstoreLinear.setOnClickListener(this::ratePlayStore);
        binding.instagramLinear.setOnClickListener(this::linkInstagram);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void linkInstagram(View view){
        Uri uri = Uri.parse("http://instagram.com/_u/volkancicex");
        Intent instagramIntent = new Intent(Intent.ACTION_VIEW, uri);
        instagramIntent.setPackage("com.instagram.android");

        try {
            startActivity(instagramIntent);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/volkancicex")));
        }
    }

    private void ratePlayStore(View view){
        Intent rateIntent = new Intent(Intent.ACTION_VIEW);
        rateIntent.setData(Uri.parse("market://details?id=" + requireActivity().getPackageName()));
        try {
            startActivity(rateIntent);
        } catch (Exception e) {
            rateIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + requireActivity().getPackageName()));
            startActivity(rateIntent);
        }
    }


    private void goMail(View view){
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"socksapp@protonmail.com"});

        if (emailIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(emailIntent);
        } else {
            Toast.makeText(view.getContext(), "No email client found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setVersionName(View view){
        try {
            PackageManager packageManager = view.getContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(view.getContext().getPackageName(), 0);
            String app_details = "Missed Connection - v" + packageInfo.versionName;
            binding.appDetails.setText(app_details);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCopyright(){
        @SuppressLint("DefaultLocale") final String copyrightString = String.format("Copyright %d by Sock's App", Calendar.getInstance().get(Calendar.YEAR));
        binding.copyright.setText(copyrightString);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
}