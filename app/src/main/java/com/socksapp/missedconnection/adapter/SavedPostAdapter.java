package com.socksapp.missedconnection.adapter;

import static com.socksapp.missedconnection.model.FindPost.LAYOUT_EMPTY;
import static com.socksapp.missedconnection.model.FindPost.LAYOUT_ONE;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.RecyclerEmptySavedPostBinding;
import com.socksapp.missedconnection.databinding.RecyclerSavedPostBinding;
import com.socksapp.missedconnection.fragment.SavedPostFragment;
import com.socksapp.missedconnection.model.FindPost;
import java.util.ArrayList;

public class SavedPostAdapter extends RecyclerView.Adapter {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firebaseFirestore;
    public ArrayList<FindPost> arrayList;
    public Context context;
    public SavedPostFragment fragment;

    public SavedPostAdapter(ArrayList<FindPost> arrayList, Context context, SavedPostFragment fragment) {
        this.arrayList = arrayList;
        this.context = context;
        this.fragment = fragment;
    }

    @Override
    public int getItemViewType(int position) {
        if(arrayList.isEmpty()){
            return 2;
        }else {
            if (arrayList.get(position).getViewType() == 1) {
                return LAYOUT_ONE;
            }
            if (arrayList.get(position).getViewType() == 2) {
                return LAYOUT_EMPTY;
            }
            return -1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType){
            case LAYOUT_ONE:
                RecyclerSavedPostBinding recyclerSavedPostBinding = RecyclerSavedPostBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
                return new SavedPostHolder(recyclerSavedPostBinding);
            case LAYOUT_EMPTY:
                RecyclerEmptySavedPostBinding recyclerEmptySavedPostBinding = RecyclerEmptySavedPostBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
                return new SavedPostEmptyHolder(recyclerEmptySavedPostBinding);
            default:
                return null;

        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        firebaseFirestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        String imageUrl,name,mail,city,district,place,explain,galleryUrl;
        double lat,lng;
        double radius;
        long date1,date2,time1,time2;
        DocumentReference documentReference;
        Timestamp timestamp;

        switch (holder.getItemViewType()) {
            case LAYOUT_ONE:

                SavedPostHolder savedPostHolder = (SavedPostHolder) holder;

                imageUrl = arrayList.get(position).imageUrl;
                galleryUrl = arrayList.get(position).galleryUrl;
                name = arrayList.get(position).name;
                mail = arrayList.get(position).mail;
                city = arrayList.get(position).city;
                district = arrayList.get(position).district;
                date1 = arrayList.get(position).date1;
                date2 = arrayList.get(position).date2;
                time1 = arrayList.get(position).time1;
                time2 = arrayList.get(position).time2;
                explain = arrayList.get(position).explain;
                lat = arrayList.get(position).lat;
                lng = arrayList.get(position).lng;
                radius = arrayList.get(position).radius;
                timestamp = arrayList.get(position).timestamp;
                documentReference = arrayList.get(position).documentReference;

                savedPostHolder.recyclerSavedPostBinding.baseConstraint.setClickable(true);

                if(galleryUrl != null && !galleryUrl.isEmpty()){
                    savedPostHolder.recyclerSavedPostBinding.galleryImage.setVisibility(View.VISIBLE);
                }else {
                    savedPostHolder.recyclerSavedPostBinding.galleryImage.setVisibility(View.GONE);
                }

                getShow(imageUrl,galleryUrl,name,city,district,explain,timestamp,savedPostHolder);

                savedPostHolder.recyclerSavedPostBinding.removeSavedMenu.setOnClickListener(v ->{
                    fragment.removeSaved(v,documentReference.getId(),holder.getAdapterPosition());
                });

                break;
            case LAYOUT_EMPTY:

                SavedPostEmptyHolder savedPostEmptyHolder = (SavedPostEmptyHolder) holder;

                savedPostEmptyHolder.recyclerEmptySavedPostBinding.goMain.setOnClickListener(v -> fragment.goMain());

                break;
        }

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public static class SavedPostHolder extends RecyclerView.ViewHolder {
        RecyclerSavedPostBinding recyclerSavedPostBinding;
        public SavedPostHolder(RecyclerSavedPostBinding recyclerSavedPostBinding) {
            super(recyclerSavedPostBinding.getRoot());
            this.recyclerSavedPostBinding = recyclerSavedPostBinding;
        }
    }

    public static class SavedPostEmptyHolder extends RecyclerView.ViewHolder {

        RecyclerEmptySavedPostBinding recyclerEmptySavedPostBinding;
        public SavedPostEmptyHolder(RecyclerEmptySavedPostBinding recyclerEmptySavedPostBinding) {
            super(recyclerEmptySavedPostBinding.getRoot());
            this.recyclerEmptySavedPostBinding = recyclerEmptySavedPostBinding;
        }
    }

    public void getShow(String imageUrl,String galleryUrl, String name, String city, String district, String explain, Timestamp timestamp, SavedPostAdapter.SavedPostHolder holder){
        if(imageUrl.isEmpty()){
            ImageView imageView;
            imageView = holder.recyclerSavedPostBinding.recyclerProfileImage;
            imageView.setImageResource(R.drawable.person_active_96);
        }else {
            Glide.with(context)
                .load(imageUrl)
                .apply(new RequestOptions()
                .error(R.drawable.person_active_96)
                .centerCrop())
                .into(holder.recyclerSavedPostBinding.recyclerProfileImage);
        }

        if(!galleryUrl.isEmpty()){
            int screenWidth = getScreenWidth(context);

            Glide.with(context)
                .load(galleryUrl)
                .apply(new RequestOptions()
                .error(R.drawable.icon_loading)
                .fitCenter()
                .centerCrop())
                .override(screenWidth, 500)
                .into(holder.recyclerSavedPostBinding.galleryImage);
        }

        String location = city;
        if(district != null){
            location = location + "/" + district;
        }

        holder.recyclerSavedPostBinding.recyclerCityAndDistrict.setText(location);
        holder.recyclerSavedPostBinding.recyclerName.setText(name);
        holder.recyclerSavedPostBinding.recyclerExplain.setText(explain);


        long secondsElapsed = (Timestamp.now().getSeconds() - timestamp.getSeconds());
        String elapsedTime;

        if(secondsElapsed < 0){
            elapsedTime = context.getString(R.string.azonce);
        } else if (secondsElapsed >= 31536000) {
            elapsedTime = "• " + (secondsElapsed / 31536000) + context.getString(R.string.yil);
        } else if (secondsElapsed >= 2592000) {
            elapsedTime = "• " + (secondsElapsed / 2592000) + context.getString(R.string.ay);
        } else if (secondsElapsed >= 86400) {
            elapsedTime = "• " + (secondsElapsed / 86400) + context.getString(R.string.g);
        } else if (secondsElapsed >= 3600) {
            elapsedTime = "• " + (secondsElapsed / 3600) + context.getString(R.string.sa);
        } else if (secondsElapsed >= 60) {
            elapsedTime = "• " + (secondsElapsed / 60) + context.getString(R.string.d);
        } else {
            elapsedTime = "• " + secondsElapsed + context.getString(R.string.s);
        }

        holder.recyclerSavedPostBinding.timestampTime.setText(elapsedTime);

    }

    private int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }
}
