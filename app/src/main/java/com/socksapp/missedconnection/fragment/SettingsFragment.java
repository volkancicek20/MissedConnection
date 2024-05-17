package com.socksapp.missedconnection.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentSettingsBinding;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private MainActivity mainActivity;
    private SharedPreferences language;
    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        language = requireActivity().getSharedPreferences("Language",Context.MODE_PRIVATE);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
                mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        binding.languageLinearLayout.setOnClickListener(this::changeLanguage);
        binding.aboutUsLinearLayout.setOnClickListener(v -> goToAboutUsFragment());

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