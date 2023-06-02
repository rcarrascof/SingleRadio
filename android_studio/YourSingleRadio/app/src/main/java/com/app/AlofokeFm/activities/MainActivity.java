package com.app.AlofokeFm.activities;

import static com.app.AlofokeFm.utils.Constant.BANNER_AD;
import static com.app.AlofokeFm.utils.Constant.INTERSTITIAL_AD;
import static com.app.AlofokeFm.utils.Constant.NATIVE_AD;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
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
import android.view.animation.AnimationUtils;
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
import com.app.AlofokeFm.utils.AdsManager;
import com.app.AlofokeFm.utils.Constant;
import com.app.AlofokeFm.utils.RelativePopupWindow;
import com.app.AlofokeFm.utils.SleepTimeReceiver;
import com.app.AlofokeFm.utils.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
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
    RoundedImageView imgRadioLarge;
    RoundedImageView imgAlbumArtLarge;
    ImageView imgMusicBackground;
    ImageView imgMusicBackgroundAlbumArt;
    ImageButton imgVolume;
    ImageButton imgTimer;
    MaterialButton fabPlayExpand;
    TextView txtRadioExpand, txtRadioMusicSong, txtSongExpand;
    SharedPref sharedPref;
    Handler handler = new Handler();
    FragmentManager fragmentManager;
    EqualizerView equalizerView;
    Utils utils;
    AdsPref adsPref;
    private DAO db;
    AdsManager adsManager;
    List<RadioEntity> radioEntities;
    ArrayList<Radio> radios;
    private AppUpdateManager appUpdateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.darkStatusBar(this, true);
        adsPref = new AdsPref(this);
        setContentView(R.layout.activity_main);
        db = AppDatabase.getDb(this).get();

        adsManager = new AdsManager(this);
        adsManager.initializeAd();
        adsManager.updateConsentStatus();
        adsManager.loadBannerAd(BANNER_AD);
        adsManager.loadInterstitialAd(INTERSTITIAL_AD, adsPref.getInterstitialAdInterval());
        adsManager.loadNativeAd(NATIVE_AD);

        if (Config.ENABLE_RTL_MODE) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        Constant.is_app_open = true;

        sharedPref = new SharedPref(this);
        sharedPref.setCheckSleepTime();
        utils = new Utils(this);

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
        Utils.notificationHandler(this, getIntent());
        loadConfig();

        if (!BuildConfig.DEBUG) {
            appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
            inAppUpdate();
            inAppReview();
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

        fabPlayExpand.setOnClickListener(view -> {
            if (!utils.isNetworkAvailable()) {
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
            if (utils.isNetworkAvailable()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> fabPlayExpand.performClick(), Constant.DELAY_PERFORM_CLICK);
            }
        }

    }

    public void initComponent() {

        fragmentManager = getSupportFragmentManager();

        imgMusicBackground = findViewById(R.id.img_music_background);
        imgMusicBackgroundAlbumArt = findViewById(R.id.img_music_background_album_art);

        equalizerView = findViewById(R.id.equalizer_view);
        progressBar = findViewById(R.id.progress_bar);
        imgTimer = findViewById(R.id.img_timer);

        imgRadioLarge = findViewById(R.id.img_radio_large);
        imgAlbumArtLarge = findViewById(R.id.img_album_art_large);

        if (Config.CIRCULAR_RADIO_IMAGE_ALBUM_ART) {
            imgRadioLarge.setOval(true);
            imgAlbumArtLarge.setOval(true);
        } else {
            imgRadioLarge.setOval(false);
            imgAlbumArtLarge.setOval(false);
        }

        imgVolume = findViewById(R.id.img_volume);
        fabPlayExpand = findViewById(R.id.fab_play);
        txtRadioExpand = findViewById(R.id.txt_radio_name_expand);
        txtSongExpand = findViewById(R.id.txt_metadata_expand);
        txtSongExpand.setSelected(true);

        if (!utils.isNetworkAvailable()) {
            txtRadioExpand.setText(getResources().getString(R.string.app_name));
            txtSongExpand.setText(getResources().getString(R.string.internet_not_connected));
        }

        setIfPlaying();

        imgTimer.setOnClickListener(v -> {
            if (sharedPref.getIsSleepTimeOn()) {
                openTimeDialog();
            } else {
                openTimeSelectDialog();
            }
        });

        imgVolume.setOnClickListener(v -> changeVolume());

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
                    .setCustomAnimations(R.anim.slide_up, 0, 0, R.anim.slide_down);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(android.R.id.content, fragmentSocial).addToBackStack("social");
            transaction.commit();
            Utils.darkStatusBar(this, false);
            drawerLayout.closeDrawer(GravityCompat.START);
            hideKeyboard();

            return true;
        } else if (itemId == R.id.drawer_rate) {
            final String package_name = BuildConfig.APPLICATION_ID;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + package_name)));
            } catch (ActivityNotFoundException e) {
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
                    .setCustomAnimations(R.anim.slide_up, 0, 0, R.anim.slide_down);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(android.R.id.content, fragmentWebView).addToBackStack("page");
            transaction.commit();
            Utils.darkStatusBar(this, false);
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
       /* }else if(itemId == R.id.drawer_chat)
        {
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(i);
            return true;

        }*/
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
                fabPlayExpand.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_button_pause));
                equalizerView.animateBars();
            }
        } else {
            if (Constant.item_radio.size() > 0) {
                changeText(Constant.item_radio.get(Constant.position));
            }
            fabPlayExpand.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_button_play));
            equalizerView.stopBars();
            imgAlbumArtLarge.setVisibility(View.GONE);
            imgMusicBackgroundAlbumArt.setVisibility(View.GONE);
        }
    }

    public void showImageAlbumArt(boolean show) {
        if (show) {
            imgAlbumArtLarge.setVisibility(View.VISIBLE);
            imgMusicBackgroundAlbumArt.setVisibility(View.VISIBLE);
        } else {
            imgAlbumArtLarge.setVisibility(View.GONE);
            imgMusicBackgroundAlbumArt.setVisibility(View.GONE);
        }
    }

    public void changeText(Radio radio) {
        if (Constant.radio_type) {
            changeSongName(Constant.metadata);

            if (Constant.metadata == null || Constant.metadata.equals(radio.getRadio_genre())) {
                imgAlbumArtLarge.setVisibility(View.GONE);
                //imgMusicBackgroundAlbumArt.setVisibility(View.GONE);
            } else {
                imgAlbumArtLarge.setVisibility(View.VISIBLE);
                //imgMusicBackgroundAlbumArt.setVisibility(View.VISIBLE);
            }

            txtSongExpand.setVisibility(View.VISIBLE);
            imgTimer.setVisibility(View.VISIBLE);
        } else {
            txtRadioMusicSong.setText("");
            txtSongExpand.setText(radio.getRadio_name());
            txtSongExpand.setVisibility(View.INVISIBLE);
            imgTimer.setVisibility(View.GONE);
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
                            Bitmap blurImage = Utils.blurImage(MainActivity.this, bitmap);
                            imgMusicBackground.setImageBitmap(blurImage);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        } /*else {
            Glide.with(getApplicationContext())
                    .load(radio.getBackground_image_url().replace(" ", "%20"))
                    .placeholder(R.drawable.ic_thumbnail)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imgMusicBackground);
        }

        Glide.with(getApplicationContext())
                .load(radio.getRadio_image_url().replace(" ", "%20"))
                .placeholder(R.drawable.ic_artwork)
                .into(imgRadioLarge);*/
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
                .thumbnail(0.3f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        imgAlbumArtLarge.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        imgAlbumArtLarge.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(imgAlbumArtLarge);

        if (sharedPref.getDynamicAlbumArtBackground().equals("true")) {
            if (sharedPref.getBlurRadioBackground().equals("true")) {
                Glide.with(getApplicationContext())
                        .asBitmap()
                        .load(artworkUrl.replace(" ", "%20"))
                        .placeholder(android.R.color.transparent)
                        .thumbnail(0.3f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                imgMusicBackgroundAlbumArt.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                imgMusicBackgroundAlbumArt.setVisibility(View.VISIBLE);
                                return false;
                            }
                        })
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                                Bitmap blurImage = Utils.blurImage(MainActivity.this, bitmap);
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
                        .thumbnail(0.3f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                imgMusicBackgroundAlbumArt.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
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
        popupWindow.showOnAnchor(imgVolume, RelativePopupWindow.VerticalPosition.ABOVE, RelativePopupWindow.HorizontalPosition.CENTER);
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
            long total_timer = utils.convertToMilliSeconds(totalTime) + System.currentTimeMillis();

            Random random = new Random();
            int id = random.nextInt(100);

            sharedPref.setSleepTime(true, total_timer, id);

            int FLAG;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                FLAG = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT;
            } else {
                FLAG = PendingIntent.FLAG_ONE_SHOT;
            }

            Intent intent = new Intent(MainActivity.this, SleepTimeReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), id, intent, FLAG);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            assert alarmManager != null;
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, total_timer, pendingIntent);
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
            int FLAG;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                FLAG = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT;
            } else {
                FLAG = PendingIntent.FLAG_ONE_SHOT;
            }
            Intent i = new Intent(MainActivity.this, SleepTimeReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, sharedPref.getSleepID(), i, FLAG);
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
        long timeLeft = time - System.currentTimeMillis();
        if (timeLeft > 0) {
            @SuppressLint("DefaultLocale") String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeLeft),
                    TimeUnit.MILLISECONDS.toMinutes(timeLeft) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(timeLeft) % TimeUnit.MINUTES.toSeconds(1));

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
                Utils.darkStatusBar(this, true);
            }
        } else {
            showExitDialog();
        }
    }

    public void showExitDialog() {

        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.custom_dialog_exit, null);

            adsManager.loadNativeAdView(view, 1);

            final AlertDialog.Builder dialog;
            dialog = new AlertDialog.Builder(this);
            dialog.setView(view);
            dialog.setCancelable(false);
            dialog.setPositiveButton(getResources().getString(R.string.dialog_option_quit), (dialogInterface, i) -> {
                finish();
                adsManager.destroyBannerAd();
                if (isServiceRunning()) {
                    Intent stop = new Intent(this, RadioPlayerService.class);
                    stop.setAction(RadioPlayerService.ACTION_STOP);
                    startService(stop);
                    Log.d("RADIO_SERVICE", "Service Running");
                } else {
                    Log.d("RADIO_SERVICE", "Service Not Running");
                }
            });

            dialog.setNegativeButton(getResources().getString(R.string.dialog_option_minimize), (dialogInterface, i) -> minimizeApp());

            dialog.setNeutralButton(getResources().getString(R.string.cancel), (dialogInterface, i) -> {

            });
            dialog.show();

        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setIcon(R.drawable.logo);
            dialog.setTitle(R.string.app_name);
            dialog.setMessage(getResources().getString(R.string.message));
            dialog.setPositiveButton(getResources().getString(R.string.dialog_option_quit), (dialogInterface, i) -> {
                finish();
                adsManager.destroyBannerAd();
                if (isServiceRunning()) {
                    Intent stop = new Intent(MainActivity.this, RadioPlayerService.class);
                    stop.setAction(RadioPlayerService.ACTION_STOP);
                    startService(stop);
                    Log.d("RADIO_SERVICE", "Service Running");
                } else {
                    Log.d("RADIO_SERVICE", "Service Not Running");
                }
            });

            dialog.setNegativeButton(getResources().getString(R.string.dialog_option_minimize), (dialogInterface, i) -> minimizeApp());

            dialog.setNeutralButton(getResources().getString(R.string.cancel), (dialogInterface, i) -> {

            });
            dialog.show();
        }

    }

//    public void showExitDialog(boolean exit) {
//        if (exit) {
//            if (lytExit.getVisibility() != View.VISIBLE) {
//                lytExit.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up));
//            }
//            lytExit.setVisibility(View.VISIBLE);
//        } else {
//            lytExit.clearAnimation();
//            lytExit.setVisibility(View.GONE);
//        }
//    }

//    public void initExitDialog() {
//
//        lytExit = findViewById(R.id.lyt_exit);
//        lytDialog = findViewById(R.id.lyt_dialog);
//
//        lytExit.setOnClickListener(v -> {
//        });
//        lytDialog.setOnClickListener(v -> {
//        });
//
//        findViewById(R.id.txt_cancel).setOnClickListener(v -> new Handler(Looper.getMainLooper()).postDelayed(() -> showExitDialog(false), 200));
//        findViewById(R.id.txt_minimize).setOnClickListener(v -> {
//                    showExitDialog(false);
//                    new Handler(Looper.getMainLooper()).postDelayed(this::minimizeApp, 200);
//                }
//        );
//        findViewById(R.id.txt_exit).setOnClickListener(v -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            finish();
//            adsManager.destroyBannerAd();
//            if (isServiceRunning()) {
//                Intent stop = new Intent(MainActivity.this, RadioPlayerService.class);
//                stop.setAction(RadioPlayerService.ACTION_STOP);
//                startService(stop);
//                Log.d(TAG, "Radio service is running");
//            } else {
//                Log.d(TAG, "Radio service is not running");
//            }
//        }, 200));
//
//    }

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

    public void aboutDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View view = layoutInflater.inflate(R.layout.custom_dialog_about, null);
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
        super.onResume();
        initComponent();
        adsManager.resumeBannerAd(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Constant.is_app_open = false;
        adsManager.destroyBannerAd();
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    private void inAppReview() {
        if (sharedPref.getInAppReviewToken() <= 3) {
            sharedPref.updateInAppReviewToken(sharedPref.getInAppReviewToken() + 1);
            Log.d(TAG, "in app update token");
        } else {
            ReviewManager manager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(MainActivity.this, reviewInfo).addOnFailureListener(e -> {
                    }).addOnCompleteListener(complete -> Log.d(TAG, "Success")
                    ).addOnFailureListener(failure -> Log.d(TAG, "Rating Failed"));
                }
            }).addOnFailureListener(failure -> Log.d(TAG, "In-App Request Failed " + failure));
            Log.d(TAG, "in app token complete, show in app review if available");
        }
        Log.d(TAG, "in app review token : " + sharedPref.getInAppReviewToken());
    }

    private void inAppUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdateFlow(appUpdateInfo);
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo);
            }
        });
    }

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

}
