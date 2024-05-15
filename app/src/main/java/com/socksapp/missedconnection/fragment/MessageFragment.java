package com.socksapp.missedconnection.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.RecentConversationsAdapter;
import com.socksapp.missedconnection.databinding.FragmentMessageBinding;
import com.socksapp.missedconnection.model.ChatMessage;
import com.socksapp.missedconnection.myclass.User;
import com.socksapp.missedconnection.myinterface.ConversionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MessageFragment extends Fragment implements ConversionListener{

    private FragmentMessageBinding binding;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private MainActivity mainActivity;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private String myMail;
    private ListenerRegistration listenerRegistration;

    public MessageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseFirestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
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

        myMail = user.getEmail();

        conversations.clear();

        binding.recyclerViewMessage.setLayoutManager(new LinearLayoutManager(view.getContext()));
        conversationsAdapter = new RecentConversationsAdapter(conversations,this,MessageFragment.this,view.getContext());
        binding.recyclerViewMessage.setAdapter(conversationsAdapter);

        listenConversations();

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
        firebaseFirestore.collection("conversations")
            .whereEqualTo("senderId",myMail)
            .addSnapshotListener(eventListener);
        firebaseFirestore.collection("conversations")
            .whereEqualTo("receiverId",myMail)
            .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null)
        {
            return;
        }
        if(value != null){
            for(DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderId = documentChange.getDocument().getString("senderId");
                    String receiverId = documentChange.getDocument().getString("receiverId");
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    chatMessage.message = documentChange.getDocument().getString("lastMessage");
                    chatMessage.dateObject = documentChange.getDocument().getDate("date");
                    if(myMail.equals(senderId)){
                        String id = documentChange.getDocument().getString("receiverId");
                        getUsersDetails(id,chatMessage);
//                        chatMessage.conversionId = documentChange.getDocument().getString("receiverId");
//                        chatMessage.conversionName = documentChange.getDocument().getString("receiverName");
//                        chatMessage.conversionImage = documentChange.getDocument().getString("receiverImage");
                    }else {
                        String id = documentChange.getDocument().getString("senderId");
                        getUsersDetails(id,chatMessage);
//                        chatMessage.conversionId = documentChange.getDocument().getString("senderId");
//                        chatMessage.conversionName = documentChange.getDocument().getString("senderName");
//                        chatMessage.conversionImage = documentChange.getDocument().getString("senderImage");
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
                }
            }
            Collections.sort(conversations, (obj1, obj2)-> obj2.dateObject.compareTo(obj1.dateObject));
            binding.recyclerViewMessage.smoothScrollToPosition(0);
            binding.recyclerViewMessage.setVisibility(View.VISIBLE);
            conversationsAdapter.notifyDataSetChanged();
        }
    };


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
        builder.setMessage("Mesajı silmek istiyor musunuz?");
        builder.setPositiveButton("Sil", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteConversations(userMail,"senderId","receiverId");
                deleteConversations(userMail,"receiverId","senderId");
                deleteChats(userMail,position);
            }
        });
        builder.setNegativeButton("Geri", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();
    }

    public void deleteConversations(String userMail, String senderId, String receiverId){
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        CollectionReference collectionReference = firebaseFirestore.collection("conversations");
        collectionReference.whereEqualTo(senderId, userMail).whereEqualTo(receiverId,myMail).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    document.getReference().delete();
                }
            }
        });
    }

    public void deleteChats(String userMail, int position){

        firebaseFirestore.collection("chatsId").document(myMail).get().addOnSuccessListener(documentSnapshot -> {
            if(documentSnapshot.exists()){
                Map<String,Object> data = documentSnapshot.getData();
                if(data != null){
                    if(!data.isEmpty()){
                        if(data.containsKey(userMail)){
                            String id = (String) data.get(userMail);
                            if(id != null && !id.isEmpty()){
                                deleteSubCollections(id,position);
                            }
                        }
                    }
                }
            }
        }).addOnFailureListener(e -> {

        });
    }
    public void deleteSubCollections(String documentId, int position) {
        WriteBatch batch = firebaseFirestore.batch();
        CollectionReference subCollectionReference = firebaseFirestore.collection("chats").document(documentId).collection(documentId);
        subCollectionReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    batch.delete(document.getReference());
                }

                DocumentReference documentReference = firebaseFirestore.collection("chats").document(documentId);
                batch.delete(documentReference);

                batch.commit().addOnCompleteListener(subTask -> {
                    if (subTask.isSuccessful()) {
                        conversationsAdapter.notifyItemRemoved(position);
                        conversations.remove(position);
                        conversationsAdapter.notifyDataSetChanged();
                    }else {

                    }
                });
            } else {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.buttonDrawerToggle.setImageResource(R.drawable.icon_menu);
        mainActivity.bottomNavigationView.setVisibility(View.VISIBLE);
        mainActivity.includedLayout.setVisibility(View.VISIBLE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
//        startListeningToChanges();
    }

    @Override
    public void onPause() {
        super.onPause();
//        if (listenerRegistration != null) {
//            listenerRegistration.remove();
//        }
    }

//    public void startListeningToChanges() {
//        listenerRegistration = firebaseFirestore.collection("chatsId").document(myMail).addSnapshotListener(MetadataChanges.INCLUDE, new EventListener<DocumentSnapshot>() {
//            @Override
//            public void onEvent(@Nullable DocumentSnapshot snapshot,
//                                @Nullable FirebaseFirestoreException e) {
//                if (e != null) {
//                    return;
//                }
//
//                if (snapshot != null && snapshot.exists()) {
//                    Map<String, Object> data = snapshot.getData();
//                    if (data != null) {
//                        for (Map.Entry<String, Object> entry : data.entrySet()) {
//                            String mail = entry.getKey();
//                            String id = (String) entry.getValue();
//                            ContentValues values = new ContentValues();
//                            values.put("mail", mail);
//                            values.put("id", id);
//                            mainActivity.chatIdDataAccess.addChatsId(mail,id);
//                        }
//                    }
//                }
//            }
//        });
//    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }
}