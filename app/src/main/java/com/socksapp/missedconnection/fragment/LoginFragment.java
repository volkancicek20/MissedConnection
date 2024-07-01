package com.socksapp.missedconnection.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentLoginBinding;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            String mail = args.getString("mail","");
            if(!mail.isEmpty()){
                binding.loginEmail.setText(mail);
                showSnackbar(view,getString(R.string.dogrulama_e_posta_gonderildi_kontrol_edin));
            }
        }

        binding.forgotpassword.setOnClickListener(v ->{
            if(!binding.loginEmail.getText().toString().trim().isEmpty()){
                resetPassword(v,binding.loginEmail.getText().toString().trim());
            }else {
                binding.loginEmailInputLayout.setError(getString(R.string.mail_adresinizi_giriniz));
                showSnackbar(v,getString(R.string.sifre_degisikligi_icin_e_posta_girin));
            }
        });
        binding.confirmMail.setOnClickListener(v ->{
            auth.getCurrentUser().sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showSnackbar(v,getString(R.string.dogrulama_e_posta_gonderildi_kontrol_edin));
                        binding.confirmMail.setVisibility(View.GONE);
                    } else {
                        showSnackbar(v,getString(R.string.dogrulama_gonderilirken_hata_olustu));
                    }
                });
        });
        binding.register.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment);
        });

        binding.loginEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.loginEmailInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.loginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.loginPasswordInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.loginButton.setOnClickListener(v -> {
            Editable mail = binding.loginEmail.getText();
            Editable password = binding.loginPassword.getText();
            if(mail != null && password != null){
                String mailText = mail.toString().trim();
                String passwordText = password.toString().trim();

                boolean isMailValid = isValidEmail(mailText);
                boolean isPasswordValid = !passwordText.isEmpty();

                if (isMailValid && isPasswordValid) {
                    login(v,mailText,passwordText);
                } else {

                    if(!isMailValid){
                        binding.loginEmailInputLayout.setError(getString(R.string.mail_adresi_ge_ersiz));
                    }else {
                        binding.loginEmailInputLayout.setError(null);
                    }

                    if (!isPasswordValid) {
                        binding.loginPasswordInputLayout.setError(getString(R.string.sifrenizi_giriniz));
                    } else {
                        binding.loginPasswordInputLayout.setError(null);
                    }
                }
            }
        });
    }

    private void login(View view,String mail,String password){
        ProgressDialog progressDialog = new ProgressDialog(view.getContext());
        progressDialog.setMessage(getString(R.string.giris_yapiliyor));
        progressDialog.setCancelable(false);
        progressDialog.setInverseBackgroundForced(false);
        progressDialog.show();
        auth.signInWithEmailAndPassword(mail, password)
            .addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    firestore.collection("userDelete").document(mail).get().addOnSuccessListener(documentSnapshot -> {
                        if(documentSnapshot.exists()){
                            progressDialog.dismiss();
                            showSnackbar(view,getString(R.string.bu_hesap_silinme_asamasindadir));
                        }else {
                            userVerified(view,progressDialog);
                        }
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        showSnackbar(view,getString(R.string.internet_baglantinizi_kontrol_edin));
                    });
                } else {
                    progressDialog.dismiss();
                    Exception exception = task.getException();
                    String errorMessage;

                    if (exception instanceof FirebaseAuthInvalidUserException) {
                        errorMessage = getString(R.string.e_posta_adresi_kay_tl_de_il);
                    } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                        errorMessage = getString(R.string.gecersiz_e_posta_veya_sifre);
                    } else {
                        errorMessage = getString(R.string.giris_yaparken_bir_hata_olu_tu_hata)+"["+exception+"]";
                    }

                    showSnackbar(view,errorMessage);
                }
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                String errorMessage;

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    errorMessage = getString(R.string.gecersiz_e_posta_veya_sifre);
                } else if (e instanceof FirebaseNetworkException) {
                    errorMessage = getString(R.string.nternet_ba_lant_s_bulunamad);
                } else {
                    errorMessage = getString(R.string.unexpected);
                }

                showSnackbar(view,errorMessage);
            });
    }

    private void resetPassword(View view,String mail) {
        auth.sendPasswordResetEmail(mail)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    showSnackbar(view,getString(R.string.sifre_sifirlama_baglantisi_gonderildi));
                } else {
                    Exception exception = task.getException();
                    String errorMessage;
                    if (exception instanceof FirebaseAuthInvalidUserException) {
                        errorMessage = getString(R.string.gecersiz_kullanici);
                    } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                        errorMessage = getString(R.string.gecersiz_kimlik);
                    } else {
                        errorMessage = (exception != null) ? exception.getMessage() : getString(R.string.sifre_sifirlama_ba_lant_s_g_nderilirken_bir_hata_olu_tu);
                    }

                    showSnackbar(view,errorMessage);
                }
            });
    }

    private void userVerified(View view,ProgressDialog progressDialog){
        if(auth.getCurrentUser().isEmailVerified()){
            firestore.collection("users")
                .document(auth.getCurrentUser().getEmail()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        progressDialog.dismiss();
                        Intent intent = new Intent(view.getContext(), MainActivity.class);
                        startActivity(intent);
                        requireActivity().finish();
                    }else {
                        Map<String,Object> data = new HashMap<>();
                        Random random = new Random();
                        int fourDigitNumber = 1000 + random.nextInt(9000);
                        String name = "User"+ fourDigitNumber;
                        String imageUrl = "https://firebasestorage.googleapis.com/v0/b/missedconnection-c000f.appspot.com/o/icon_person_url.png?alt=media&token=427b2c8d-2e7f-4ea5-97df-79e16beec535";
                        data.put("name",name);
                        data.put("imageUrl",imageUrl);

                        firestore.collection("users")
                            .document(auth.getCurrentUser().getEmail())
                            .set(data, SetOptions.merge()).addOnSuccessListener(unused -> {
                                progressDialog.dismiss();
                                Intent intent = new Intent(view.getContext(), MainActivity.class);
                                startActivity(intent);
                                requireActivity().finish();
                        }).addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            showSnackbar(view,getString(R.string.unexpected));
                        });
                    }
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    showSnackbar(view,getString(R.string.unexpected));
                });
        }else {
            progressDialog.dismiss();
            showSnackbar(view,getString(R.string.e_posta_adresinizi_dogrulamadiniz));
            binding.confirmMail.setVisibility(View.VISIBLE);
        }
    }

    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }

        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void showSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        snackbar.setBackgroundTint(Color.rgb(48, 44, 44));

        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);

        snackbar.show();
    }


}