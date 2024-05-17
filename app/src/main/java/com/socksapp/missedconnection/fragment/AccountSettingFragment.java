package com.socksapp.missedconnection.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.LoginActivity;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentAccountSettingBinding;

import java.util.HashMap;
import java.util.Map;

public class AccountSettingFragment extends Fragment {
    private FragmentAccountSettingBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private SharedPreferences nameShared,imageUrlShared;
    private String myMail;
    private MainActivity mainActivity;

    public AccountSettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        nameShared = requireActivity().getSharedPreferences("Name",Context.MODE_PRIVATE);
        imageUrlShared = requireActivity().getSharedPreferences("ImageUrl",Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountSettingBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);

        setProfile(view);

        myMail = user.getEmail();

        binding.deleteAccountLinearLayout.setOnClickListener(this::deleteAccount);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void deleteAccount(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Hesap Silme Onayı");
        builder.setMessage("Bu işlem hesabınızı ve tüm verilerinizi kalıcı olarak silecektir. Devam etmek istediğinize emin misiniz?");
        builder.setPositiveButton("Evet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteUserAccount();
            }
        });

        builder.setNegativeButton("İptal", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void deleteUserAccount() {
        WriteBatch deleteBatch = firestore.batch();

        Map<String, Object> data = new HashMap<>();
        data.put("mail", myMail);
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

    private void setProfile(View view){
        String name = nameShared.getString("name","");
        String imageUrl = imageUrlShared.getString("imageUrl","");

        if(!name.isEmpty()){
            binding.profileName.setText(name);
        }
        if(!imageUrl.isEmpty()){
            Glide.with(view.getContext())
                .load(imageUrl)
                .apply(new RequestOptions()
                .error(R.drawable.person_active_96)
                .centerCrop())
                .into(binding.profileImage);
        }else {
            binding.profileImage.setImageResource(R.drawable.person_active_96);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof MainActivity){
            mainActivity = (MainActivity) context;
        }
    }
}