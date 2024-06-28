package com.socksapp.missedconnection.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.RecentConversationsAdapter;
import com.socksapp.missedconnection.databinding.FragmentMessageBinding;
import com.socksapp.missedconnection.model.ChatMessage;
import com.socksapp.missedconnection.myclass.User;
import com.socksapp.missedconnection.myinterface.ConversionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageFragment extends Fragment implements ConversionListener{

    private FragmentMessageBinding binding;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFunctions functions;
    private MainActivity mainActivity;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private String myMail;
    private ListenerRegistration senderListenerRegistration;
    private ListenerRegistration receiverListenerRegistration;
    private Menu menu;
    private MenuItem menuItem;

    public MessageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseFirestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        functions = FirebaseFunctions.getInstance();
        conversations = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessageBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);

        menu = mainActivity.navigationView.getMenu();
        menuItem = menu.findItem(R.id.nav_drawer_home);
        menuItem.setIcon(R.drawable.home_default_96);

        myMail = user.getEmail();

        conversations.clear();

        binding.recyclerViewMessage.setLayoutManager(new LinearLayoutManager(view.getContext()));
        conversationsAdapter = new RecentConversationsAdapter(conversations,this,MessageFragment.this,view.getContext());
        binding.recyclerViewMessage.setAdapter(conversationsAdapter);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToMainFragment(view);
            }
        });

    }

    private void goToMainFragment(View v){

        mainActivity.bottomNavigationView.setSelectedItemId(R.id.navHome);

        MainFragment fragment = new MainFragment();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void listenConversations(){
        senderListenerRegistration = firebaseFirestore.collection("conversations")
            .whereEqualTo("senderId", myMail)
            .addSnapshotListener(eventListener);

        receiverListenerRegistration = firebaseFirestore.collection("conversations")
            .whereEqualTo("receiverId", myMail)
            .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null)
        {
            return;
        }
        if(value != null){
            if(!value.isEmpty()){
                binding.text.setVisibility(View.GONE);
                binding.text2.setVisibility(View.GONE);
            }
            for(DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderId = documentChange.getDocument().getString("senderId");
                    String receiverId = documentChange.getDocument().getString("receiverId");
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.viewType = 1;
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    chatMessage.message = documentChange.getDocument().getString("lastMessage");
                    chatMessage.dateObject = documentChange.getDocument().getDate("date");
                    if(myMail.equals(senderId)){
                        String id = documentChange.getDocument().getString("receiverId");
                        getUsersDetails(id,chatMessage);
                    }else {
                        String id = documentChange.getDocument().getString("senderId");
                        getUsersDetails(id,chatMessage);
                    }
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for(int i = 0; i<conversations.size(); i++){
                        String senderId = documentChange.getDocument().getString("senderId");
                        String receiverId = documentChange.getDocument().getString("receiverId");
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                            conversations.get(i).message = documentChange.getDocument().getString("lastMessage");
                            conversations.get(i).dateObject = documentChange.getDocument().getDate("date");
                            break;
                        }
                    }
                }else if (documentChange.getType() == DocumentChange.Type.REMOVED){
                    for (int i = 0; i < conversations.size(); i++) {
                        String senderId = documentChange.getDocument().getString("senderId");
                        String receiverId = documentChange.getDocument().getString("receiverId");
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                            conversations.remove(i);
                            conversationsAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                }
            }

            Collections.sort(conversations, (obj1, obj2)-> obj2.dateObject.compareTo(obj1.dateObject));
            binding.recyclerViewMessage.smoothScrollToPosition(0);
            binding.recyclerViewMessage.setVisibility(View.VISIBLE);
            conversationsAdapter.notifyDataSetChanged();
        }
    };

    private void stopListeningConversations() {
        if (senderListenerRegistration != null) {
            senderListenerRegistration.remove();
            senderListenerRegistration = null;
        }
        if (receiverListenerRegistration != null) {
            receiverListenerRegistration.remove();
            receiverListenerRegistration = null;
        }
    }

    private void getUsersDetails(String mail,ChatMessage chatMessage){
        firebaseFirestore.collection("users").document(mail).get().addOnSuccessListener(documentSnapshot -> {
            if(documentSnapshot.exists()){
                String name = documentSnapshot.getString("name");
                String imageUrl = documentSnapshot.getString("imageUrl");
                chatMessage.conversionName = name;
                chatMessage.conversionImage = imageUrl;
                chatMessage.conversionId = mail;
                conversations.add(chatMessage);
                Collections.sort(conversations, (obj1, obj2)-> obj2.dateObject.compareTo(obj1.dateObject));
            }else {
                chatMessage.conversionName = "";
                chatMessage.conversionImage = "";
                chatMessage.conversionId = null;
                conversations.add(chatMessage);
                Collections.sort(conversations, (obj1, obj2)-> obj2.dateObject.compareTo(obj1.dateObject));
            }
            binding.recyclerViewMessage.smoothScrollToPosition(0);
            binding.recyclerViewMessage.setVisibility(View.VISIBLE);
            conversationsAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            chatMessage.conversionName = "";
            chatMessage.conversionImage = "";
            chatMessage.conversionId = null;
            conversations.add(chatMessage);
            Collections.sort(conversations, (obj1, obj2)-> obj2.dateObject.compareTo(obj1.dateObject));
            binding.recyclerViewMessage.smoothScrollToPosition(0);
            binding.recyclerViewMessage.setVisibility(View.VISIBLE);
            conversationsAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onConversionClicked(User user) {
        String mail = user.id;

        Bundle args = new Bundle();
        args.putString("anotherMail", mail);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void choiceItem(View view, String userMail, int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog_message_delete, null);
        builder.setView(dialogView);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);

        AlertDialog dialog = builder.create();
        dialog.show();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        deleteButton.setOnClickListener(v -> {
            deleteChatsAndConversationFunctions(userMail,position,v);
//            deleteConversation(userMail);
            dialog.dismiss();
        });
    }

    private Task<Map<String, Object>> deleteBothAndSubcollection(String userMail, String senderId, String receiverId, String mainCollectionPath, String documentId, String subcollectionId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userMail", userMail);
        data.put("senderId", senderId);
        data.put("receiverId", receiverId);
        data.put("mainCollectionPath", mainCollectionPath);
        data.put("documentId", documentId);
        data.put("subcollectionId", subcollectionId);

        return functions
            .getHttpsCallable("deleteBothAndSubcollection")
            .call(data)
            .continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                @Override
                public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                    return (Map<String, Object>) task.getResult().getData();
                }
            });
    }

    private void deleteChatsAndConversationFunctions(String userMail, int position,View view){
        firebaseFirestore.collection("chatsId").document(myMail).get().addOnSuccessListener(documentSnapshot -> {
            if(documentSnapshot.exists()){
                Map<String,Object> data = documentSnapshot.getData();
                if(data != null){
                    if(!data.isEmpty()){
                        if(data.containsKey(userMail)){
                            String id = (String) data.get(userMail);
                            if(id != null && !id.isEmpty()){
                                ProgressDialog progressDialog = new ProgressDialog(view.getContext());
                                progressDialog.setMessage("Mesaj siliniyor..");
                                progressDialog.show();
                                deleteBothAndSubcollection(userMail, "senderId", "receiverId", "chats", id, id)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            // Başarılı sonuçları işleyin
                                            Map<String, Object> result = task.getResult();
                                            Log.d("Function Success", "Result: " + result);
                                            progressDialog.dismiss();
                                        } else {
                                            progressDialog.dismiss();
                                            showSnackbar(view,getString(R.string.mesaj_silinirken_hata_olustu));
                                            // Hataları işleyin
//                                            Exception e = task.getException();
//                                            if (e instanceof FirebaseFunctionsException) {
//                                                FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
//                                                FirebaseFunctionsException.Code code = ffe.getCode();
//                                                Object details = ffe.getDetails();
//                                                Log.e("Function Error", "Code: " + code + ", Details: " + details, e);
//                                            } else {
//                                                Log.e("Function Error", "Error: ", e);
//                                            }
                                        }
                                    }).addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        showSnackbar(view,getString(R.string.mesaj_silinirken_hata_olustu));
                                    });
                            }
                        }
                    }
                }
            }
        }).addOnFailureListener(e -> {

        });
    }

//    private void deleteConversation(String userMail){
//        Map<String, Object> data = new HashMap<>();
//        data.put("userMail", userMail);
//        data.put("senderId", "senderId");
//        data.put("receiverId", "receiverId");
//
//        functions.getHttpsCallable("deleteBothConversations").call(data)
//            .continueWith(new Continuation<HttpsCallableResult, Object>() {
//                @Override
//                public Object then(@NonNull Task<HttpsCallableResult> task) throws Exception {
//                    return (Map<String, Object>) task.getResult().getData();
//                }
//            });
//    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
        mainActivity.includedLayout.setVisibility(View.VISIBLE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        listenConversations();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopListeningConversations();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    private void showSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);

        snackbar.setBackgroundTint(Color.rgb(48, 44, 44));

        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);

        snackbar.show();
    }
}