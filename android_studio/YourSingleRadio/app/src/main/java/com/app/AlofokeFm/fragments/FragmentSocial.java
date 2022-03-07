package com.app.AlofokeFm.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.adapters.AdapterSocial;
import com.app.AlofokeFm.callbacks.CallbackConfig;
import com.app.AlofokeFm.database.dao.AppDatabase;
import com.app.AlofokeFm.database.dao.DAO;
import com.app.AlofokeFm.database.dao.SocialEntity;
import com.app.AlofokeFm.models.Social;
import com.app.AlofokeFm.rests.ApiInterface;
import com.app.AlofokeFm.rests.RestAdapter;
import com.app.AlofokeFm.utils.Tools;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentSocial extends DialogFragment {

    private RecyclerView recyclerView;
    private AdapterSocial mAdapter;
    private SwipeRefreshLayout swipe_refresh;
    private Call<CallbackConfig> callbackCall = null;
    ArrayList<Social> items = new ArrayList<>();
    DAO db;
    View rootView;
    RelativeLayout parentView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_social, container, false);
        initView();
        return rootView;
    }

    private void initView() {

        parentView = rootView.findViewById(R.id.parentView);
        parentView.setOnClickListener(v -> {
            //do nothing
        });

        (rootView.findViewById(R.id.button_close)).setOnClickListener(v -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (getActivity() != null) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                    }
                }
                Tools.darkStatusBar(getActivity(), true);
                dismiss();
            }, 300);
        });

        swipe_refresh = rootView.findViewById(R.id.swipeRefreshLayout);
        swipe_refresh.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));

        if (Config.ENABLE_REMOTE_JSON) {
            loadDataFromRemoteJson();
        } else {
            db = AppDatabase.getDb(getActivity()).get();
            loadDataFromDatabase(db.getAllSocial());
        }

        mAdapter.setOnItemClickListener((view, obj, position) -> {
            if (Config.OPEN_SOCIAL_MENU_IN_EXTERNAL_BROWSER) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(obj.social_url));
                startActivity(intent);
            } else {
                FragmentWebView fragmentWebView = new FragmentWebView();
                Bundle args = new Bundle();
                args.putString("title", obj.social_name);
                args.putString("url", obj.social_url);
                fragmentWebView.setArguments(args);

                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out);
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.add(android.R.id.content, fragmentWebView).addToBackStack("page");
                transaction.commit();
            }
        });
    }

    private void loadDataFromDatabase(final List<SocialEntity> socials) {
        swipe_refresh.setEnabled(false);
        mAdapter = new AdapterSocial(getActivity(), new ArrayList<>());
        recyclerView.setAdapter(mAdapter);
        ArrayList<Social> items = new ArrayList<>();
        for (SocialEntity p : socials) items.add(p.original());
        showNoItemView(false);
        showFailedView(false, "");
        mAdapter.resetListData();
        mAdapter.setItems(items);
        if (socials.size() == 0) {
            showNoItemView(true);
        }
    }

    private void loadDataFromRemoteJson() {
        mAdapter = new AdapterSocial(getActivity(), items);
        recyclerView.setAdapter(mAdapter);
        swipe_refresh.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            mAdapter.resetListData();
            requestAction();
        });

        requestAction();
    }

    private void requestAPI() {
        ApiInterface apiInterface = RestAdapter.createAPI();
        callbackCall = apiInterface.getConfig(Config.JSON_URL);
        callbackCall.enqueue(new Callback<CallbackConfig>() {
            @Override
            public void onResponse(Call<CallbackConfig> call, Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                if (resp != null) {
                    displayData(resp.socials);
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(Call<CallbackConfig> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest();
            }

        });
    }

    private void displayData(final ArrayList<Social> socials) {
        mAdapter.setItems(socials);
        swipeProgress(false);
        if (socials.size() == 0) {
            showNoItemView(true);
        }
    }

    private void onFailRequest() {
        swipeProgress(false);
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction() {
        showFailedView(false, "");
        showNoItemView(false);
        swipeProgress(true);
        new Handler(Looper.getMainLooper()).postDelayed(this::requestAPI, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = rootView.findViewById(R.id.lyt_failed);
        ((TextView) rootView.findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        rootView.findViewById(R.id.btn_failed_retry).setOnClickListener(view -> requestAction());
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = rootView.findViewById(R.id.lyt_no_item);
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.no_social_found);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipe_refresh.setRefreshing(false);
            return;
        }
        swipe_refresh.post(() -> swipe_refresh.setRefreshing(true));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

}