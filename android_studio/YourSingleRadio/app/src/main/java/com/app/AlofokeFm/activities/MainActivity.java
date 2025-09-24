package com.app.AlofokeFm.activities;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.AlofokeFm.BuildConfig;
import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.adapters.AdapterNavigation;
import com.app.AlofokeFm.database.dao.AppDatabase;
import com.app.AlofokeFm.database.dao.DAO;
import com.app.AlofokeFm.database.dao.RadioEntity;
import com.app.AlofokeFm.database.prefs.AdsPref;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.fragments.FragmentRadio;
import com.app.AlofokeFm.fragments.FragmentSettings;
import com.app.AlofokeFm.fragments.FragmentWebView;
import com.app.AlofokeFm.models.Radio;
import com.app.AlofokeFm.services.RadioPlayerService;
import com.app.AlofokeFm.utils.AdsManager;
import com.app.AlofokeFm.utils.Constant;
import com.app.AlofokeFm.utils.Tools;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.slider.Slider;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.makeramen.roundedimageview.RoundedImageView;
import com.solodroid.push.sdk.provider.OneSignalPush;

import java.util.ArrayList;
import java.util.List;

import es.claucookie.miniequalizerlibrary.EqualizerView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private final static String COLLAPSING_TOOLBAR_FRAGMENT_TAG = "";
    private final static String SELECTED_TAG = "selected_index";
    private static int selectedIndex;
    private final static int COLLAPSING_TOOLBAR = 0;
    ActionBarDrawerToggle actionBarDrawerToggle;
    private DrawerLayout drawerLayout;
    NavigationView navigationView;
    ProgressBar progressBar;

    RelativeLayout lytCoverImagePortrait;
    RoundedImageView imgRadioLargePortrait;
    RoundedImageView imgAlbumArtLargePortrait;

    RelativeLayout lytCoverImageLandscape;
    ShapeableImageView imgRadioLargeLandscape;
    ShapeableImageView imgAlbumArtLargeLandscape;

    ImageView imgMusicBackground;
    ImageView imgMusicBackgroundAlbumArt;
    ImageView imgVolume;
    ImageView imgTimer;
    ImageView imgTimerStop;
    MaterialButton fabPlayExpand;
    TextView txtRadioExpand, txtRadioMusicSong, txtSongExpand;
    SharedPref sharedPref;
    Handler handler = new Handler();
    FragmentManager fragmentManager;
    EqualizerView equalizerView;
    Tools tools;
    AdsPref adsPref;
    private DAO db;
    AdsManager adsManager;
    List<RadioEntity> radioEntities;
    ArrayList<Radio> radios;
    private AppUpdateManager appUpdateManager;
    TextView txtTimer;
    TextView txtMinutes;
    CountDownTimer mCountDownTimer;
    public static long minutes = 1;
    LinearLayout lytBannerAd;
    OnBackPressedDispatcher onBackPressedDispatcher;
    RecyclerView recyclerView;
    public static AlertDialog alertDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.darkStatusBar(this, true);
        setContentView(R.layout.activity_main);
        Tools.setNavigation(this);

        db = AppDatabase.getDb(this).get();
        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            adsManager.initializeAd();
            adsManager.updateConsentStatus();
            adsManager.loadBannerAd(Constant.BANNER_AD);
            adsManager.loadInterstitialAd(Constant.INTERSTITIAL_AD, adsPref.getInterstitialAdInterval());
        }, 100);

        if (Config.ENABLE_RTL_MODE) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        Constant.is_app_open = true;

        sharedPref = new SharedPref(this);
        sharedPref.setCheckSleepTime();
        tools = new Tools(this);

        navigationView = findViewById(R.id.navigation_view);
        drawerLayout = findViewById(R.id.drawer_layout);

        selectedIndex = COLLAPSING_TOOLBAR;
        getSupportFragmentManager().beginTransaction()
                .add(R.id.frame_layout, new FragmentRadio(), COLLAPSING_TOOLBAR_FRAGMENT_TAG)
                .commit();

        initView();
        initComponent();

        Tools.notificationHandler(this, getIntent());
        loadConfig();

        if (!BuildConfig.DEBUG) {
            appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
            inAppUpdate();
            tools.inAppReview(this);
        }

        new OneSignalPush.Builder(this).requestNotificationPermission();

        isPortrait(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE);

        handleOnBackPressed();
        loadNavigationMenu();
    }

    private void loadNavigationMenu() {

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        AdapterNavigation adapterNavigation = new AdapterNavigation(this, new ArrayList<>());
        adapterNavigation.setListData(db.getAllSocial());

        recyclerView.setAdapter(adapterNavigation);

        adapterNavigation.setOnItemClickListener((v, obj, position) -> {
            if (position == 0) {
                Log.d(TAG, "space for native ad");
            } else if (position == 1) {
                Log.d(TAG, "open home");
            } else if (position == 2) {
                openFragmentSettings();
                Log.d(TAG, "open settings");
            } else {
                if (obj.social_url.contains("?target=internal")) {
                    openFragmentWebView(obj.social_name, obj.social_url);
                } else if (obj.social_url.contains("?target=external")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(obj.social_url.trim())));
                }  else if (obj.social_url.contains("?target=custom-tabs")) {
                    Tools.openCustomTabs(this, obj.social_url);
                } else {
                    openFragmentWebView(obj.social_name, obj.social_url);
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START);
        });

    }

    public void openFragmentSettings() {
        FragmentSettings fragment = new FragmentSettings();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_up, 0, 0, R.anim.slide_down);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.fragment_container, fragment).addToBackStack("settings");
        transaction.commit();
    }

    public void openFragmentWebView(String title, String url) {
        FragmentWebView fragment = new FragmentWebView();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("url", url);
        fragment.setArguments(args);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_up, 0, 0, R.anim.slide_down);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.fragment_container, fragment).addToBackStack("webview");
        transaction.commit();
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

        fabPlayExpand.setOnClickListener(view -> {
            if (!tools.isNetworkAvailable()) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.internet_not_connected), Toast.LENGTH_SHORT).show();
            } else {
                Radio radio = radios.get(0);
                final Intent intent = new Intent(MainActivity.this, RadioPlayerService.class);

                if (RadioPlayerService.getInstance() != null) {
                    Radio playerCurrentRadio = RadioPlayerService.getInstance().getPlayingRadioStation();
                    if (playerCurrentRadio != null) {
                        if (radio.getRadio_id() != RadioPlayerService.getInstance().getPlayingRadioStation().getRadio_id()) {
                            RadioPlayerService.getInstance().initializeRadio(MainActivity.this, radio);
                            intent.setAction(RadioPlayerService.ACTION_PLAY);
                        } else {
                            intent.setAction(RadioPlayerService.ACTION_TOGGLE);
                        }
                    } else {
                        RadioPlayerService.getInstance().initializeRadio(MainActivity.this, radio);
                        intent.setAction(RadioPlayerService.ACTION_PLAY);
                    }
                } else {
                    RadioPlayerService.createInstance().initializeRadio(MainActivity.this, radio);
                    intent.setAction(RadioPlayerService.ACTION_PLAY);
                }
                startService(intent);

                if (!Constant.is_playing) {
                    adsManager.showInterstitialAd();
                }

            }
        });

        if (sharedPref.getAutoPlay().equals("true")) {
            if (tools.isNetworkAvailable()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> fabPlayExpand.performClick(), Constant.DELAY_PERFORM_CLICK);
            }
        }

    }

    private void initView() {

        fragmentManager = getSupportFragmentManager();

        txtMinutes = findViewById(R.id.txt_minutes);
        imgMusicBackground = findViewById(R.id.img_music_background);
        imgMusicBackgroundAlbumArt = findViewById(R.id.img_music_background_album_art);
        lytBannerAd = findViewById(R.id.lyt_banner_ad);

        equalizerView = findViewById(R.id.equalizer_view);
        progressBar = findViewById(R.id.progress_bar);

        txtTimer = findViewById(R.id.txt_timer);
        imgTimer = findViewById(R.id.img_timer);
        imgTimerStop = findViewById(R.id.img_timer_stop);

        lytCoverImagePortrait = findViewById(R.id.lyt_cover_image_portrait);
        imgRadioLargePortrait = findViewById(R.id.img_radio_large_portrait);
        imgAlbumArtLargePortrait = findViewById(R.id.img_album_art_large_portrait);

        lytCoverImageLandscape = findViewById(R.id.lyt_cover_image_landscape);
        imgRadioLargeLandscape = findViewById(R.id.img_radio_large_landscape);
        imgAlbumArtLargeLandscape = findViewById(R.id.img_album_art_large_landscape);

        imgVolume = findViewById(R.id.img_volume);
        fabPlayExpand = findViewById(R.id.fab_play);
        txtRadioExpand = findViewById(R.id.txt_radio_name_expand);
        txtSongExpand = findViewById(R.id.txt_metadata_expand);
    }

    public void initComponent() {

        if (Config.CIRCULAR_RADIO_IMAGE_ALBUM_ART) {
            imgRadioLargePortrait.setOval(true);
            imgAlbumArtLargePortrait.setOval(true);
        } else {
            imgRadioLargePortrait.setOval(false);
            imgAlbumArtLargePortrait.setOval(false);
        }
        txtSongExpand.setSelected(true);

        if (!tools.isNetworkAvailable()) {
            txtRadioExpand.setText(getResources().getString(R.string.app_name));
            txtSongExpand.setText(getResources().getString(R.string.internet_not_connected));
        }

        setIfPlaying();

        imgTimer.setOnClickListener(v -> {
            showBannerAd(false);

            View view = getLayoutInflater().inflate(R.layout.dialog_timer, null);

            TextView txtMinutes = view.findViewById(R.id.txt_minutes);
            txtMinutes.setText(minutes + " " + getString(R.string.min));

            Slider seekBar = view.findViewById(R.id.seek_bar_timer);
            seekBar.setValueFrom(1);
            seekBar.setValueTo(180);
            seekBar.setValue(minutes);
            seekBar.addOnChangeListener((slider, value, fromUser) -> {
                int min = (int) value;
                txtMinutes.setText(min + " " + getString(R.string.min));
                minutes = min;
            });

            final MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
            alert.setView(view);
            AlertDialog alertDialog = alert.create();
            Tools.dialogButtonSelected(this, view, alertDialog, () -> {
                startTimer(minutes);
            });
            alertDialog.setOnDismissListener(dialogInterface -> showBannerAd(true));
            alertDialog.show();
        });

        imgTimerStop.setOnClickListener(v -> stopTimer());

        imgVolume.setOnClickListener(v -> tools.changeVolume(this, imgVolume));

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAG, selectedIndex);
    }

    public void setupNavigationDrawer(Toolbar toolbar) {
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                showBannerAd(false);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                showBannerAd(true);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    public void changePlayPause(Boolean flag) {
        Constant.is_playing = flag;
        if (flag) {
            Radio radio = RadioPlayerService.getInstance().getPlayingRadioStation();
            if (radio != null) {
                changeText(radio);
                fabPlayExpand.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_action_pause));
                equalizerView.animateBars();
            }
        } else {
            if (Constant.item_radio.size() > 0) {
                changeText(Constant.item_radio.get(Constant.position));
            }
            fabPlayExpand.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_action_play));
            equalizerView.stopBars();

            imgAlbumArtLargePortrait.setVisibility(View.GONE);
            imgAlbumArtLargeLandscape.setVisibility(View.GONE);

            imgMusicBackgroundAlbumArt.setVisibility(View.GONE);
        }
    }

    public void showImageAlbumArt(boolean show) {
        if (show) {
            imgAlbumArtLargePortrait.setVisibility(View.VISIBLE);
            imgAlbumArtLargeLandscape.setVisibility(View.VISIBLE);
            imgMusicBackgroundAlbumArt.setVisibility(View.VISIBLE);
        } else {
            imgAlbumArtLargePortrait.setVisibility(View.GONE);
            imgAlbumArtLargeLandscape.setVisibility(View.GONE);
            imgMusicBackgroundAlbumArt.setVisibility(View.GONE);
        }
    }

    public void changeText(Radio radio) {
        if (Constant.radio_type) {
            changeSongName(Constant.metadata);

            if (Constant.metadata == null || Constant.metadata.equals(radio.getRadio_genre())) {
                imgAlbumArtLargePortrait.setVisibility(View.GONE);
                imgAlbumArtLargeLandscape.setVisibility(View.GONE);
            } else {
                imgAlbumArtLargePortrait.setVisibility(View.VISIBLE);
                imgAlbumArtLargeLandscape.setVisibility(View.VISIBLE);
            }

            txtSongExpand.setVisibility(View.VISIBLE);
        } else {
            txtRadioMusicSong.setText("");
            txtSongExpand.setText(radio.getRadio_name());
            txtSongExpand.setVisibility(View.INVISIBLE);
        }
        txtRadioExpand.setText(radio.getRadio_name());

        if (!Constant.is_playing) {
            txtSongExpand.setText(radio.getRadio_genre());
        }

        if (sharedPref.getBlurRadioBackground().equals("true")) {
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(radio.getBackground_image_url().replace(" ", "%20"))
                    .placeholder(R.drawable.ic_thumbnail)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                            Bitmap blurImage = Tools.blurImage(MainActivity.this, bitmap);
                            imgMusicBackground.setImageBitmap(blurImage);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        } else {
            Glide.with(getApplicationContext())
                    .load(radio.getBackground_image_url().replace(" ", "%20"))
                    .placeholder(R.drawable.ic_thumbnail)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imgMusicBackground);
        }

        Glide.with(getApplicationContext())
                .load(radio.getRadio_image_url().replace(" ", "%20"))
                .placeholder(R.drawable.ic_artwork)
                .into(imgRadioLargePortrait);

        Glide.with(getApplicationContext())
                .load(radio.getRadio_image_url().replace(" ", "%20"))
                .placeholder(R.drawable.ic_artwork)
                .into(imgRadioLargeLandscape);
    }

    public void changeSongName(String songName) {
        Constant.metadata = songName;
        txtSongExpand.setText(songName);
    }

    public void changeAlbumArt(String artworkUrl) {
        Constant.albumArt = artworkUrl;
        Glide.with(getApplicationContext())
                .load(artworkUrl.replace(" ", "%20"))
                .placeholder(android.R.color.transparent)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                        imgAlbumArtLargePortrait.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                        imgAlbumArtLargePortrait.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(imgAlbumArtLargePortrait);

        Glide.with(getApplicationContext())
                .load(artworkUrl.replace(" ", "%20"))
                .placeholder(android.R.color.transparent)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                        imgAlbumArtLargeLandscape.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                        imgAlbumArtLargeLandscape.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(imgAlbumArtLargeLandscape);

        if (sharedPref.getDynamicAlbumArtBackground().equals("true")) {
            if (sharedPref.getBlurRadioBackground().equals("true")) {
                Glide.with(getApplicationContext())
                        .asBitmap()
                        .load(artworkUrl.replace(" ", "%20"))
                        .placeholder(android.R.color.transparent)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                                imgMusicBackgroundAlbumArt.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(@NonNull Bitmap resource, @NonNull Object model, Target<Bitmap> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                imgMusicBackgroundAlbumArt.setVisibility(View.VISIBLE);
                                return false;
                            }
                        })
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                                Bitmap blurImage = Tools.blurImage(MainActivity.this, bitmap);
                                imgMusicBackgroundAlbumArt.setImageBitmap(blurImage);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });
            } else {
                Glide.with(getApplicationContext())
                        .load(artworkUrl.replace(" ", "%20"))
                        .placeholder(android.R.color.transparent)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                                imgMusicBackgroundAlbumArt.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                imgMusicBackgroundAlbumArt.setVisibility(View.VISIBLE);
                                return false;
                            }
                        })
                        .into(imgMusicBackgroundAlbumArt);
            }
        }
    }

    public void setIfPlaying() {
        if (RadioPlayerService.getInstance() != null) {
            RadioPlayerService.initialize(MainActivity.this);
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

    public void handleOnBackPressed() {
        onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int count = getSupportFragmentManager().getBackStackEntryCount();
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (count != 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    showExitDialog();
                }
            }
        });
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

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(Constant.BANNER_AD);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Constant.is_app_open = false;
        Constant.isAppOpen = false;
        Constant.isRadioPlaying = false;
        adsManager.destroyBannerAd();
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    private void inAppUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdateFlow(appUpdateInfo);
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo);
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void startUpdateFlow(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, Constant.IMMEDIATE_APP_UPDATE_REQ_CODE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.IMMEDIATE_APP_UPDATE_REQ_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), getString(R.string.msg_cancel_update), Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), getString(R.string.msg_success_update), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.msg_failed_update), Toast.LENGTH_SHORT).show();
                inAppUpdate();
            }
        }
    }

    @SuppressLint("InflateParams")
    private void showExitDialog() {
        showBannerAd(false);
        View view;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            view = getLayoutInflater().inflate(R.layout.dialog_exit_landscape, null);
        } else {
            view = getLayoutInflater().inflate(R.layout.dialog_exit_portrait, null);
        }

        final MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
        alert.setView(view);

        FloatingActionButton btnRate = view.findViewById(R.id.btn_rate);
        FloatingActionButton btnShare = view.findViewById(R.id.btn_share);

        LinearLayout nativeAdView = view.findViewById(R.id.native_ad_view);
        Tools.setNativeAdStyle(MainActivity.this, nativeAdView, Constant.NATIVE_AD_STYLE_EXIT_DIALOG);
        adsManager.loadNativeAdView(view, Constant.NATIVE_AD_EXIT_DIALOG, Constant.NATIVE_AD_STYLE_EXIT_DIALOG);

        alertDialog = alert.create();
        Tools.dialogExitButtonSelected(this, view, alertDialog, () -> {
            finish();
            adsManager.destroyBannerAd();
            if (isServiceRunning()) {
                Intent stop = new Intent(this, RadioPlayerService.class);
                stop.setAction(RadioPlayerService.ACTION_STOP);
                startService(stop);
                Log.d(TAG, "Service Running");
            } else {
                Log.d(TAG, "Service Not Running");
            }
        }, this::minimizeApp);
        alertDialog.setOnDismissListener(dialogInterface -> showBannerAd(true));

        btnRate.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
            alertDialog.dismiss();
        });

        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_content) + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            intent.setType("text/plain");
            startActivity(intent);
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private void startTimer(long minutes) {
        setCountDownTimer(minutes * 60);
        showCountDownTimer(true);
        mCountDownTimer.start();
    }

    private void stopTimer() {
        mCountDownTimer.cancel();
        showCountDownTimer(false);
        txtTimer.setText("00:00:00");
    }

    private void setCountDownTimer(long seconds) {
        mCountDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                txtTimer.setText(Tools.formatSeconds(millisUntilFinished / 1000));
                if (millisUntilFinished > 0) {
                    showCountDownTimer(true);
                } else {
                    showCountDownTimer(false);
                }
                Log.d(TAG, "seconds remaining: " + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                txtTimer.setText("00:00:00");
                showCountDownTimer(false);
                if (isServiceRunning()) {
                    Intent stop = new Intent(MainActivity.this, RadioPlayerService.class);
                    stop.setAction(RadioPlayerService.ACTION_STOP);
                    startService(stop);
                }
            }
        };
    }

    private void showCountDownTimer(boolean show) {
        if (show) {
            imgTimer.setVisibility(View.GONE);
            imgTimerStop.setVisibility(View.VISIBLE);
            txtTimer.setVisibility(View.VISIBLE);
        } else {
            imgTimer.setVisibility(View.VISIBLE);
            imgTimerStop.setVisibility(View.GONE);
            txtTimer.setVisibility(View.GONE);
        }
    }

    private void showBannerAd(boolean show) {
        if (show) {
            lytBannerAd.setVisibility(View.VISIBLE);
        } else {
            lytBannerAd.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        isPortrait(newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE);
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    private void isPortrait(boolean isPortrait) {
        if (isPortrait) {
            lytCoverImagePortrait.setVisibility(View.VISIBLE);
            lytCoverImageLandscape.setVisibility(View.GONE);
        } else {
            lytCoverImagePortrait.setVisibility(View.GONE);
            lytCoverImageLandscape.setVisibility(View.VISIBLE);
        }
    }

}