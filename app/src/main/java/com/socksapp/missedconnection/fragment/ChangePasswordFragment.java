package com.socksapp.missedconnection.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.LoginActivity;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentChangePasswordBinding;

public class ChangePasswordFragment extends Fragment {

    private FragmentChangePasswordBinding binding;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private MainActivity mainActivity;
    private String myMail;

    public ChangePasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);

        binding.changePasswordButton.setOnClickListener(this::changePassword);

        myMail = user.getEmail();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        binding.oldPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.oldPasswordInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.newPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.newPasswordInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.newPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.newPasswordConfirmInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void changePassword(View view){
        String password = binding.oldPassword.getText().toString();
        String newPassword = binding.newPassword.getText().toString();
        String newPasswordConfirm = binding.newPasswordConfirm.getText().toString();

        boolean isPasswordValid = !password.isEmpty() && password.length() >= 6;
        boolean isNewPasswordValid = !newPassword.isEmpty() && newPassword.length() >= 6
                && hasUpperCase(newPassword) && hasLowerCase(newPassword) && hasDigit(newPassword);
        boolean passwordsMatch = newPassword.equals(newPasswordConfirm);

        if (isPasswordValid && isNewPasswordValid && passwordsMatch) {
            updatePassword(view,password, newPassword);
        } else {
            if (!isPasswordValid) {
                binding.oldPasswordInputLayout.setError("Geçersiz şifre");
            } else {
                binding.oldPasswordInputLayout.setError(null);
            }

            if (!isNewPasswordValid) {
                binding.newPasswordInputLayout.setError("Şifre en az 6 karakter uzunluğunda olmalı, en az 1 büyük harf, 1 küçük harf ve 1 rakam içermelidir.");
            } else {
                binding.newPasswordInputLayout.setError(null);
            }

            if (!passwordsMatch) {
                binding.newPasswordConfirmInputLayout.setError("Şifreler eşleşmiyor");
            } else {
                binding.newPasswordConfirmInputLayout.setError(null);
            }
        }
    }

    private void updatePassword(View view,String password, String newPassword){
        AuthCredential credential = EmailAuthProvider.getCredential(myMail, password);
        user.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                user.updatePassword(newPassword).addOnSuccessListener(unused -> {
                    Toast.makeText(view.getContext(),"Şifreniz güncellendi. Tekrar giriş yapınız.",Toast.LENGTH_LONG).show();
                    auth.signOut();
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(view.getContext(),"Bir hata oluştu. Lütfen daha sonra tekrar deneyiniz.",Toast.LENGTH_SHORT).show();
                });
            } else {
                binding.oldPasswordInputLayout.setError("Şifrenizi yanlış girdiniz");
            }
        });
    }

    private static boolean hasUpperCase(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isUpperCase(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLowerCase(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLowerCase(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDigit(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof MainActivity){
            mainActivity = (MainActivity) context;
        }
    }
}