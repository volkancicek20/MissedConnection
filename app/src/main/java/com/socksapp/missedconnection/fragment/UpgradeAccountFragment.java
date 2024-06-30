package com.socksapp.missedconnection.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.SkuDetailsParams;
import com.socksapp.missedconnection.databinding.FragmentUpgradeAccountBinding;

import java.util.Collections;
import java.util.List;

public class UpgradeAccountFragment extends Fragment {

    private FragmentUpgradeAccountBinding binding;
    private BillingClient billingClient;
    public UpgradeAccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUpgradeAccountBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.purchase.setOnClickListener(this::handlePurchaseButtonClick);
    }

    public void handlePurchaseButtonClick(View view) {

    }

}