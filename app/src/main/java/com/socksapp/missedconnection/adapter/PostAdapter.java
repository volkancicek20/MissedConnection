package com.socksapp.missedconnection.adapter;

import static com.socksapp.missedconnection.model.FindPost.LAYOUT_EMPTY;
import static com.socksapp.missedconnection.model.FindPost.LAYOUT_ONE;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.RecyclerEmptyPostBinding;
import com.socksapp.missedconnection.databinding.RecyclerPostBinding;
import com.socksapp.missedconnection.fragment.MainFragment;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.myclass.SharedPreferencesHelper;

import java.util.ArrayList;
import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firebaseFirestore;
    private SharedPreferencesHelper sharedPreferencesHelper;

    public ArrayList<FindPost> arrayList;
    public Context context;
    public MainFragment fragment;

    public PostAdapter(ArrayList<FindPost> arrayList,Context context,MainFragment fragment) {
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
                return new PostHolder(recyclerPostBinding);
            case LAYOUT_EMPTY:
                 RecyclerEmptyPostBinding recyclerViewEmptyPostBinding = RecyclerEmptyPostBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
                return new PostEmptyHolder(recyclerViewEmptyPostBinding);
            default:
                return null;

        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        firebaseFirestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        sharedPreferencesHelper = new SharedPreferencesHelper(context);
        String myMail = sharedPreferencesHelper.getString("myMail", "");
        String imageUrl,name,mail,city,district,place,explain,galleryUrl;
        double lat,lng;
        double radius;
        long date1,date2,time1,time2;
        DocumentReference documentReference;
        Timestamp timestamp;

        switch (holder.getItemViewType()) {
            case LAYOUT_ONE:


                PostHolder postHolder = (PostHolder) holder;

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

                postHolder.recyclerPostBinding.baseConstraint.setClickable(true);

                if(galleryUrl != null && !galleryUrl.isEmpty()){
                    postHolder.recyclerPostBinding.galleryImage.setVisibility(View.VISIBLE);
                }else {
                    postHolder.recyclerPostBinding.galleryImage.setVisibility(View.GONE);
                }

                postHolder.recyclerPostBinding.recyclerProfileImage.setOnClickListener(v ->{
                    getImageShow(v,imageUrl);
                });

                getShow(imageUrl,galleryUrl,name,city,district,explain,timestamp,postHolder);

                ((PostHolder) holder).recyclerPostBinding.verticalMenu.setOnClickListener(v ->{
                    fragment.dialogShow(v,mail,name,lat,lng,radius,documentReference);
                });

                if(!myMail.equals(mail)){
                    fragment.setActivityNotification(mail,documentReference);
                }

                break;
            case LAYOUT_EMPTY:

                PostEmptyHolder postEmptyHolder = (PostEmptyHolder) holder;

                postEmptyHolder.recyclerViewEmptyPostBinding.goFind.setOnClickListener(v -> fragment.goFind());

                break;
        }

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    private static class PostHolder extends RecyclerView.ViewHolder {
        RecyclerPostBinding recyclerPostBinding;
        public PostHolder(RecyclerPostBinding recyclerPostBinding) {
            super(recyclerPostBinding.getRoot());
            this.recyclerPostBinding = recyclerPostBinding;
        }
    }

    private static class PostEmptyHolder extends RecyclerView.ViewHolder {
        RecyclerEmptyPostBinding recyclerViewEmptyPostBinding;
        public PostEmptyHolder(RecyclerEmptyPostBinding recyclerViewEmptyPostBinding) {
            super(recyclerViewEmptyPostBinding.getRoot());
            this.recyclerViewEmptyPostBinding = recyclerViewEmptyPostBinding;
        }
    }

    public void getShow(String imageUrl,String galleryUrl,String name,String city,String district,String explain,Timestamp timestamp,PostHolder holder){
        if(imageUrl.isEmpty()){
            ImageView imageView;
            imageView = holder.recyclerPostBinding.recyclerProfileImage;
            imageView.setImageResource(R.drawable.person_active_96);
        }else {
            Glide.with(context)
                .load(imageUrl)
                .apply(new RequestOptions()
                .error(R.drawable.person_active_96)
                .centerCrop())
                .into(holder.recyclerPostBinding.recyclerProfileImage);
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
                .into(holder.recyclerPostBinding.galleryImage);
        }

        String location = city;
        if(district != null){
            location = location + "/" + district;
        }

        holder.recyclerPostBinding.recyclerCityAndDistrict.setText(location);
        holder.recyclerPostBinding.recyclerName.setText(name);
        holder.recyclerPostBinding.recyclerExplain.setText(explain);


        long secondsElapsed = (Timestamp.now().getSeconds() - timestamp.getSeconds());
        String elapsedTime;

        if(secondsElapsed < 0){
            elapsedTime = "şimdi";
        } else if (secondsElapsed >= 31536000) {
            elapsedTime = "• " + (secondsElapsed / 31536000) + "yıl";
        } else if (secondsElapsed >= 2592000) {
            elapsedTime = "• " + (secondsElapsed / 2592000) + "ay";
        } else if (secondsElapsed >= 86400) {
            elapsedTime = "• " + (secondsElapsed / 86400) + "g";
        } else if (secondsElapsed >= 3600) {
            elapsedTime = "• " + (secondsElapsed / 3600) + "sa";
        } else if (secondsElapsed >= 60) {
            elapsedTime = "• " + (secondsElapsed / 60) + "d";
        } else {
            elapsedTime = "• " + secondsElapsed + "s";
        }

        holder.recyclerPostBinding.timestampTime.setText(elapsedTime);

    }

    private int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    private void getImageShow(View view,String imageUrl){
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        View popupView = inflater.inflate(R.layout.show_image, null);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

        ImageView cancel = popupView.findViewById(R.id.cancel_image);

        cancel.setOnClickListener(v -> {
            popupWindow.dismiss();
        });

        ConstraintLayout constraintLayout = popupView.findViewById(R.id.base_constraint_image);
        constraintLayout.setOnClickListener(v -> {
            popupWindow.dismiss();
        });

//        popupWindow.setOutsideTouchable(true);
//        popupWindow.setFocusable(true);
//        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        CircleImageView imageView = popupView.findViewById(R.id.show_image);

        Glide.with(view.getContext())
            .load(imageUrl)
            .apply(new RequestOptions()
            .error(R.drawable.person_active_96)
            .centerCrop())
            .into(imageView);

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }
}
