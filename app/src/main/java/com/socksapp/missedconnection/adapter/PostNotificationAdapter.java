package com.socksapp.missedconnection.adapter;

import static com.socksapp.missedconnection.model.PostNotification.LAYOUT_EMPTY;
import static com.socksapp.missedconnection.model.PostNotification.LAYOUT_ONE;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.socksapp.missedconnection.R;
import com.socksapp.missedconnection.databinding.RecyclerEmptyPostBinding;
import com.socksapp.missedconnection.databinding.RecyclerPostBinding;
import com.socksapp.missedconnection.databinding.RecyclerViewEmptyPostNotificationBinding;
import com.socksapp.missedconnection.databinding.RecyclerViewNotificationBinding;
import com.socksapp.missedconnection.fragment.PostsActivityFragment;
import com.socksapp.missedconnection.model.FindPost;
import com.socksapp.missedconnection.model.PostNotification;
import com.socksapp.missedconnection.myclass.SharedPreferencesGetLanguage;
import com.socksapp.missedconnection.myclass.SharedPreferencesHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostNotificationAdapter extends RecyclerView.Adapter {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firebaseFirestore;
    public ArrayList<PostNotification> arrayList;
    public Context context;
    public PostsActivityFragment fragment;
    private SharedPreferencesHelper sharedPreferencesHelper;
    private SharedPreferencesGetLanguage sharedPreferencesGetLanguage;

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
        sharedPreferencesGetLanguage = new SharedPreferencesGetLanguage(context);
        String language = sharedPreferencesGetLanguage.getString("language","");
        String myMail = sharedPreferencesHelper.getString("myMail", "");
        String name,other_name,explain,action_explain,city,district,imageUrl,galleryUrl;
        Timestamp timestamp,timestamp2;
        switch (holder.getItemViewType()) {
            case LAYOUT_ONE:

                PostNotificationHolder postNotificationHolder = (PostNotificationHolder) holder;

                imageUrl = arrayList.get(position).imageUrl;
                galleryUrl = arrayList.get(position).galleryUrl;
                name = arrayList.get(position).name;
                other_name = arrayList.get(position).other_name;
                city = arrayList.get(position).city;
                district = arrayList.get(position).district;
                explain = arrayList.get(position).explain;
                action_explain = arrayList.get(position).action_explain;
                timestamp = arrayList.get(position).timestamp;
                timestamp2 = arrayList.get(position).timestamp2;

                postNotificationHolder.recyclerViewNotificationBinding.recyclerProfileImage.setOnClickListener(v ->{
                    getImageShow(v,imageUrl);
                });

                postNotificationHolder.recyclerViewNotificationBinding.galleryImage.setOnClickListener(v ->{
                    getGalleryShow(v,galleryUrl);
                });

                getShow(name,other_name,imageUrl,galleryUrl,explain,action_explain,city,district,timestamp,timestamp2,postNotificationHolder,language);

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

    private void getShow(String name,String other_name,String imageUrl,String galleryUrl,String explain,String action_explain,String city,String district,Timestamp timestamp,Timestamp timestamp2,PostNotificationHolder holder,String language){
        if(action_explain.equals("view")){
            action_explain = context.getString(R.string.taraf_ndan_g_r_nt_lendi);
        }

        String city_and_district = city + "/" + district;

        holder.recyclerViewNotificationBinding.recyclerName.setText(name);
        holder.recyclerViewNotificationBinding.recyclerCityAndDistrict.setText(city_and_district);
        holder.recyclerViewNotificationBinding.recyclerExplain.setText(explain);


        if(language.equals("turkish")){
            holder.recyclerViewNotificationBinding.recyclerOtherName.setText(other_name);
            holder.recyclerViewNotificationBinding.recyclerActionExplain.setText(action_explain);
        }else {
            holder.recyclerViewNotificationBinding.recyclerOtherName.setText(action_explain);
            holder.recyclerViewNotificationBinding.recyclerActionExplain.setText(other_name);
        }

        if(imageUrl.isEmpty()){
            ImageView imageView;
            imageView = holder.recyclerViewNotificationBinding.recyclerProfileImage;
            imageView.setImageResource(R.drawable.person_active_96);
        }else {
            Glide.with(context)
                .load(imageUrl)
                .apply(new RequestOptions()
                .error(R.drawable.person_active_96)
                .centerCrop())
                .into(holder.recyclerViewNotificationBinding.recyclerProfileImage);
        }

        if(!galleryUrl.isEmpty()){
            int screenWidth = getScreenWidth(context);

            Glide.with(context)
                .load(galleryUrl)
                .apply(new RequestOptions()
                .error(R.drawable.icon_loading)
                .fitCenter()
                .centerCrop())
                .override(screenWidth, Target.SIZE_ORIGINAL)
                .into(holder.recyclerViewNotificationBinding.galleryImage);
        }



        long secondsElapsed = (Timestamp.now().getSeconds() - timestamp.getSeconds());
        String elapsedTime;

        if (language.equals("turkish")) {
            if(secondsElapsed < 0){
                elapsedTime = "şimdi";
            } else if (secondsElapsed >= 31536000) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("tr"));
                elapsedTime = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed >= 2592000) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", new Locale("tr"));
                elapsedTime = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed >= 86400) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", new Locale("tr"));
                elapsedTime = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed >= 3600) {
                elapsedTime = "• " + (secondsElapsed / 3600) + "s";
            } else if (secondsElapsed >= 60) {
                elapsedTime = "• " + (secondsElapsed / 60) + "d";
            } else {
                elapsedTime = "• " + secondsElapsed + " saniye";
            }
        } else {
            if(secondsElapsed < 0){
                elapsedTime = "now";
            } else if (secondsElapsed >= 31536000) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", new Locale("en"));
                elapsedTime = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed >= 2592000) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", new Locale("en"));
                elapsedTime = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed >= 86400) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", new Locale("en"));
                elapsedTime = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed >= 3600) {
                elapsedTime = "• " + (secondsElapsed / 3600) + "h";
            } else if (secondsElapsed >= 60) {
                elapsedTime = "• " + (secondsElapsed / 60) + "m";
            } else {
                elapsedTime = "• " + secondsElapsed + " seconds";
            }
        }

//        if(secondsElapsed < 0){
//            elapsedTime = context.getString(R.string.azonce);
//        } else if (secondsElapsed >= 31536000) {
//            elapsedTime = "• " + (secondsElapsed / 31536000) + context.getString(R.string.yil);
//        } else if (secondsElapsed >= 2592000) {
//            elapsedTime = "• " + (secondsElapsed / 2592000) + context.getString(R.string.ay);
//        } else if (secondsElapsed >= 86400) {
//            elapsedTime = "• " + (secondsElapsed / 86400) + context.getString(R.string.g);
//        } else if (secondsElapsed >= 3600) {
//            elapsedTime = "• " + (secondsElapsed / 3600) + context.getString(R.string.sa);
//        } else if (secondsElapsed >= 60) {
//            elapsedTime = "• " + (secondsElapsed / 60) + context.getString(R.string.d);
//        } else {
//            elapsedTime = "• " + secondsElapsed + context.getString(R.string.s);
//        }

        holder.recyclerViewNotificationBinding.timestampTime.setText(elapsedTime);


        long secondsElapsed2 = (Timestamp.now().getSeconds() - timestamp2.getSeconds());
        String elapsedTime2;

        if (language.equals("turkish")) {
            if(secondsElapsed2 < 0){
                elapsedTime2 = "şimdi";
            } else if (secondsElapsed2 >= 31536000) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("tr"));
                elapsedTime2 = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed2 >= 2592000) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", new Locale("tr"));
                elapsedTime2 = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed2 >= 86400) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", new Locale("tr"));
                elapsedTime2 = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed2 >= 3600) {
                elapsedTime2 = "• " + (secondsElapsed2 / 3600) + "s";
            } else if (secondsElapsed2 >= 60) {
                elapsedTime2 = "• " + (secondsElapsed2 / 60) + "d";
            } else {
                elapsedTime2 = "• " + secondsElapsed2 + " saniye";
            }
        } else {
            if(secondsElapsed2 < 0){
                elapsedTime2 = "now";
            } else if (secondsElapsed2 >= 31536000) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", new Locale("en"));
                elapsedTime2 = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed2 >= 2592000) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", new Locale("en"));
                elapsedTime2 = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed2 >= 86400) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", new Locale("en"));
                elapsedTime2 = "• " + dateFormat.format(timestamp.toDate());
            } else if (secondsElapsed2 >= 3600) {
                elapsedTime2 = "• " + (secondsElapsed2 / 3600) + "h";
            } else if (secondsElapsed2 >= 60) {
                elapsedTime2 = "• " + (secondsElapsed2 / 60) + "m";
            } else {
                elapsedTime2 = "• " + secondsElapsed2 + " seconds";
            }
        }

//        if(secondsElapsed2 < 0){
//            elapsedTime2 = context.getString(R.string.azonce);
//        } else if (secondsElapsed2 >= 31536000) {
//            elapsedTime2 = "• " + (secondsElapsed2 / 31536000) + context.getString(R.string.yil);
//        } else if (secondsElapsed2 >= 2592000) {
//            elapsedTime2 = "• " + (secondsElapsed2 / 2592000) + context.getString(R.string.ay);
//        } else if (secondsElapsed2 >= 86400) {
//            elapsedTime2 = "• " + (secondsElapsed2 / 86400) + context.getString(R.string.g);
//        } else if (secondsElapsed2 >= 3600) {
//            elapsedTime2 = "• " + (secondsElapsed2 / 3600) + context.getString(R.string.sa);
//        } else if (secondsElapsed2 >= 60) {
//            elapsedTime2 = "• " + (secondsElapsed2 / 60) + context.getString(R.string.d);
//        } else {
//            elapsedTime2 = "• " + secondsElapsed2 + context.getString(R.string.s);
//        }

        holder.recyclerViewNotificationBinding.recyclerTimestampTime.setText(elapsedTime2);
    }

    private void getImageShow(View view, String imageUrl) {
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        View popupView = inflater.inflate(R.layout.show_image, null);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

        Animation showAnimation = AnimationUtils.loadAnimation(view.getContext(), R.anim.popup_image_enter);
        popupView.startAnimation(showAnimation);

        Animation hideAnimation = AnimationUtils.loadAnimation(view.getContext(), R.anim.popup_image_exit);
        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                popupWindow.dismiss();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        CircleImageView imageView = popupView.findViewById(R.id.show_image);

        Glide.with(view.getContext())
            .load(imageUrl)
            .apply(new RequestOptions()
            .error(R.drawable.person_active_96)
            .centerCrop())
            .into(imageView);

        ConstraintLayout constraintLayout = popupView.findViewById(R.id.base_constraint_image);
        constraintLayout.setOnClickListener(v -> popupView.startAnimation(hideAnimation));

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }


    private void getGalleryShow(View view,String galleryUrl){
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        View popupView = inflater.inflate(R.layout.show_gallery, null);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

        Animation showAnimation = AnimationUtils.loadAnimation(view.getContext(), R.anim.popup_image_enter);
        popupView.startAnimation(showAnimation);

        Animation hideAnimation = AnimationUtils.loadAnimation(view.getContext(), R.anim.popup_image_exit);
        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                popupWindow.dismiss();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        ShapeableImageView imageView = popupView.findViewById(R.id.show_image);

        int screenWidth = getScreenWidth(context);

        Glide.with(context)
            .load(galleryUrl)
            .apply(new RequestOptions()
            .error(R.drawable.icon_loading)
            .fitCenter()
            .centerCrop())
            .override(screenWidth, Target.SIZE_ORIGINAL)
            .into(imageView);

        ConstraintLayout constraintLayout = popupView.findViewById(R.id.base_constraint_image);
        constraintLayout.setOnClickListener(v -> {
            popupWindow.dismiss();
        });

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    private int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }
}
