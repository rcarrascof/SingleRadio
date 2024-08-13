package com.app.matrixFM.fragments;

import static com.app.matrixFM.utils.Constant.LOCALHOST_ADDRESS;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
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

import com.app.matrixFM.Config;
import com.app.matrixFM.R;
import com.app.matrixFM.activities.MainActivity;
import com.app.matrixFM.adapters.AdapterSocial;
import com.app.matrixFM.callbacks.CallbackConfig;
import com.app.matrixFM.database.dao.AppDatabase;
import com.app.matrixFM.database.dao.DAO;
import com.app.matrixFM.database.dao.SocialEntity;
import com.app.matrixFM.models.Social;
import com.app.matrixFM.rests.ApiInterface;
import com.app.matrixFM.rests.RestAdapter;
import com.app.matrixFM.utils.Utils;
import com.solodroid.ads.sdk.util.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentSocial extends DialogFragment {

    private RecyclerView recyclerView;
    private AdapterSocial adapterSocial;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Call<CallbackConfig> callbackCall = null;
    ArrayList<Social> items = new ArrayList<>();
    DAO db;
    View rootView;
    RelativeLayout parentView;
    MainActivity activity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

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
                Utils.darkStatusBar(getActivity(), true);
                dismiss();
            }, 300);
        });

        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.color_light_primary);

        recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));

        if (Config.ENABLE_REMOTE_JSON) {
            loadDataFromRemoteJson();
        } else {
            db = AppDatabase.getDb(getActivity()).get();
            loadDataFromDatabase(db.getAllSocial());
        }

        adapterSocial.setOnItemClickListener((view, obj, position) -> {
            if (Config.OPEN_SOCIAL_MENU_IN_EXTERNAL_BROWSER) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(obj.social_url));
                startActivity(intent);
            } else {
                urlHandler(obj.social_name, obj.social_url);
            }
        });
    }

    private void urlHandler(String title, String url) {
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            if (url.contains("mailto:")) {
                MailTo mailTo = MailTo.parse(url);
                Utils.startEmailActivity(activity, mailTo.getTo(), mailTo.getSubject(), mailTo.getBody());
            } else if (url.contains("tel:")) {
                Utils.startCallActivity(activity, url);
            } else if (url.contains("sms:")) {
                Utils.startSmsActivity(activity, url);
            } else if (url.contains("geo:")) {
                Utils.startMapSearchActivity(activity, url);
            } else if (url.contains("instagram.com")) {
                Utils.startWebActivity(activity, url);
            } else if (url.contains("facebook.com") || url.contains("fb://")) {
                Utils.startWebActivity(activity, url);
            } else if (url.contains("twitter.com") || url.contains("twitter://")) {
                Utils.startWebActivity(activity, url);
            } else if (url.contains("api.whatsapp.com") || url.contains("whatsapp://")) {
                Utils.startWebActivity(activity, url);
            } else if (url.contains("maps.google.com")) {
                Utils.startWebActivity(activity, url);
            } else {
                FragmentWebView fragmentWebView = new FragmentWebView();
                Bundle args = new Bundle();
                args.putString("title", title);
                args.putString("url", url);
                fragmentWebView.setArguments(args);
                FragmentManager fragmentManager = getFragmentManager();
                assert fragmentManager != null;
                FragmentTransaction transaction = fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0, 0, R.anim.slide_down);
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.add(android.R.id.content, fragmentWebView).addToBackStack("page");
                transaction.commit();
            }
        }
    }

    private void loadDataFromDatabase(final List<SocialEntity> socials) {
        swipeRefreshLayout.setEnabled(false);
        adapterSocial = new AdapterSocial(getActivity(), new ArrayList<>());
        recyclerView.setAdapter(adapterSocial);
        ArrayList<Social> items = new ArrayList<>();
        for (SocialEntity p : socials) items.add(p.original());
        showNoItemView(false);
        showFailedView(false, "");
        adapterSocial.resetListData();
        adapterSocial.setItems(items);
        if (socials.size() == 0) {
            showNoItemView(true);
        }
    }

    private void loadDataFromRemoteJson() {
        adapterSocial = new AdapterSocial(getActivity(), items);
        recyclerView.setAdapter(adapterSocial);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterSocial.resetListData();
            requestAction();
        });

        requestAction();
    }

    private void requestAPI(String remoteUrl) {
        ApiInterface apiInterface = RestAdapter.createAPI();
        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            if (remoteUrl.contains("https://drive.google.com")) {
                String driveUrl = remoteUrl.replace("https://", "").replace("http://", "");
                List<String> data = Arrays.asList(driveUrl.split("/"));
                String googleDriveFileId = data.get(3);
                callbackCall = apiInterface.getDriveJsonFileId(googleDriveFileId);
            } else {
                callbackCall = apiInterface.getJsonUrl(remoteUrl);
            }
        } else {
            callbackCall = apiInterface.getDriveJsonFileId(remoteUrl);
        }
        callbackCall.enqueue(new Callback<CallbackConfig>() {
            @Override
            public void onResponse(@NonNull Call<CallbackConfig> call, @NonNull Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                if (resp != null) {
                    displayData(resp.socials);
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackConfig> call, @NonNull Throwable t) {
                if (!call.isCanceled()) onFailRequest();
            }

        });
    }

    private void displayData(final ArrayList<Social> socials) {
        adapterSocial.setItems(socials);
        swipeProgress(false);
        if (socials.size() == 0) {
            showNoItemView(true);
        }
    }

    private void onFailRequest() {
        swipeProgress(false);
        if (Utils.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction() {
        showFailedView(false, "");
        showNoItemView(false);
        swipeProgress(true);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!Config.ACCESS_KEY.contains("XXXXX")) {
                String data = Tools.decode(Config.ACCESS_KEY);
                String[] results = data.split("_applicationId_");
                String remoteUrl = results[0].replace("http://localhost", LOCALHOST_ADDRESS);
                requestAPI(remoteUrl);
            }
        }, 10);
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
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

}