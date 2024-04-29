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
import com.socksapp.missedconnection.databinding.RecyclerEmptyMyPostBinding;
import com.socksapp.missedconnection.databinding.RecyclerPostBinding;
import com.socksapp.missedconnection.fragment.MyPostFragment;
import com.socksapp.missedconnection.model.FindPost;
import java.util.ArrayList;

public class MyPostAdapter extends RecyclerView.Adapter {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firebaseFirestore;
    public ArrayList<FindPost> arrayList;
    public Context context;
    public MyPostFragment fragment;

    public MyPostAdapter(ArrayList<FindPost> arrayList,Context context,MyPostFragment fragment) {
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
                RecyclerPostBinding recyclerPostBinding = RecyclerPostBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
                return new MyPostHolder(recyclerPostBinding);
            case LAYOUT_EMPTY:
                RecyclerEmptyMyPostBinding recyclerEmptyMyPostBinding = RecyclerEmptyMyPostBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
                return new MyPostEmptyHolder(recyclerEmptyMyPostBinding);
            default:
                return null;

        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        firebaseFirestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        String imageUrl,name,mail,city,district,place,date1,date2,time1,time2,explain;
        double lat,lng;
        int radius;
        DocumentReference documentReference;
        Timestamp timestamp;

        switch (holder.getItemViewType()) {
            case LAYOUT_ONE:

                MyPostHolder myPostHolder = (MyPostHolder) holder;

                imageUrl = arrayList.get(position).imageUrl;
                name = arrayList.get(position).name;
                mail = arrayList.get(position).mail;
                city = arrayList.get(position).city;
                district = arrayList.get(position).district;
                place = arrayList.get(position).place;
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

                myPostHolder.recyclerPostBinding.baseConstraint.setClickable(true);

                if(place.isEmpty()){
                    myPostHolder.recyclerPostBinding.placeIcon.setVisibility(View.GONE);
                }

                getShow(imageUrl,name,city,district,place,explain,timestamp,myPostHolder);

                ((MyPostHolder) holder).recyclerPostBinding.verticalMenu.setOnClickListener(v ->{
                    fragment.dialogShow(v,city,lat,lng,radius,documentReference,holder.getAdapterPosition());
                });

                break;
            case LAYOUT_EMPTY:

                break;
        }

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    private static class MyPostHolder extends RecyclerView.ViewHolder {
        RecyclerPostBinding recyclerPostBinding;
        public MyPostHolder(RecyclerPostBinding recyclerPostBinding) {
            super(recyclerPostBinding.getRoot());
            this.recyclerPostBinding = recyclerPostBinding;
        }
    }

    private static class MyPostEmptyHolder extends RecyclerView.ViewHolder {
        RecyclerEmptyMyPostBinding recyclerEmptyMyPostBinding;
        public MyPostEmptyHolder(RecyclerEmptyMyPostBinding recyclerEmptyMyPostBinding) {
            super(recyclerEmptyMyPostBinding.getRoot());
            this.recyclerEmptyMyPostBinding = recyclerEmptyMyPostBinding;
        }
    }

    public void getShow(String imageUrl,String name,String city,String district,String place,String explain,Timestamp timestamp,MyPostHolder holder){

        if(imageUrl.isEmpty()){
            ImageView imageView;
            imageView = holder.recyclerPostBinding.recyclerProfileImage;
            imageView.setImageResource(R.drawable.icon_person);
        }else {

            Glide.with(context)
                .load(imageUrl)
                .apply(new RequestOptions()
                .error(R.drawable.person_active_96)
                .centerCrop())
                .into(holder.recyclerPostBinding.recyclerProfileImage);
        }

        String location = city;
        if(district != null){
            location = location + "/" + district;
        }

        holder.recyclerPostBinding.recyclerCityAndDistrict.setText(location);
        holder.recyclerPostBinding.recyclerName.setText(name);
        holder.recyclerPostBinding.recyclerExplain.setText(explain);
        if(place != null){
            holder.recyclerPostBinding.recyclerPlace.setText(place);
        }else {
            holder.recyclerPostBinding.placeIcon.setVisibility(View.GONE);
        }

        long secondsElapsed = (Timestamp.now().getSeconds() - timestamp.getSeconds());
        String elapsedTime;

        if(secondsElapsed < 0){
            elapsedTime = "şimdi";
        } else if (secondsElapsed >= 31536000) {
            elapsedTime = "• " + (secondsElapsed / 31536000) + " yıl önce";
        } else if (secondsElapsed >= 2592000) {
            elapsedTime = "• " + (secondsElapsed / 2592000) + " ay önce";
        } else if (secondsElapsed >= 86400) {
            elapsedTime = "• " + (secondsElapsed / 86400) + " gün önce";
        } else if (secondsElapsed >= 3600) {
            elapsedTime = "• " + (secondsElapsed / 3600) + " saat önce";
        } else if (secondsElapsed >= 60) {
            elapsedTime = "• " + (secondsElapsed / 60) + " dakika önce";
        } else {
            elapsedTime = "• " + secondsElapsed + " saniye önce";
        }

        holder.recyclerPostBinding.timestampTime.setText(elapsedTime);

    }
}
