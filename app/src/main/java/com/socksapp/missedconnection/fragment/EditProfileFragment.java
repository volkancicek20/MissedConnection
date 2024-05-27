package com.socksapp.missedconnection.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.databinding.FragmentEditProfileBinding;
import com.socksapp.missedconnection.myclass.SharedPreferencesHelper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    public ActivityResultLauncher<Intent> activityResultLauncher;
    public ActivityResultLauncher<String> permissionLauncher;
    private SharedPreferences nameShared,imageUrlShared;
    private Bitmap selectedBitmap;
    private Uri imageData;
    private String myUserName,myImageUrl,userMail;
    private MainActivity mainActivity;
    private SharedPreferencesHelper sharedPreferencesHelper;
    public EditProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        nameShared = requireActivity().getSharedPreferences("Name",Context.MODE_PRIVATE);
        imageUrlShared = requireActivity().getSharedPreferences("ImageUrl",Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_backspace);

        sharedPreferencesHelper = new SharedPreferencesHelper(view.getContext());

        userMail = user.getEmail();

        imageData = null;

        setProfile(view);
        registerLauncher(view);

        binding.profileImage.setOnClickListener(this::setImage);

        binding.saveProfile.setOnClickListener(this::saveProfile);

        binding.nameTextInputLayout.setEndIconOnClickListener(v ->{
            binding.nameEdittext.setEnabled(true);
            binding.nameEdittext.requestFocus();
            binding.nameTextInputLayout.setEndIconVisible(false);
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
                mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

    }

    private void saveProfile(View view){
        String nameString = binding.nameEdittext.getText().toString();
        boolean nameCheck;
        nameCheck = !nameString.isEmpty();
        uploadProfile(view,nameString,nameCheck);
    }

    private void uploadProfile(View view, String uploadName,boolean nameCheck) {
        ProgressDialog progressDialog = new ProgressDialog(view.getContext());
        progressDialog.setMessage(getString(R.string.kaydediliyor));
        progressDialog.show();
        DocumentReference usersRef = firestore.collection("users").document(userMail);
        WriteBatch batch = firestore.batch();
        Map<String, Object> updates = new HashMap<>();
        if (imageData != null) {

            if(nameCheck){
                if(!nameShared.getString("name","").isEmpty()){
                    if(!uploadName.equals(myUserName)){
                        updates.put("name", uploadName);
                        batch.set(usersRef, updates, SetOptions.merge());
                    }else {

                    }
                }else {
                    updates.put("name", uploadName);
                    batch.set(usersRef, updates, SetOptions.merge());
                }
            }

            storageReference.child("userProfilePhoto").child(userMail).putFile(imageData)
                    .addOnSuccessListener(taskSnapshot -> {
                        Task<Uri> downloadUrlTask = taskSnapshot.getStorage().getDownloadUrl();
                        downloadUrlTask.addOnCompleteListener(task -> {
                            String imageUrl = task.getResult().toString();

                            batch.update(usersRef, "imageUrl", imageUrl);

                            batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    updateProfile(nameCheck,uploadName,imageUrl);
                                    showToastShort(getString(R.string.profiliniz_kaydedildi));
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    showErrorMessage(view.getContext(), e.getLocalizedMessage());
                                });
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        showErrorMessage(view.getContext(), e.getLocalizedMessage());
                    });

        } else {

            boolean checkCommit = true;

            if(nameCheck){
                if(!nameShared.getString("name","").isEmpty()){
                    if(!uploadName.equals(myUserName)){
                        updates.put("name", uploadName);
                        batch.set(usersRef, updates, SetOptions.merge());
                    }else {
                        checkCommit = false;
                    }
                }else {
                    updates.put("name", uploadName);
                    batch.set(usersRef, updates, SetOptions.merge());
                }
            }else {
                checkCommit = false;
            }

            if(checkCommit){
                batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        updateProfile(nameCheck,uploadName);
                        showToastShort(getString(R.string.profiliniz_kaydedildi));
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        showErrorMessage(view.getContext(), e.getLocalizedMessage());
                    });
            }else {
                progressDialog.dismiss();
            }
        }
    }

    private void updateProfile(boolean nameCheck,String uploadName,String uploadImageUrl){
        sharedPreferencesHelper.saveString("myMail", userMail);
        if(nameCheck){
            SharedPreferences.Editor editor = nameShared.edit();
            editor.putString("name",uploadName);
            editor.apply();

            myUserName = uploadName;

            TextView textView = mainActivity.headerView.findViewById(R.id.drawer_user_name);
            textView.setText(uploadName);

            binding.nameEdittext.setEnabled(false);
            binding.nameEdittext.setText("");
            binding.nameEdittext.setHint(uploadName);
            binding.nameTextInputLayout.setEndIconVisible(true);
        }

        SharedPreferences.Editor editor = imageUrlShared.edit();
        editor.putString("imageUrl",uploadImageUrl);
        editor.apply();

        ImageView imageView = mainActivity.headerView.findViewById(R.id.drawer_image);
        Glide.with(requireActivity())
            .load(uploadImageUrl)
            .apply(new RequestOptions()
            .error(R.drawable.person_active_96)
            .centerCrop())
            .into(imageView);

        imageData = null;
    }

    private void updateProfile(boolean nameCheck,String uploadName){
        sharedPreferencesHelper.saveString("myMail", userMail);
        if(nameCheck){
            SharedPreferences.Editor editor = nameShared.edit();
            editor.putString("name",uploadName);
            editor.apply();

            myUserName = uploadName;

            TextView textView = mainActivity.headerView.findViewById(R.id.drawer_user_name);
            textView.setText(uploadName);

            binding.nameEdittext.setEnabled(false);
            binding.nameEdittext.setText("");
            binding.nameEdittext.setHint(uploadName);
            binding.nameTextInputLayout.setEndIconVisible(true);
        }
    }

    private void setImage(View view) {
        String[] permissions;
        String rationaleMessage;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        }else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        rationaleMessage = getString(R.string.galeriye_gitmek_i_in_izin_gerekli);

        if (ContextCompat.checkSelfPermission(view.getContext(), permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permissions[0])) {
                Snackbar.make(view, rationaleMessage, Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.izin_ver), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionLauncher.launch(permissions[0]);
                    }
                }).show();
            } else {
                permissionLauncher.launch(permissions[0]);
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intent);
        }
    }

    private void registerLauncher(View view){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent intentForResult = result.getData();
                    if(intentForResult != null){
                        imageData = intentForResult.getData();
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            try {
                                ImageDecoder.Source source = ImageDecoder.createSource(view.getContext().getContentResolver(),imageData);
                                selectedBitmap = ImageDecoder.decodeBitmap(source);
                                binding.profileImage.setImageBitmap(selectedBitmap);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }else {
                            try {
                                InputStream inputStream = view.getContext().getContentResolver().openInputStream(imageData);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                binding.profileImage.setImageBitmap(bitmap);
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
            }
        });
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intent);
                }else{
                    showToastShort(getString(R.string.izinleri_aktif_etmeniz_gerekiyor));
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
        });
    }

    private void setProfile(View view){

        binding.mailEdittext.setHint(userMail);

        String name = nameShared.getString("name","");
        String imageUrl = imageUrlShared.getString("imageUrl","");

        if(!name.isEmpty()){
            myUserName = name;
            binding.nameEdittext.setHint(name);
            binding.nameEdittext.setEnabled(false);
        }else {
            binding.nameTextInputLayout.setEndIconVisible(false);
        }
        if(!imageUrl.isEmpty()){
            myImageUrl = imageUrl;

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
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    public void showToastShort(String message){
        Toast.makeText(requireActivity().getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }
    public void showToastLong(String message){
        Toast.makeText(requireActivity().getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }
    private void showErrorMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}