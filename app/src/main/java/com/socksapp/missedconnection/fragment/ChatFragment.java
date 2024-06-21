package com.socksapp.missedconnection.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.socksapp.missedconnection.FCM.FCMNotificationSender;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.activity.MainActivity;
import com.socksapp.missedconnection.adapter.ChatAdapter;
import com.socksapp.missedconnection.databinding.FragmentChatBinding;
import com.socksapp.missedconnection.model.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firebaseFirestore;
    private ChatAdapter chatAdapter;
    private SharedPreferences nameShared,imageUrlShared,language;
    private String myUserName,myImageUrl,myMail,anotherMail;
    private String conversionId = null;
    private List<ChatMessage> chatMessages;
    private ListenerRegistration collectionListener; // ilerde chat ile anlık silme eklersen bunu kullan veya uyarı
    private MainActivity mainActivity;
    private DocumentSnapshot lastVisibleMessage;
    private final int pageSize = 15;
    private boolean checkLastMessage;
    private boolean checkDateTitle;
    private int checkTitleCount;
    private String lastDate;
    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();
        chatMessages = new ArrayList<>();

        language = requireActivity().getSharedPreferences("Language",Context.MODE_PRIVATE);
        nameShared = requireActivity().getSharedPreferences("Name",Context.MODE_PRIVATE);
        imageUrlShared = requireActivity().getSharedPreferences("ImageUrl",Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater,container,false);
        Bundle args = getArguments();
        if (args != null) {
            anotherMail = args.getString("anotherMail");
            getUsers(anotherMail);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        myMail = user.getEmail();
        myUserName = nameShared.getString("name","");
        myImageUrl = imageUrlShared.getString("imageUrl","");

        mainActivity.bottomNavigationView.setVisibility(View.GONE);
        mainActivity.includedLayout.setVisibility(View.GONE);
        mainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        chatAdapter = new ChatAdapter(chatMessages,myMail);
        binding.recyclerViewChat.setAdapter(chatAdapter);

        lastVisibleMessage = null;
        checkLastMessage = true;
        checkDateTitle = true;
        checkTitleCount = 0;
        lastDate = "";

        listenMessages();

        binding.layoutSend.setOnClickListener(sendMessageClickListener);
        binding.backAndImageLinearLayout.setOnClickListener(backAndImageLinearLayoutClickListener);

        binding.recyclerViewChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy < 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                        loadMoreMessages();
                    }
                }
            }
        });

//        binding.recyclerViewChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
//                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
//
//                if (firstVisibleItemPosition != RecyclerView.NO_POSITION) {
//                    ChatMessage topVisibleMessage = chatAdapter.getItem(firstVisibleItemPosition);
//                    String getLanguage = language.getString("language","");
//                    if(getLanguage.equals("turkish")){
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
//                        String formattedDate = dateFormat.format(topVisibleMessage.dateObject);
//                        Toast.makeText(requireContext(),formattedDate,Toast.LENGTH_SHORT).show();
//                    }else {
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("en"));
//                        String formattedDate = dateFormat.format(topVisibleMessage.dateObject);
//                        Toast.makeText(requireContext(),formattedDate,Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//        });


        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToMessageFragment();
            }
        });
    }

    private void goToMessageFragment(){

        mainActivity.bottomNavigationView.setSelectedItemId(R.id.navMessage);

        MessageFragment fragment = new MessageFragment();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public View.OnClickListener sendMessageClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String message = binding.inputMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message,view);
                binding.inputMessage.setText("");
            }
        }
    };

    public View.OnClickListener backAndImageLinearLayoutClickListener = view -> goToMessageFragment();

    private String generateAlphanumericUUID() {
        String uuid = UUID.randomUUID().toString();
        uuid = uuid.replaceAll("-", "");
        return uuid;
    }

    private void sendMessage(String msg,View view){
        firebaseFirestore.collection("chatsId").document(myMail).get().addOnSuccessListener(documentSnapshot -> {
            if(documentSnapshot.exists()){
                Map<String,Object> data = documentSnapshot.getData();
                if(data != null){
                    if(!data.isEmpty()){
                        if(data.containsKey(anotherMail)){
                            String id = (String) data.get(anotherMail);
                            if(id != null && !id.isEmpty()){
                                WriteBatch batch = firebaseFirestore.batch();

                                String messageId = firebaseFirestore.collection("chats").document(id).collection(id).document().getId();

                                HashMap<String, Object> message = new HashMap<>();
                                message.put("senderId", myMail);
                                message.put("receiverId", anotherMail);
                                message.put("message", msg);
                                message.put("date", new Date());

                                batch.set(firebaseFirestore.collection("chats").document(id).collection(id).document(messageId), message);
                                DocumentReference conversionRef = firebaseFirestore.collection("conversations").document();
                                if (conversionId != null) {
                                    DocumentReference conversationRef = firebaseFirestore.collection("conversations").document(conversionId);
                                    batch.update(conversationRef, "lastMessage", msg, "date", new Date());
                                } else {
                                    HashMap<String, Object> conversion = new HashMap<>();
                                    conversion.put("senderId", myMail);
                                    conversion.put("receiverId", anotherMail);
                                    conversion.put("lastMessage", msg);
                                    conversion.put("date", new Date());
                                    batch.set(conversionRef, conversion);
                                }

                                batch.commit().addOnSuccessListener(aVoid -> {
                                    firebaseFirestore.collection("users").document(anotherMail).get().addOnSuccessListener(documentSnapshot1 -> {
                                        if(documentSnapshot1.exists()){
                                            String token = documentSnapshot1.getString("fcmToken");
                                            String name;
                                            name = documentSnapshot1.getString("name");
                                            if(name == null) name = "";
                                            FCMNotificationSender fcmNotificationSender = new FCMNotificationSender(token,name,msg,view.getContext());
                                            fcmNotificationSender.SendNotification();
                                        }
                                    });
                                    binding.inputMessage.setText("");
                                });


                            }
                        }else {
                            String auto_id = generateAlphanumericUUID();
                            Map<String, Object> dataId = new HashMap<>();
                            dataId.put(anotherMail,auto_id);
                            Map<String, Object> dataId2 = new HashMap<>();
                            dataId2.put(myMail,auto_id);

                            WriteBatch batch = firebaseFirestore.batch();

                            DocumentReference docRef1 = firebaseFirestore.collection("chatsId").document(myMail);
                            batch.set(docRef1, dataId, SetOptions.merge());

                            DocumentReference docRef2 = firebaseFirestore.collection("chatsId").document(anotherMail);
                            batch.set(docRef2, dataId2, SetOptions.merge());

//                            mainActivity.chatIdDataAccess.addChatsId(anotherMail,auto_id);

                            String messageId = firebaseFirestore.collection("chats").document(auto_id).collection(auto_id).document().getId();

                            HashMap<String, Object> message = new HashMap<>();
                            message.put("senderId", myMail);
                            message.put("receiverId", anotherMail);
                            message.put("message", msg);
                            message.put("date", new Date());

                            batch.set(firebaseFirestore.collection("chats").document(auto_id).collection(auto_id).document(messageId), message);
                            DocumentReference conversionRef = firebaseFirestore.collection("conversations").document();
                            if (conversionId != null) {
                                DocumentReference conversationRef = firebaseFirestore.collection("conversations").document(conversionId);
                                batch.update(conversationRef, "lastMessage", msg, "date", new Date());
                            } else {
                                HashMap<String, Object> conversion = new HashMap<>();
                                conversion.put("senderId", myMail);
                                conversion.put("receiverId", anotherMail);
                                conversion.put("lastMessage", msg);
                                conversion.put("date", new Date());
                                batch.set(conversionRef, conversion);
                            }

                            batch.commit().addOnSuccessListener(aVoid -> {
                                firebaseFirestore.collection("users").document(anotherMail).get().addOnSuccessListener(documentSnapshot1 -> {
                                    if(documentSnapshot1.exists()){
                                        String token = documentSnapshot1.getString("fcmToken");
                                        String name;
                                        name = documentSnapshot1.getString("name");
                                        if(name == null) name = "";
                                        FCMNotificationSender fcmNotificationSender = new FCMNotificationSender(token,name,msg,view.getContext());
                                        fcmNotificationSender.SendNotification();
                                    }
                                });
                                binding.inputMessage.setText("");
                            });
                        }
                    }else {
                        String auto_id = generateAlphanumericUUID();
                        Map<String, Object> dataId = new HashMap<>();
                        dataId.put(anotherMail,auto_id);
                        Map<String, Object> dataId2 = new HashMap<>();
                        dataId2.put(myMail,auto_id);

                        WriteBatch batch = firebaseFirestore.batch();

                        DocumentReference docRef1 = firebaseFirestore.collection("chatsId").document(myMail);
                        batch.set(docRef1, dataId, SetOptions.merge());

                        DocumentReference docRef2 = firebaseFirestore.collection("chatsId").document(anotherMail);
                        batch.set(docRef2, dataId2, SetOptions.merge());

//                        mainActivity.chatIdDataAccess.addChatsId(anotherMail,auto_id);

                        String messageId = firebaseFirestore.collection("chats").document(auto_id).collection(auto_id).document().getId();

                        HashMap<String, Object> message = new HashMap<>();
                        message.put("senderId", myMail);
                        message.put("receiverId", anotherMail);
                        message.put("message", msg);
                        message.put("date", new Date());

                        batch.set(firebaseFirestore.collection("chats").document(auto_id).collection(auto_id).document(messageId), message);
                        DocumentReference conversionRef = firebaseFirestore.collection("conversations").document();
                        if (conversionId != null) {
                            DocumentReference conversationRef = firebaseFirestore.collection("conversations").document(conversionId);
                            batch.update(conversationRef, "lastMessage", msg, "date", new Date());
                        } else {
                            HashMap<String, Object> conversion = new HashMap<>();
                            conversion.put("senderId", myMail);
                            conversion.put("receiverId", anotherMail);
                            conversion.put("lastMessage", msg);
                            conversion.put("date", new Date());
                            batch.set(conversionRef, conversion);
                        }

                        batch.commit().addOnSuccessListener(aVoid -> {
                            firebaseFirestore.collection("users").document(anotherMail).get().addOnSuccessListener(documentSnapshot1 -> {
                                if(documentSnapshot1.exists()){
                                    String token = documentSnapshot1.getString("fcmToken");
                                    String name;
                                    name = documentSnapshot1.getString("name");
                                    if(name == null) name = "";
                                    FCMNotificationSender fcmNotificationSender = new FCMNotificationSender(token,name,msg,view.getContext());
                                    fcmNotificationSender.SendNotification();
                                }
                            });
                            refreshFragment();
                            binding.inputMessage.setText("");
                        });
                    }
                }
            }else {
                String auto_id = generateAlphanumericUUID();
                Map<String, Object> dataId = new HashMap<>();
                dataId.put(anotherMail,auto_id);
                Map<String, Object> dataId2 = new HashMap<>();
                dataId2.put(myMail,auto_id);

                WriteBatch batch = firebaseFirestore.batch();

                DocumentReference docRef1 = firebaseFirestore.collection("chatsId").document(myMail);
                batch.set(docRef1, dataId, SetOptions.merge());

                DocumentReference docRef2 = firebaseFirestore.collection("chatsId").document(anotherMail);
                batch.set(docRef2, dataId2, SetOptions.merge());

//                mainActivity.chatIdDataAccess.addChatsId(anotherMail,auto_id);

                String messageId = firebaseFirestore.collection("chats").document(auto_id).collection(auto_id).document().getId();

                HashMap<String, Object> message = new HashMap<>();
                message.put("senderId", myMail);
                message.put("receiverId", anotherMail);
                message.put("message", msg);
                message.put("date", new Date());

                batch.set(firebaseFirestore.collection("chats").document(auto_id).collection(auto_id).document(messageId), message);
                DocumentReference conversionRef = firebaseFirestore.collection("conversations").document();
                if (conversionId != null) {
                    DocumentReference conversationRef = firebaseFirestore.collection("conversations").document(conversionId);
                    batch.update(conversationRef, "lastMessage", msg, "date", new Date());
                } else {
                    HashMap<String, Object> conversion = new HashMap<>();
                    conversion.put("senderId", myMail);
                    conversion.put("receiverId", anotherMail);
                    conversion.put("lastMessage", msg);
                    conversion.put("date", new Date());
                    batch.set(conversionRef, conversion);
                }

                batch.commit().addOnSuccessListener(aVoid -> {
                    firebaseFirestore.collection("users").document(anotherMail).get().addOnSuccessListener(documentSnapshot1 -> {
                        if(documentSnapshot1.exists()){
                            String token = documentSnapshot1.getString("fcmToken");
                            String name;
                            name = documentSnapshot1.getString("name");
                            if(name == null) name = "";
                            FCMNotificationSender fcmNotificationSender = new FCMNotificationSender(token,name,msg,view.getContext());
                            fcmNotificationSender.SendNotification();
                        }
                    });
                    refreshFragment();
                    binding.inputMessage.setText("");
                });
            }
        }).addOnFailureListener(e -> {

        });
    }

    public void refreshFragment() {
        Bundle args = new Bundle();
        args.putString("anotherMail", anotherMail);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView2,fragment);
//        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void listenMessages() {
        firebaseFirestore.collection("chatsId").document(myMail).get().addOnSuccessListener(documentSnapshot -> {
            if(documentSnapshot.exists()){
                Map<String , Object> data = documentSnapshot.getData();
                if(data != null && !data.isEmpty()){
                    String id = (String) data.get(anotherMail);
                    if(id != null && !id.isEmpty()){
                        firebaseFirestore.collection("chats")
                            .document(id).collection(id)
                            .orderBy("date", Query.Direction.DESCENDING)
                            .limit(pageSize)
                            .addSnapshotListener(eventListener);
                    }
                }
            }
        });
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null){
            return;
        }
        if (value != null && !value.isEmpty()){
            if(checkLastMessage){
                lastVisibleMessage = value.getDocuments().get(value.size() - 1);
                checkLastMessage = false;
            }
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString("senderId");
                    chatMessage.receiverId = documentChange.getDocument().getString("receiverId");
                    chatMessage.message = documentChange.getDocument().getString("message");
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate("date"));
                    chatMessage.dateObject = documentChange.getDocument().getDate("date");
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if(count == 0){
                chatAdapter.notifyDataSetChanged();
                binding.recyclerViewChat.smoothScrollToPosition(chatMessages.size());
            }else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.recyclerViewChat.smoothScrollToPosition(chatMessages.size());
            }
        }

        if(conversionId == null){
            checkForConversion();
        }
    };

    private void loadMoreMessages() {
        binding.progressBar.setVisibility(View.VISIBLE);
        firebaseFirestore.collection("chatsId").document(myMail).get().addOnSuccessListener(documentSnapshot -> {
            if(documentSnapshot.exists()){
                Map<String , Object> data = documentSnapshot.getData();
                if(data != null && !data.isEmpty()){
                    String id = (String) data.get(anotherMail);
                    if(id != null && !id.isEmpty()){
                        if (lastVisibleMessage != null) {
                            firebaseFirestore.collection("chats")
                                .document(id)
                                .collection(id)
                                .orderBy("date", Query.Direction.DESCENDING)
                                .startAfter(lastVisibleMessage)
                                .limit(pageSize)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    
                                    binding.progressBar.setVisibility(View.GONE);
                                    List<ChatMessage> newMessages = new ArrayList<>();

                                    for (QueryDocumentSnapshot documentSnapshot2 : queryDocumentSnapshots) {
                                        ChatMessage chatMessage = new ChatMessage();
                                        chatMessage.senderId = documentSnapshot2.getString("senderId");
                                        chatMessage.receiverId = documentSnapshot2.getString("receiverId");
                                        chatMessage.message = documentSnapshot2.getString("message");
                                        chatMessage.dateTime = getReadableDateTime(documentSnapshot2.getDate("date"));
                                        chatMessage.dateObject = documentSnapshot2.getDate("date");
                                        newMessages.add(chatMessage);
                                    }

                                    Collections.sort(newMessages, (obj2, obj1) -> obj2.dateObject.compareTo(obj1.dateObject));

                                    chatMessages.addAll(0, newMessages);
                                    chatAdapter.notifyItemRangeInserted(0, newMessages.size());

                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        lastVisibleMessage = queryDocumentSnapshots.getDocuments()
                                                .get(queryDocumentSnapshots.size() - 1);
                                    }

                                })
                                .addOnFailureListener(e -> {
                                    /* Hata yönetimi */
                                });
                        }
                    }
                }
            }
        }).addOnFailureListener(e -> {

        });
    }

    private String getReadableDateTime(Date date){
        if (date != null) {
            String getLanguage = language.getString("language","");
            if(getLanguage.equals("turkish")){
                return new SimpleDateFormat("hh:mm a", new Locale("tr")).format(date);
            }else {
                return new SimpleDateFormat("hh:mm a", new Locale("en")).format(date);
            }
        } else {
            return "";
        }
    }

    private void checkForConversion(){
        if(chatMessages.size() != 0){
            checkForConversionRemotely(
                    myMail,
                    anotherMail
            );
            checkForConversionRemotely(
                    anotherMail,
                    myMail
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId){
        firebaseFirestore.collection("conversations")
            .whereEqualTo("senderId",senderId)
            .whereEqualTo("receiverId",receiverId)
            .get()
            .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    public void getUsers(String mail) {
        firebaseFirestore.collection("users").document(mail)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    binding.chatHeaderName.setText(name);

                    String imageUrl = documentSnapshot.getString("imageUrl");
                    if(imageUrl == null || imageUrl.isEmpty()){
                        binding.chatHeaderImage.setImageResource(R.drawable.person_active_96);
                    }else {
                        Glide.with(this)
                            .load(imageUrl)
                            .apply(new RequestOptions()
                            .error(R.drawable.person_active_96)
                            .centerCrop())
                            .into(binding.chatHeaderImage);
                    }
                }
            });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

}