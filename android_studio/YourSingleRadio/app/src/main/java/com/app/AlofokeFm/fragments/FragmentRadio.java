package com.app.AlofokeFm.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.app.AlofokeFm.R;
import com.app.AlofokeFm.activities.MainActivity;

@SuppressWarnings("deprecation")
public class FragmentRadio extends Fragment {

    private MainActivity mainActivity;
    private Toolbar toolbar;
    View view;
    public static FragmentRadio instance;

    public FragmentRadio() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_radio, container, false);
        instance = this;
        toolbar = view.findViewById(R.id.toolbar);
        setupToolbar();
        return view;
    }

    public static FragmentRadio getInstance() {
        return instance;
    }

    public void showToolbar(boolean show) {
        if (show) {
            toolbar.setVisibility(View.VISIBLE);
        } else {
            toolbar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity.setupNavigationDrawer(toolbar);
    }

    private void setupToolbar() {
        toolbar.setTitle("");
        mainActivity.setSupportActionBar(toolbar);
    }

}