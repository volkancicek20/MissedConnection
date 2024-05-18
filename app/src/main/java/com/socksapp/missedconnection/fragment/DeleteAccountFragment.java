package com.socksapp.missedconnection.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.LoginActivity;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentDeleteAccountBinding;

import java.util.HashMap;
import java.util.Map;

public class DeleteAccountFragment extends Fragment {

    private FragmentDeleteAccountBinding binding;
    private MainActivity mainActivity;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String myMail;

    public DeleteAccountFragment() {
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
        binding = FragmentDeleteAccountBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);


        myMail = user.getEmail();

        binding.deleteAccountButton.setOnClickListener(v -> deleteAccount(view));

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

        binding.explain.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.explainInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void deleteAccount(View view){

        String password = binding.oldPassword.getText().toString();
        String explain = binding.explain.getText().toString();

        if(!password.isEmpty() && !explain.isEmpty()){
            checkPassword(view,password,explain);
        }else {
            if(password.isEmpty()){
                binding.oldPasswordInputLayout.setError("Şifrenizi giriniz!");
            }
            if(explain.isEmpty()){
                binding.explainInputLayout.setError("Bu alan boş bırakılamaz!");
            }
        }
    }

    private void checkPassword(View view,String password, String explain){
        AuthCredential credential = EmailAuthProvider.getCredential(myMail, password);
        user.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                deleteUserAccount(view,explain);
            } else {
                binding.oldPasswordInputLayout.setError("Şifreniz yanlış");
            }
        });
    }

    private void deleteUserAccount(View view,String explain) {

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Hesap Silme Onayı");
        builder.setMessage("Bu işlem hesabınızı ve tüm verilerinizi kalıcı olarak silecektir. Devam etmek istediğinize emin misiniz?");
        builder.setPositiveButton("Evet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WriteBatch deleteBatch = firestore.batch();

                Map<String, Object> data = new HashMap<>();
                data.put("explain", explain);
                DocumentReference userDeleteRef = firestore.collection("userDelete").document(myMail);
                deleteBatch.set(userDeleteRef, data);

                deleteBatch.commit().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Hesabınız 1 hafta içinde silinecektir.", Toast.LENGTH_LONG).show();
                        auth.signOut();
                        Intent intent = new Intent(requireActivity(), LoginActivity.class);
                        startActivity(intent);
                        requireActivity().finish();
                    } else {
                        Toast.makeText(getContext(), "Hesap silme işlemi başarısız. Lütfen tekrar deneyiniz", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        builder.setNegativeButton("İptal", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();


//        user.delete().addOnCompleteListener(task -> {
//            if(task.isSuccessful()){
//                Toast.makeText(getContext(), "Hesap başarıyla silindi.", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(requireActivity(), LoginActivity.class);
//                startActivity(intent);
//                requireActivity().finish();
//            }
//        }).addOnFailureListener(e -> {
//
//        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof MainActivity){
            mainActivity = (MainActivity) context;
        }
    }
}