package com.socksapp.missedconnection.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firestore;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.loginPage.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_registerFragment_to_loginFragment);
        });

        binding.signupEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.signupEmailInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.signupPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.signupPasswordInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.signupConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.signupConfirmInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        binding.signupButton.setOnClickListener(v -> {
            Editable mail = binding.signupEmail.getText();
            Editable password = binding.signupPassword.getText();
            Editable passwordCheck = binding.signupConfirm.getText();

            if(mail != null && password != null && passwordCheck != null){
                String mailText = mail.toString().trim();
                String passwordText = password.toString().trim();
                String passwordConfirmText = passwordCheck.toString().trim();

                boolean isMailValid = isValidEmail(mailText);
                boolean isPasswordValid = !passwordText.isEmpty() && passwordText.length() >= 6
                        && hasUpperCase(passwordText) && hasLowerCase(passwordText) && hasDigit(passwordText);
                boolean passwordsMatch = passwordText.equals(passwordConfirmText);

                if (isMailValid && isPasswordValid && passwordsMatch) {
                    sign(v,mailText,passwordText);
                } else {

                    if(!isMailValid){
                        binding.signupEmailInputLayout.setError(getString(R.string.mail_adresi_ge_ersiz));
                    }else {
                        binding.signupEmailInputLayout.setError(null);
                    }

                    if (!isPasswordValid) {
                        binding.signupPasswordInputLayout.setError(getString(R.string.sifre_girme_kurali));
                    } else {
                        binding.signupPasswordInputLayout.setError(null);
                    }

                    if (!passwordsMatch) {
                        binding.signupConfirmInputLayout.setError(getString(R.string.sifreler_eslesmiyor));
                    } else {
                        binding.signupConfirmInputLayout.setError(null);
                    }
                }
            }
        });
    }

    private void sign(View view,String mail,String password){
        ProgressDialog progressDialog = new ProgressDialog(view.getContext());
        progressDialog.setMessage(getString(R.string.kayit_olunuyor));
        progressDialog.setCancelable(false);
        progressDialog.setInverseBackgroundForced(false);
        progressDialog.show();
        auth.createUserWithEmailAndPassword(mail, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    progressDialog.dismiss();
                    sendEmailVerification(view);
                } else {
                    progressDialog.dismiss();
                    Exception exception = task.getException();
                    if(exception instanceof FirebaseAuthUserCollisionException){
                        Toast.makeText(view.getContext(), getString(R.string.bu_e_posta_adresiyle_zaten_kay_tl_bir_kullan_c_var), Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(view.getContext(), getString(R.string.kayit_olurken_bir_hata_olustu)+"["+exception+"]", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
//                Toast.makeText(view.getContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void sendEmailVerification(View view) {
        auth.getCurrentUser().sendEmailVerification()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    Bundle args = new Bundle();
                    args.putString("mail", auth.getCurrentUser().getEmail());

                    NavController navController = Navigation.findNavController(view);
                    navController.navigate(R.id.action_registerFragment_to_loginFragment, args);

                } else {
//                    Exception exception = task.getException();
                    Toast.makeText(view.getContext(), getString(R.string.dogrulama_gonderilirken_hata_olustu), Toast.LENGTH_SHORT).show();
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

    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }

        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}