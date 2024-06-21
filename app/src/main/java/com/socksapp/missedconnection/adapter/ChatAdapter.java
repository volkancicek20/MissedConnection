package com.socksapp.missedconnection.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.socksapp.missedconnection.databinding.RecycleViewChatTextMeBinding;
import com.socksapp.missedconnection.databinding.RecycleViewChatTextYouBinding;
import com.socksapp.missedconnection.databinding.RecyclerViewDateTitleBinding;
import com.socksapp.missedconnection.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private final List<ChatMessage> chatMessages;
    private final String senderId;
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public ChatAdapter(List<ChatMessage> chatMessages, String senderId) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
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
        } else{
            return new ReceiverMessageViewHolder(
                    RecycleViewChatTextYouBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    ),this
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT){
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position),position);
        }else{
            ((ReceiverMessageViewHolder) holder).setData(chatMessages.get(position),position);
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
        if (chatMessages.get(position).senderId.equals(senderId)){
            return VIEW_TYPE_SENT;
        }else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder{
        private final RecycleViewChatTextMeBinding binding;
        private final ChatAdapter adapter;
        private final SimpleDateFormat dateFormat;
        SentMessageViewHolder(RecycleViewChatTextMeBinding itemContainerSentMessageBinding, ChatAdapter adapter){
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
            this.adapter = adapter;
            this.dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        }
        void setData(ChatMessage chatMessage,int position){
            binding.messageText.setText(chatMessage.message);
            binding.timeChatText.setText(chatMessage.dateTime);

            if (position > 0) {
                ChatMessage previousMessage = adapter.getItem(position - 1);
                String currentDate = dateFormat.format(chatMessage.dateObject);
                String previousDate = dateFormat.format(previousMessage.dateObject);

                if (currentDate.equals(previousDate)) {
                    binding.linearDateTitle.setVisibility(View.GONE);
                } else {
                    binding.linearDateTitle.setVisibility(View.VISIBLE);
                    binding.dateTitle.setText(currentDate);
                }
            } else {
                String currentDate = dateFormat.format(chatMessage.dateObject);
                binding.linearDateTitle.setVisibility(View.VISIBLE);
                binding.dateTitle.setText(currentDate);
            }
        }

    }
    static class ReceiverMessageViewHolder extends RecyclerView.ViewHolder{
        private final RecycleViewChatTextYouBinding binding;
        private final ChatAdapter adapter;
        private final SimpleDateFormat dateFormat;
        ReceiverMessageViewHolder(RecycleViewChatTextYouBinding itemContainerReceivedMessageBinding, ChatAdapter adapter){
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
            this.adapter = adapter;
            this.dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        }
        void setData(ChatMessage chatMessage,int position){
            binding.messageText.setText(chatMessage.message);
            binding.timeChatText.setText(chatMessage.dateTime);

            if (position > 0) {
                ChatMessage previousMessage = adapter.getItem(position - 1);
                String currentDate = dateFormat.format(chatMessage.dateObject);
                String previousDate = dateFormat.format(previousMessage.dateObject);

                if (currentDate.equals(previousDate)) {
                    binding.linearDateTitle.setVisibility(View.GONE);
                } else {
                    binding.linearDateTitle.setVisibility(View.VISIBLE);
                    binding.dateTitle.setText(currentDate);
                }
            } else {
                String currentDate = dateFormat.format(chatMessage.dateObject);
                binding.linearDateTitle.setVisibility(View.VISIBLE);
                binding.dateTitle.setText(currentDate);
            }
        }

    }

}
