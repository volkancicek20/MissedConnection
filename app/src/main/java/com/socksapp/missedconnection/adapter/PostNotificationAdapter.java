package com.socksapp.missedconnection.adapter;

import static com.socksapp.missedconnection.model.PostNotification.LAYOUT_EMPTY;
import static com.socksapp.missedconnection.model.PostNotification.LAYOUT_ONE;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.socksapp.missedconnection.databinding.RecyclerEmptyPostBinding;
import com.socksapp.missedconnection.databinding.RecyclerPostBinding;
import com.socksapp.missedconnection.databinding.RecyclerViewEmptyPostNotificationBinding;
import com.socksapp.missedconnection.databinding.RecyclerViewNotificationBinding;
import com.socksapp.missedconnection.fragment.PostsActivityFragment;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.model.PostNotification;
import com.socksapp.missedconnection.myclass.SharedPreferencesHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PostNotificationAdapter extends RecyclerView.Adapter {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firebaseFirestore;
    public ArrayList<PostNotification> arrayList;
    public Context context;
    public PostsActivityFragment fragment;
    private SharedPreferencesHelper sharedPreferencesHelper;

    public PostNotificationAdapter(ArrayList<PostNotification> arrayList, Context context, PostsActivityFragment fragment) {
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
                RecyclerViewNotificationBinding recyclerViewNotificationBinding = RecyclerViewNotificationBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
                return new PostNotificationHolder(recyclerViewNotificationBinding);
            case LAYOUT_EMPTY:
                RecyclerViewEmptyPostNotificationBinding recyclerViewEmptyPostNotificationBinding = RecyclerViewEmptyPostNotificationBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
                return new PostNotificationEmptyHolder(recyclerViewEmptyPostNotificationBinding);
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
        String name,explain,explain_post,city,district;
        Timestamp timestamp;
        switch (holder.getItemViewType()) {
            case LAYOUT_ONE:

                PostNotificationHolder postNotificationHolder = (PostNotificationHolder) holder;

                name = arrayList.get(position).name;
                city = arrayList.get(position).city;
                district = arrayList.get(position).district;
                explain = arrayList.get(position).explain;
                explain_post = arrayList.get(position).explain_post;
                timestamp = arrayList.get(position).timestamp;

                if(explain.equals("view")){
                    explain = "gönderinizi gördü";
                }

                String formattedTime = TimestampFormatter.formatTime(timestamp);
                String formattedDate = TimestampFormatter.formatDate(timestamp);

                String time = formattedTime + " | " + formattedDate;

                String city_and_district = city + "/" + district;

                postNotificationHolder.recyclerViewNotificationBinding.recyclerViewName.setText(name);
                postNotificationHolder.recyclerViewNotificationBinding.recyclerViewCityDistrict.setText(city_and_district);
                postNotificationHolder.recyclerViewNotificationBinding.recyclerViewExplain.setText(explain);
                postNotificationHolder.recyclerViewNotificationBinding.recyclerViewExplainPost.setText(explain_post);
                postNotificationHolder.recyclerViewNotificationBinding.recyclerViewTime.setText(time);


                break;
            case LAYOUT_EMPTY:

                PostNotificationEmptyHolder postNotificationEmptyHolder = (PostNotificationEmptyHolder) holder;

                postNotificationEmptyHolder.recyclerViewEmptyPostNotificationBinding.goAddPost.setOnClickListener(v -> fragment.goToAddPostFragment());

                break;
        }

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public static class PostNotificationHolder extends RecyclerView.ViewHolder {

        RecyclerViewNotificationBinding recyclerViewNotificationBinding;
        public PostNotificationHolder(RecyclerViewNotificationBinding recyclerViewNotificationBinding) {
            super(recyclerViewNotificationBinding.getRoot());
            this.recyclerViewNotificationBinding = recyclerViewNotificationBinding;
        }
    }

    public static class PostNotificationEmptyHolder extends RecyclerView.ViewHolder {

        RecyclerViewEmptyPostNotificationBinding recyclerViewEmptyPostNotificationBinding;
        public PostNotificationEmptyHolder(RecyclerViewEmptyPostNotificationBinding recyclerViewEmptyPostNotificationBinding) {
            super(recyclerViewEmptyPostNotificationBinding.getRoot());
            this.recyclerViewEmptyPostNotificationBinding = recyclerViewEmptyPostNotificationBinding;
        }
    }

    public static class TimestampFormatter {
        public static String formatTime(Timestamp timestamp) {
            Date date = timestamp.toDate();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return timeFormat.format(date);
        }

        public static String formatDate(Timestamp timestamp) {
            Date date = timestamp.toDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("tr"));
            return dateFormat.format(date);
        }
    }


}
