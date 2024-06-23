package com.socksapp.missedconnection.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.socksapp.missedconnection.databinding.RecycleViewChatTextMeBinding;
import com.socksapp.missedconnection.databinding.RecycleViewChatTextYouBinding;
import com.socksapp.missedconnection.databinding.RecyclerViewDateTitleBinding;
import com.socksapp.missedconnection.model.ChatMessage;
import com.socksapp.missedconnection.myclass.SharedPreferencesGetLanguage;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private final List<ChatMessage> chatMessages;
    private final String senderId;
    private final Context context;
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    private SharedPreferencesGetLanguage sharedPreferencesGetLanguage;

    public ChatAdapter(List<ChatMessage> chatMessages, String senderId, Context context) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SENT){
            return new SentMessageViewHolder(
                    RecycleViewChatTextMeBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    ),this
            );
        } else if (viewType == VIEW_TYPE_RECEIVED){
            return new ReceiverMessageViewHolder(
                    RecycleViewChatTextYouBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    ),this
            );
        }else {
            return new DateTitleViewHolder(
                    RecyclerViewDateTitleBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    ),this
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        sharedPreferencesGetLanguage = new SharedPreferencesGetLanguage(context);
        String language = sharedPreferencesGetLanguage.getString("language","");
        if (getItemViewType(position) == VIEW_TYPE_SENT){
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position),position,language);
        }else if (getItemViewType(position) == VIEW_TYPE_RECEIVED){
            ((ReceiverMessageViewHolder) holder).setData(chatMessages.get(position),position,language);
        }else {
            ((DateTitleViewHolder) holder).setData(chatMessages.get(position),position,language);
        }
    }

    public ChatMessage getItem(int position) {
        return chatMessages.get(position);
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).viewType == 3){
            return 3;
        }else {
            if (chatMessages.get(position).senderId.equals(senderId)){
                return VIEW_TYPE_SENT;
            }else {
                return VIEW_TYPE_RECEIVED;
            }
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder{
        private final RecycleViewChatTextMeBinding binding;
        private final ChatAdapter adapter;
        SentMessageViewHolder(RecycleViewChatTextMeBinding itemContainerSentMessageBinding, ChatAdapter adapter){
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
            this.adapter = adapter;
        }
        void setData(ChatMessage chatMessage,int position,String language){
            binding.messageText.setText(chatMessage.message);
            binding.timeChatText.setText(chatMessage.dateTime);

            SimpleDateFormat dateFormat;
            SimpleDateFormat dayOfWeekFormat;

            if (language.equals("turkish")) {
                dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
                dayOfWeekFormat = new SimpleDateFormat("EEEE", new Locale("tr"));
            } else {
                dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("en"));
                dayOfWeekFormat = new SimpleDateFormat("EEEE", new Locale("en"));
            }

            LocalDate now = LocalDate.now();

            LocalDate messageDate = chatMessage.dateObject.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            String displayDate;
            if (messageDate.isEqual(now)) {
                if (language.equals("turkish")) {
                    displayDate = "Bugün";
                } else {
                    displayDate = "Today";
                }
            } else if (messageDate.isEqual(now.minusDays(1))) {
                if (language.equals("turkish")) {
                    displayDate = "Dün";
                } else {
                    displayDate = "Yesterday";
                }
            } else if (messageDate.isAfter(now.minusDays(6))) { // if within the last 6 days
                displayDate = dayOfWeekFormat.format(chatMessage.dateObject);
            } else {
                displayDate = dateFormat.format(chatMessage.dateObject);
            }

            if (position > 0) {
                ChatMessage previousMessage = adapter.getItem(position - 1);
                LocalDate previousMessageDate = previousMessage.dateObject.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                if (messageDate.isEqual(previousMessageDate)) {
                    binding.linearDateTitle.setVisibility(View.GONE);
                } else {
                    binding.linearDateTitle.setVisibility(View.VISIBLE);
                    binding.dateTitle.setText(displayDate);
                }
            } else {
                binding.linearDateTitle.setVisibility(View.GONE);
            }

//            if(position > 0){
//                ChatMessage previousMessage = adapter.getItem(position - 1);
//                ConstraintLayout constraintLayout = binding.senderBaseConstraintLayout;
//                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) constraintLayout.getLayoutParams();
//                if (!chatMessage.senderId.equals(previousMessage.senderId)) {
//                    layoutParams.topMargin = 20;
//                }else {
//                    layoutParams.topMargin = 0;
//                }
//                constraintLayout.setLayoutParams(layoutParams);
//            }else {
//                ConstraintLayout constraintLayout = binding.senderBaseConstraintLayout;
//                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) constraintLayout.getLayoutParams();
//                layoutParams.topMargin = 0;
//                constraintLayout.setLayoutParams(layoutParams);
//            }
        }
    }

    static class ReceiverMessageViewHolder extends RecyclerView.ViewHolder{
        private final RecycleViewChatTextYouBinding binding;
        private final ChatAdapter adapter;
        ReceiverMessageViewHolder(RecycleViewChatTextYouBinding itemContainerReceivedMessageBinding, ChatAdapter adapter){
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
            this.adapter = adapter;
        }
        void setData(ChatMessage chatMessage,int position,String language){
            binding.messageText.setText(chatMessage.message);
            binding.timeChatText.setText(chatMessage.dateTime);

            SimpleDateFormat dateFormat;
            SimpleDateFormat dayOfWeekFormat;

            if (language.equals("turkish")) {
                dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
                dayOfWeekFormat = new SimpleDateFormat("EEEE", new Locale("tr"));
            } else {
                dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("en"));
                dayOfWeekFormat = new SimpleDateFormat("EEEE", new Locale("en"));
            }

            LocalDate now = LocalDate.now();

            LocalDate messageDate = chatMessage.dateObject.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            String displayDate;
            if (messageDate.isEqual(now)) {
                if (language.equals("turkish")) {
                    displayDate = "Bugün";
                } else {
                    displayDate = "Today";
                }
            } else if (messageDate.isEqual(now.minusDays(1))) {
                if (language.equals("turkish")) {
                    displayDate = "Dün";
                } else {
                    displayDate = "Yesterday";
                }
            } else if (messageDate.isAfter(now.minusDays(6))) { // if within the last 6 days
                displayDate = dayOfWeekFormat.format(chatMessage.dateObject);
            } else {
                displayDate = dateFormat.format(chatMessage.dateObject);
            }

            if (position > 0) {
                ChatMessage previousMessage = adapter.getItem(position - 1);
                LocalDate previousMessageDate = previousMessage.dateObject.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                if (messageDate.isEqual(previousMessageDate)) {
                    binding.linearDateTitle.setVisibility(View.GONE);
                } else {
                    binding.linearDateTitle.setVisibility(View.VISIBLE);
                    binding.dateTitle.setText(displayDate);
                }
            } else {
                binding.linearDateTitle.setVisibility(View.GONE);
            }

//            if(position > 0){
//                ChatMessage previousMessage = adapter.getItem(position - 1);
//                ConstraintLayout constraintLayout = binding.receiverBaseConstraintLayout;
//                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) constraintLayout.getLayoutParams();
//                if (!chatMessage.senderId.equals(previousMessage.senderId)) {
//                    layoutParams.topMargin = 20;
//                }else {
//                    layoutParams.topMargin = 0;
//                }
//                constraintLayout.setLayoutParams(layoutParams);
//            }else {
//                ConstraintLayout constraintLayout = binding.receiverBaseConstraintLayout;
//                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) constraintLayout.getLayoutParams();
//                layoutParams.topMargin = 0;
//                constraintLayout.setLayoutParams(layoutParams);
//            }
        }
    }

    static class DateTitleViewHolder extends RecyclerView.ViewHolder{
        private final RecyclerViewDateTitleBinding binding;
        private final ChatAdapter adapter;
        DateTitleViewHolder(RecyclerViewDateTitleBinding recyclerViewDateTitleBinding, ChatAdapter adapter){
            super(recyclerViewDateTitleBinding.getRoot());
            binding = recyclerViewDateTitleBinding;
            this.adapter = adapter;
        }
        void setData(ChatMessage chatMessage,int position,String language){
            SimpleDateFormat dateFormat;
            SimpleDateFormat dayOfWeekFormat;

            if (language.equals("turkish")) {
                dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
                dayOfWeekFormat = new SimpleDateFormat("EEEE", new Locale("tr"));
            } else {
                dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("en"));
                dayOfWeekFormat = new SimpleDateFormat("EEEE", new Locale("en"));
            }

            LocalDate now = LocalDate.now();

            LocalDate messageDate = chatMessage.dateObject.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            String displayDate;
            if (messageDate.isEqual(now)) {
                if (language.equals("turkish")) {
                    displayDate = "Bugün";
                } else {
                    displayDate = "Today";
                }
            } else if (messageDate.isEqual(now.minusDays(1))) {
                if (language.equals("turkish")) {
                    displayDate = "Dün";
                } else {
                    displayDate = "Yesterday";
                }
            } else if (messageDate.isAfter(now.minusDays(6))) { // if within the last 6 days
                displayDate = dayOfWeekFormat.format(chatMessage.dateObject);
            } else {
                displayDate = dateFormat.format(chatMessage.dateObject);
            }
//            String title = dateFormat.format(chatMessage.dateObject);
            binding.dateTitle.setText(displayDate);
        }
    }

}
