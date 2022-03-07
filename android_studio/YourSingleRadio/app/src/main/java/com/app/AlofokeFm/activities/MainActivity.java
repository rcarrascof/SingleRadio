package com.app.AlofokeFm.activities;

import static com.app.AlofokeFm.Config.USE_LEGACY_GDPR_EU_CONSENT;
import static com.app.AlofokeFm.utils.Constant.ADMOB;
import static com.app.AlofokeFm.utils.Constant.AD_STATUS_ON;
import static com.app.AlofokeFm.utils.Constant.APPLOVIN;
import static com.app.AlofokeFm.utils.Constant.STARTAPP;
import static com.app.AlofokeFm.utils.Constant.UNITY;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.app.AlofokeFm.BuildConfig;
import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.database.dao.AppDatabase;
import com.app.AlofokeFm.database.dao.DAO;
import com.app.AlofokeFm.database.dao.RadioEntity;
import com.app.AlofokeFm.database.prefs.AdsPref;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.fragments.FragmentRadio;
import com.app.AlofokeFm.fragments.FragmentSocial;
import com.app.AlofokeFm.fragments.FragmentWebView;
import com.app.AlofokeFm.models.Radio;
import com.app.AlofokeFm.services.RadioPlayerService;
import com.app.AlofokeFm.utils.AdNetwork;
import com.app.AlofokeFm.utils.Constant;
import com.app.AlofokeFm.utils.GDPR;
import com.app.AlofokeFm.utils.RelativePopupWindow;
import com.app.AlofokeFm.utils.SleepTimeReceiver;
import com.app.AlofokeFm.utils.Tools;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.makeramen.roundedimageview.RoundedImageView;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import es.claucookie.miniequalizerlibrary.EqualizerView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private final static String COLLAPSING_TOOLBAR_FRAGMENT_TAG = "";
    private final static String SELECTED_TAG = "selected_index";
    private static int selectedIndex;
    private final static int COLLAPSING_TOOLBAR = 0;
    ActionBarDrawerToggle actionBarDrawerToggle;
    private DrawerLayout drawerLayout;
    NavigationView navigationView;
    ProgressBar progressBar;
    RoundedImageView img_radio_large;
    RoundedImageView img_album_art_large;
    ImageView img_music_background;
    ImageButton img_volume;
    ImageButton img_timer;
    FloatingActionButton fab_play_expand;
    TextView txt_radio_expand, txt_radio_music_song, txt_song_expand;
    SharedPref sharedPref;
    Handler handler = new Handler();
    FragmentManager fragmentManager;
    EqualizerView equalizerView;
    Tools tools;
    AdsPref adsPref;
    private ConsentInformation consentInformation;
    ConsentForm consentForm;
    private DAO db;
    AdNetwork adNetwork;
    LinearLayout lyt_exit;
    View lyt_dialog;
    List<RadioEntity> radioEntities;
    ArrayList<Radio> radios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.darkStatusBar(this, true);
        adsPref = new AdsPref(this);
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            if (adsPref.getAdType().equals(STARTAPP)) {
                StartAppSDK.init(MainActivity.this, adsPref.getStartappAppID(), false);
                StartAppSDK.setTestAdsEnabled(BuildConfig.DEBUG);
            }
        }
        setContentView(R.layout.activity_main);
        db = AppDatabase.getDb(this).get();
        adNetwork = new AdNetwork(this);

        if (Config.ENABLE_RTL_MODE) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        initAdNetwork();

        Constant.is_app_open = true;

        sharedPref = new SharedPref(this);
        sharedPref.setCheckSleepTime();
        tools = new Tools(this);

        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        drawerLayout = findViewById(R.id.drawer_layout);

        if ((ContextCompat.checkSelfPermission(MainActivity.this, "android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_GRANTED)) {
            Menu menu = navigationView.getMenu();
            MenuItem menuItem = menu.findItem(R.id.drawer_permission);
            menuItem.setVisible(false);
        }

        if (savedInstanceState != null) {
            navigationView.getMenu().getItem(savedInstanceState.getInt(SELECTED_TAG)).setChecked(true);
            return;
        }

        selectedIndex = COLLAPSING_TOOLBAR;
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, new FragmentRadio(), COLLAPSING_TOOLBAR_FRAGMENT_TAG)
                .commit();

        initComponent();
        initExitDialog();
        Tools.notificationHandler(this, getIntent());
        loadConfig();

        adNetwork.loadBannerAdNetwork(1);
        adNetwork.loadInterstitialAdNetwork(1);
        adNetwork.loadNativeAdNetwork(1);

    }

    private void initAdNetwork() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    MobileAds.initialize(this, initializationStatus -> {
                        Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                        for (String adapterClass : statusMap.keySet()) {
                            AdapterStatus status = statusMap.get(adapterClass);
                            assert status != null;
                            Log.d(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d", adapterClass, status.getDescription(), status.getLatency()));
                            Log.d(TAG, "FAN open bidding with AdMob as mediation partner selected");
                        }
                    });
                    if (USE_LEGACY_GDPR_EU_CONSENT) {
                        GDPR.updateConsentStatus(this);
                    } else {
                        updateConsentStatus();
                    }
                    break;
                case STARTAPP:
                    StartAppSDK.setUserConsent(this, "pas", System.currentTimeMillis(), true);
                    StartAppAd.disableSplash();
                    break;
                case UNITY:
                    UnityAds.addListener(new IUnityAdsListener() {
                        @Override
                        public void onUnityAdsReady(String placementId) {
                            Log.d(TAG, placementId);
                        }

                        @Override
                        public void onUnityAdsStart(String placementId) {

                        }

                        @Override
                        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {

                        }

                        @Override
                        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {

                        }
                    });
                    UnityAds.initialize(getApplicationContext(), adsPref.getUnityGameId(), BuildConfig.DEBUG, new IUnityAdsInitializationListener() {
                        @Override
                        public void onInitializationComplete() {
                            Log.d(TAG, "Unity Ads Initialization Complete");
                            Log.d(TAG, "Unity Ads Game ID : " + adsPref.getUnityGameId());
                        }

                        @Override
                        public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                            Log.d(TAG, "Unity Ads Initialization Failed: [" + error + "] " + message);
                        }
                    });
                    break;
                case APPLOVIN:
                    AppLovinSdk.getInstance(this).setMediationProvider(AppLovinMediationProvider.MAX);
                    AppLovinSdk.getInstance(this).initializeSdk(config -> {
                    });
                    final String sdkKey = AppLovinSdk.getInstance(getApplicationContext()).getSdkKey();
                    if (!sdkKey.equals(getString(R.string.applovin_sdk_key))) {
                        Log.e(TAG, "AppLovin ERROR : Please update your sdk key in the manifest file.");
                    }
                    Log.d(TAG, "AppLovin SDK Key : " + sdkKey);
                    break;
            }
        }
    }

    private void loadConfig() {
        Constant.item_radio.clear();
        new Handler().postDelayed(() -> {
            radioEntities = db.getAllRadio();
            radios = new ArrayList<>();
            for (RadioEntity radio : radioEntities) radios.add(radio.original());
            displayData();
        }, 50);
    }

    private void displayData() {

        Constant.item_radio.addAll(radios);
        changeText(radios.get(0));

        fab_play_expand.setOnClickListener(view -> {
            if (!tools.isNetworkAvailable()) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.internet_not_connected), Toast.LENGTH_SHORT).show();
            } else if (txt_song_expand.getText().equals(getString(R.string.app_name))) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.no_radio_selected), Toast.LENGTH_SHORT).show();
            } else {
                Radio radio = radios.get(0);
                final Intent intent = new Intent(MainActivity.this, RadioPlayerService.class);

                if (RadioPlayerService.getInstance() != null) {
                    Radio playerCurrentRadio = RadioPlayerService.getInstance().getPlayingRadioStation();
                    if (playerCurrentRadio != null) {
                        if (radio.getRadio_id() != RadioPlayerService.getInstance().getPlayingRadioStation().getRadio_id()) {
                            RadioPlayerService.getInstance().initialize(MainActivity.this, radio);
                            intent.setAction(RadioPlayerService.ACTION_PLAY);
                        } else {
                            intent.setAction(RadioPlayerService.ACTION_TOGGLE);
                        }
                    } else {
                        RadioPlayerService.getInstance().initialize(MainActivity.this, radio);
                        intent.setAction(RadioPlayerService.ACTION_PLAY);
                    }
                } else {
                    RadioPlayerService.createInstance().initialize(MainActivity.this, radio);
                    intent.setAction(RadioPlayerService.ACTION_PLAY);
                }
                startService(intent);

                if (!Constant.is_playing) {
                    adNetwork.showInterstitialAdNetwork(1, adsPref.getInterstitialAdInterval());
                }

            }
        });

        if (Config.ENABLE_AUTOPLAY) {
            if (tools.isNetworkAvailable()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> fab_play_expand.performClick(), Constant.DELAY_PERFORM_CLICK);
            }
        }

    }

    public void initComponent() {

        fragmentManager = getSupportFragmentManager();

        img_music_background = findViewById(R.id.img_music_background);

        equalizerView = findViewById(R.id.equalizer_view);
        progressBar = findViewById(R.id.progress_bar);
        img_timer = findViewById(R.id.img_timer);

        img_radio_large = findViewById(R.id.img_radio_large);
        img_album_art_large = findViewById(R.id.img_album_art_large);

        if (Config.CIRCULAR_RADIO_IMAGE_ALBUM_ART) {
            img_radio_large.setOval(true);
            img_album_art_large.setOval(true);
        } else {
            img_radio_large.setOval(false);
            img_album_art_large.setOval(false);
        }

        img_volume = findViewById(R.id.img_volume);
        fab_play_expand = findViewById(R.id.fab_play);
        txt_radio_expand = findViewById(R.id.txt_radio_name_expand);
        txt_song_expand = findViewById(R.id.txt_metadata_expand);

        if (!tools.isNetworkAvailable()) {
            txt_radio_expand.setText(getResources().getString(R.string.app_name));
            txt_song_expand.setText(getResources().getString(R.string.internet_not_connected));
        }

        setIfPlaying();

        img_timer.setOnClickListener(v -> {
            if (sharedPref.getIsSleepTimeOn()) {
                openTimeDialog();
            } else {
                openTimeSelectDialog();
            }
        });

        img_volume.setOnClickListener(v -> changeVolume());

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAG, selectedIndex);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.drawer_recent) {
            FragmentRadio fragment = new FragmentRadio();
            loadFrag(fragment, fragmentManager);
            drawerLayout.closeDrawer(GravityCompat.START);
            hideKeyboard();

            return true;
        } else if (itemId == R.id.drawer_social) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentSocial fragmentSocial = new FragmentSocial();
            FragmentTransaction transaction = fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(android.R.id.content, fragmentSocial).addToBackStack("social");
            transaction.commit();
            Tools.darkStatusBar(this, false);
            drawerLayout.closeDrawer(GravityCompat.START);
            hideKeyboard();

            return true;
        } else if (itemId == R.id.drawer_rate) {
            final String package_name = BuildConfig.APPLICATION_ID;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + package_name)));
            } catch (ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + package_name)));
            }
            drawerLayout.closeDrawer(GravityCompat.START);

            return true;
        } else if (itemId == R.id.drawer_more) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedPref.getMoreAppsUrl())));
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.drawer_share) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_content) + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            intent.setType("text/plain");
            startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.drawer_privacy) {
            FragmentWebView fragmentWebView = new FragmentWebView();
            Bundle args = new Bundle();
            args.putString("title", getString(R.string.drawer_privacy_policy));
            args.putString("url", sharedPref.getPrivacyPolicyUrl());
            fragmentWebView.setArguments(args);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(android.R.id.content, fragmentWebView).addToBackStack("page");
            transaction.commit();
            Tools.darkStatusBar(this, false);
            drawerLayout.closeDrawer(GravityCompat.START);
            hideKeyboard();
            return true;
        } else if (itemId == R.id.drawer_permission) {
            startActivity(new Intent(getApplicationContext(), ActivityPermission.class));
            drawerLayout.closeDrawer(GravityCompat.START);
            hideKeyboard();
            return true;
        } else if (itemId == R.id.drawer_about) {
            aboutDialog();
            drawerLayout.closeDrawer(GravityCompat.START);
            hideKeyboard();

            return true;
        }
        return false;
    }

    public void setupNavigationDrawer(Toolbar toolbar) {
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };

        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    public void loadFrag(Fragment f1, FragmentManager fm) {
        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack();
        }
        FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.fragment_container, f1);
        ft.commit();
    }

    public void changePlayPause(Boolean flag) {
        Constant.is_playing = flag;
        if (flag) {
            Radio radio = RadioPlayerService.getInstance().getPlayingRadioStation();
            if (radio != null) {
                changeText(radio);
                fab_play_expand.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_pause_white));
                equalizerView.animateBars();
            }
        } else {
            if (Constant.item_radio.size() > 0) {
                changeText(Constant.item_radio.get(Constant.position));
            }
            fab_play_expand.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_play_arrow_white));
            equalizerView.stopBars();
            img_album_art_large.setVisibility(View.GONE);
        }
    }

    public void changeText(Radio radio) {
        if (Constant.radio_type) {
            changeSongName(Constant.metadata);

            if (Constant.metadata == null || Constant.metadata.equals(radio.getRadio_genre())) {
                img_album_art_large.setVisibility(View.GONE);
            } else {
                img_album_art_large.setVisibility(View.VISIBLE);
            }

            txt_song_expand.setVisibility(View.VISIBLE);
            img_timer.setVisibility(View.VISIBLE);
        } else {
            txt_radio_music_song.setText("");
            txt_song_expand.setText(radio.getRadio_name());
            txt_song_expand.setVisibility(View.INVISIBLE);
            img_timer.setVisibility(View.GONE);
        }
        txt_radio_expand.setText(radio.getRadio_name());

        if (!Constant.is_playing) {
            txt_song_expand.setText(radio.getRadio_genre());
        }

      /*  Glide.with(this)
                .load(radio.getBackground_image_url().replace(" ", "%20"))
                .placeholder(R.drawable.ic_thumbnail)
                //.apply(RequestOptions.bitmapTransform(new BlurTransformation(20, 3)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(img_music_background);

        Glide.with(this)
                .load(radio.getRadio_image_url().replace(" ", "%20"))
                .placeholder(R.drawable.ic_artwork)
                .into(img_radio_large);*/
    }

    public void changeSongName(String songName) {
        Constant.metadata = songName;
        txt_song_expand.setText(songName);
    }

    public void changeAlbumArt(String artworkUrl) {
        Constant.albumArt = artworkUrl;


        Glide.with(this)
                .load(artworkUrl.replace(" ", "%20"))
                .placeholder(android.R.color.transparent)
                .thumbnail(0.3f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        img_album_art_large.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        img_album_art_large.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(img_album_art_large);
    }

    public void setIfPlaying() {
        if (RadioPlayerService.getInstance() != null) {
            RadioPlayerService.initNewContext(MainActivity.this);
            changePlayPause(RadioPlayerService.getInstance().isPlaying());
        } else {
            changePlayPause(false);
        }
    }


    public void setBuffer(Boolean flag) {
        if (flag) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void changeVolume() {
        final RelativePopupWindow popupWindow = new RelativePopupWindow(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        assert inflater != null;
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.lyt_volume, null);
        ImageView imageView1 = view.findViewById(R.id.img_volume_max);
        ImageView imageView2 = view.findViewById(R.id.img_volume_min);
        imageView1.setColorFilter(Color.BLACK);
        imageView2.setColorFilter(Color.BLACK);

        VerticalSeekBar seekBar = view.findViewById(R.id.seek_bar_volume);
        seekBar.getThumb().setColorFilter(sharedPref.getFirstColor(), PorterDuff.Mode.SRC_IN);
        seekBar.getProgressDrawable().setColorFilter(sharedPref.getSecondColor(), PorterDuff.Mode.SRC_IN);

        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert am != null;
        seekBar.setMax(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int volume_level = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekBar.setProgress(volume_level);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        popupWindow.setFocusable(true);
        popupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setContentView(view);
        popupWindow.showOnAnchor(img_volume, RelativePopupWindow.VerticalPosition.ABOVE, RelativePopupWindow.HorizontalPosition.CENTER);
    }

    public void openTimeSelectDialog() {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setTitle(getString(R.string.sleep_time));

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.lyt_dialog_select_time, null);
        alt_bld.setView(dialogView);

        final TextView tv_min = dialogView.findViewById(R.id.txt_minutes);
        tv_min.setText("1 " + getString(R.string.min));

        SeekBar seekBar = dialogView.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_min.setText(progress + " " + getString(R.string.min));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        alt_bld.setPositiveButton(getString(R.string.set), (dialog, which) -> {
            String hours = String.valueOf(seekBar.getProgress() / 60);
            String minute = String.valueOf(seekBar.getProgress() % 60);

            if (hours.length() == 1) {
                hours = "0" + hours;
            }

            if (minute.length() == 1) {
                minute = "0" + minute;
            }

            String totalTime = hours + ":" + minute;
            long total_timer = tools.convertToMilliSeconds(totalTime) + System.currentTimeMillis();

            Random random = new Random();
            int id = random.nextInt(100);

            sharedPref.setSleepTime(true, total_timer, id);

            Intent intent = new Intent(MainActivity.this, SleepTimeReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), id, intent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                assert alarmManager != null;
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, total_timer, pendingIntent);
            } else {
                assert alarmManager != null;
                alarmManager.set(AlarmManager.RTC_WAKEUP, total_timer, pendingIntent);
            }
        });
        alt_bld.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {

        });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    public void openTimeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.sleep_time));
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.lyt_dialog_time, null);
        builder.setView(dialogView);

        TextView textView = dialogView.findViewById(R.id.txt_time);

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {

        });

        builder.setPositiveButton(getString(R.string.stop), (dialog, which) -> {
            Intent i = new Intent(MainActivity.this, SleepTimeReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, sharedPref.getSleepID(), i, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            pendingIntent.cancel();
            assert alarmManager != null;
            alarmManager.cancel(pendingIntent);
            sharedPref.setSleepTime(false, 0, 0);
        });

        updateTimer(textView, sharedPref.getSleepTime());

        builder.show();
    }

    private void updateTimer(final TextView textView, long time) {
        long timeleft = time - System.currentTimeMillis();
        if (timeleft > 0) {
            @SuppressLint("DefaultLocale") String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeleft),
                    TimeUnit.MILLISECONDS.toMinutes(timeleft) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(timeleft) % TimeUnit.MINUTES.toSeconds(1));

            textView.setText(hms);
            handler.postDelayed(() -> {
                if (sharedPref.getIsSleepTimeOn()) {
                    updateTimer(textView, sharedPref.getSleepTime());
                }
            }, 1000);
        }
    }

    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (count != 0) {
            getSupportFragmentManager().popBackStack();
            if (count == 1) {
                Tools.darkStatusBar(this, true);
            }
        } else {
            lyt_exit.setVisibility(View.VISIBLE);
            showExitDialog(true);
        }
    }

    public void showExitDialog(boolean exit) {
        if (exit) {
            lyt_exit.setVisibility(View.VISIBLE);
        } else {
            lyt_exit.setVisibility(View.GONE);
        }
    }

    public void initExitDialog() {

        lyt_exit = findViewById(R.id.lyt_exit);
        lyt_dialog = findViewById(R.id.lyt_dialog);

        lyt_exit.setOnClickListener(v -> {
        });
        lyt_dialog.setOnClickListener(v -> {
        });

        findViewById(R.id.txt_cancel).setOnClickListener(v -> new Handler(Looper.getMainLooper()).postDelayed(() -> showExitDialog(false), 300));
        findViewById(R.id.txt_minimize).setOnClickListener(v -> {
                    showExitDialog(false);
                    new Handler(Looper.getMainLooper()).postDelayed(this::minimizeApp, 300);
                }
        );
        findViewById(R.id.txt_exit).setOnClickListener(v -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finish();
            if (isServiceRunning()) {
                Intent stop = new Intent(MainActivity.this, RadioPlayerService.class);
                stop.setAction(RadioPlayerService.ACTION_STOP);
                startService(stop);
                Log.d(TAG, "Radio service is running");
            } else {
                Log.d(TAG, "Radio service is not running");
            }
        }, 300));

    }

    public void minimizeApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RadioPlayerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void hideKeyboard() {
        try {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            if (((getCurrentFocus() != null) && ((getCurrentFocus().getWindowToken() != null)))) {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateConsentStatus() {
        if (BuildConfig.DEBUG) {
            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA)
                    .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID")
                    .build();
            ConsentRequestParameters params = new ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build();
            consentInformation = UserMessagingPlatform.getConsentInformation(this);
            consentInformation.requestConsentInfoUpdate(this, params, () -> {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    },
                    formError -> {
                    });
        } else {
            ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
            consentInformation = UserMessagingPlatform.getConsentInformation(this);
            consentInformation.requestConsentInfoUpdate(this, params, () -> {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    },
                    formError -> {
                    });
        }
    }

    public void loadForm() {
        UserMessagingPlatform.loadConsentForm(this, consentForm -> {
                    MainActivity.this.consentForm = consentForm;
                    if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                        consentForm.show(MainActivity.this, formError -> {
                            loadForm();
                        });
                    }
                },
                formError -> {
                }
        );
    }

    public void aboutDialog() {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(MainActivity.this);
        View view = layoutInflaterAndroid.inflate(R.layout.custom_dialog_about, null);

        ((TextView) view.findViewById(R.id.txt_app_version)).setText(getString(R.string.sub_about_app_version) + " " + BuildConfig.VERSION_NAME);

        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setView(view);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.option_ok, (dialog, which) -> dialog.dismiss());
        alert.show();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        initComponent();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Constant.is_app_open = false;
        super.onDestroy();
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

}
