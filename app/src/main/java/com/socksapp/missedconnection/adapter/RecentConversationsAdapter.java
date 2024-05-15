package com.socksapp.missedconnection.adapter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.RecycleViewMessageBinding;
import com.socksapp.missedconnection.fragment.MessageFragment;
import com.socksapp.missedconnection.model.ChatMessage;
import com.socksapp.missedconnection.myclass.User;
import com.socksapp.missedconnection.myinterface.ConversionListener;
import com.squareup.picasso.Picasso;

import java.util.List;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder> {
    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;
    private final MessageFragment fragment;
    private Context context;
    public RecentConversationsAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener, MessageFragment fragment,Context context) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
        this.fragment = fragment;
        this.context = context;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
            RecycleViewMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
            )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        holder.binding.recyclerViewProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                getShow(holder.itemView,chatMessages.get(pos).conversionImage);
            }
        });
        holder.binding.messageConstraintLayout.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if(pos != RecyclerView.NO_POSITION){
                String selectedItem = chatMessages.get(pos).conversionId;
                if(fragment != null){
                    fragment.choiceItem(holder.itemView,selectedItem,pos);
                }
            }
            return true;
        });

    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public class ConversionViewHolder extends RecyclerView.ViewHolder {
        RecycleViewMessageBinding binding;
        ConversionViewHolder(RecycleViewMessageBinding itemContainerRecentConversionBinding){
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }
        void setData(ChatMessage chatMessage){
            if(chatMessage.conversionImage == null || chatMessage.conversionImage.isEmpty()){
                binding.recyclerViewProfilePhoto.setImageResource(R.drawable.person_active_96);
            }else {
                Glide.with(context)
                    .load(chatMessage.conversionImage)
                    .apply(new RequestOptions()
                    .error(R.drawable.person_active_96)
                    .centerCrop())
                    .into(binding.recyclerViewProfilePhoto);
            }
            binding.recyclerViewName.setText(chatMessage.conversionName);
            binding.recyclerViewMessage.setText(chatMessage.message);
            binding.getRoot().setOnClickListener(v-> {
                User user = new User();
                user.id = chatMessage.conversionId;
                user.name = chatMessage.conversionName;
                user.image = chatMessage.conversionImage;
                conversionListener.onConversionClicked(user);
            });
        }
    }
    public void getShow(View view,String imageUrl){
//        LayoutInflater inflater = LayoutInflater.from(view.getContext());
//        View popupView = inflater.inflate(R.layout.popup_image, null);
//        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
//
//        popupWindow.setOutsideTouchable(true);
//        popupWindow.setFocusable(true);
//        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//
//        ImageView imageView = popupView.findViewById(R.id.popup_circle_image);
//        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("image")) {
//            Picasso.get().load(imageUrl).into(imageView);
//        }else {
//            imageView.setImageResource(R.drawable.user);
//        }
//        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

}

