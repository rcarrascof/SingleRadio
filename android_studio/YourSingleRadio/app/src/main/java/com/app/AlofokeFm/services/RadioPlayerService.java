package com.app.AlofokeFm.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.AssetDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.extractor.metadata.icy.IcyInfo;
import androidx.media3.extractor.metadata.id3.TextInformationFrame;

import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.activities.MainActivity;
import com.app.AlofokeFm.callbacks.CallbackAlbumArt;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.models.AlbumArt;
import com.app.AlofokeFm.models.Radio;
import com.app.AlofokeFm.rests.RestAdapter;
import com.app.AlofokeFm.services.parser.ParserM3UToURL;
import com.app.AlofokeFm.utils.AsyncTaskExecutor;
import com.app.AlofokeFm.utils.Constant;
import com.app.AlofokeFm.utils.HttpsTrustManager;
import com.app.AlofokeFm.utils.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("StaticFieldLeak")
@SuppressWarnings({"deprecation", "CallToPrintStackTrace"})
public class RadioPlayerService extends Service implements AudioFocusChangedCallback {

    public static final String TAG = "RadioService";
    private MediaSessionCompat mediaSessionCompat;
    MediaControllerCompat mediaControllerCompat;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaSessionCompat.Callback callback;
    Call<CallbackAlbumArt> callbackCall = null;
    LoadSong loadSong;
    PlaybackStateCompat playbackState;
    NotificationCompat.Builder builder;
    static NotificationManager notificationManager;
    private BroadcastReceiver broadcastReceiver;
//    DefaultBandwidthMeter.Builder bandwidthMeter;
    static SharedPref sharedPref;
    Bitmap bitmap;
    private Boolean isCanceled = false;
    boolean isCounterRunning = false;
    static RadioPlayerService service;
    static Context context;
    static Radio radio;
    ComponentName componentName;
    AudioManager mAudioManager;
    PowerManager.WakeLock mWakeLock;
//    Tools tools;
    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = Tools.getApplicationId();
    public static final String ACTION_TOGGLE = Tools.getApplicationId() + ".togglepause";
    public static final String ACTION_PLAY = Tools.getApplicationId() + ".play";
    public static final String ACTION_NEXT = Tools.getApplicationId() + ".next";
    public static final String ACTION_PREVIOUS = Tools.getApplicationId() + ".prev";
    public static final String ACTION_STOP = Tools.getApplicationId() + ".stop";
    public static final String MEDIA_SESSION_TAG = "MEDIA_SESSION";
    MediaMetadata mediaMetadata;

    static public void initialize(Context context) {
        RadioPlayerService.context = context;
        RadioPlayerService.sharedPref = new SharedPref(context);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void initializeRadio(Context context, Radio station) {
        RadioPlayerService.context = context;
        RadioPlayerService.sharedPref = new SharedPref(context);
        radio = station;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static RadioPlayerService getInstance() {
        return service;
    }

    public static RadioPlayerService createInstance() {
        if (service == null) {
            service = new RadioPlayerService();
        }
        return service;
    }

    public Boolean isPlaying() {
        if (service == null) {
            return false;
        } else {
            if (Constant.exoPlayer != null) {
                return Constant.exoPlayer.getPlayWhenReady();
            } else {
                return false;
            }
        }
    }

    @SuppressLint({"UnspecifiedImmutableFlag", "UnspecifiedRegisterReceiverFlag"})
    @Override
    public void onCreate() {
        super.onCreate();
//        tools = new Tools(context);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        componentName = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(componentName);

        LocalBroadcastManager.getInstance(this).registerReceiver(onCallIncome, new IntentFilter("android.intent.action.PHONE_STATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(onHeadPhoneDetect, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        Constant.exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
        Constant.exoPlayer.addListener(listener);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.setReferenceCounted(false);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
        Constant.exoPlayer.setAudioAttributes(audioAttributes, true);

        stateBuilder = new PlaybackStateCompat.Builder();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleCommand(intent);
            }
        };

        MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                if (state.getState() == PlaybackStateCompat.STATE_PAUSED || state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    if (builder != null) {
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                    }
                }
                if (state.getState() == PlaybackStateCompat.STATE_PAUSED) {
                    //stopForeground(true);
                    Log.d(TAG, "State Paused : stopForeground disabled");
                } else if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    if (builder != null) {
                        startForeground(NOTIFICATION_ID, builder.build());
                        Log.d(TAG, "State Playing : startForeground");
                    }
                }
            }

            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                if (builder != null) {
                    notificationManager.notify(NOTIFICATION_ID, builder.build());
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_TOGGLE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED);
        } else {
            this.registerReceiver(broadcastReceiver, filter);
        }

        callback = new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                mediaSessionCompat.setActive(true);
                newPlay();
            }

            @Override
            public void onPause() {
                togglePlayPause();
                if (Constant.exoPlayer.getPlayWhenReady()) {
                    mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_PAUSED));
                } else {
                    mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_PLAYING));
                }
            }

            @Override
            public void onSkipToNext() {
                next();
                mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT));
            }

            @Override
            public void onSkipToPrevious() {
                previous();
                mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS));
            }

            @Override
            public void onStop() {
                mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_STOPPED));
                mediaSessionCompat.setActive(false);
                stop(false);
            }

            @Override
            public void onSkipToQueueItem(long id) {
                mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM));
                onPlay();
            }

            @Override
            public void onSeekTo(long pos) {
                mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_BUFFERING));
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                KeyEvent mediaEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (mediaEvent != null && mediaEvent.getAction() == KeyEvent.ACTION_UP) {
                    int keyCode = mediaEvent.getKeyCode();
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            onSkipToNext();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            onSkipToPrevious();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            onPause();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STOP:
                            if (isPlaying()) {
                                new Handler(Looper.getMainLooper()).postDelayed(() -> stop(false), 2000);
                                pause();
                            } else {
                                stop(false);
                            }
                            break;
                    }
                }
                return true;
            }
        };

        mediaSessionCompat = new MediaSessionCompat(this, MEDIA_SESSION_TAG);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            mediaSessionCompat.setCallback(callback);
            mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_NONE));
            mediaControllerCompat = new MediaControllerCompat(this, mediaSessionCompat);
            mediaControllerCompat.registerCallback(controllerCallback);
        }

    }

    @Override
    public void onTaskRemoved(Intent intent) {
        super.onTaskRemoved(intent);
        if (isPlaying()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                ((MainActivity) context).finish();
                stop(false);
            }, 2000);
            pause();
        } else {
            ((MainActivity) context).finish();
            stop(false);
        }
        Constant.isRadioPlaying = false;
//        stopCountDownTimerMetadata();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null)
            try {
                handleCommand(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "error " + e.getMessage());
            }
        return START_NOT_STICKY;
    }

    private void handleCommand(Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(ACTION_TOGGLE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    togglePlayPause();
                } else {
                    callback.onPause();
                }
            } else if (action.equals(ACTION_NEXT)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    next();
                } else {
                    callback.onSkipToNext();
                }
            } else if (action.equals(ACTION_PREVIOUS)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    previous();
                } else {
                    callback.onSkipToPrevious();
                }
            } else if (action.equals(ACTION_PLAY)) {
                newPlay();
            } else if (action.equals(ACTION_STOP)) {
                if (isPlaying()) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> callback.onStop(), 2000);
                    pause();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        stop(false);
                    } else {
                        callback.onStop();
                    }
                }
            }
        }
    }

    public class LoadSong extends AsyncTaskExecutor<Void, Void, Void> {

        MediaSource mediaSource;
        List<MediaSource> mediaItemsSource;

        @Override
        protected void onPreExecute() {
            ((MainActivity) context).setBuffer(true);
            ((MainActivity) context).changeSongName(Constant.item_radio.get(Constant.position).radio_genre);
            updateNotificationMetadata(Constant.item_radio.get(Constant.position).radio_genre);
            onMediaMetadataCompatChanged(Constant.item_radio.get(Constant.position).radio_genre);
            updateNotificationAlbumArt(Constant.item_radio.get(Constant.position).radio_image_url);
        }

        @OptIn(markerClass = UnstableApi.class)
        @Override
        protected Void doInBackground(Void params) {
            try {
                HttpsTrustManager.allowAllSSL();
                String url = Constant.item_radio.get(Constant.position).radio_url;

                DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setKeepPostFor302Redirects(true)
                        .setUserAgent(Util.getUserAgent(getApplicationContext(), getString(R.string.app_name)));

                mediaItemsSource = new ArrayList<>();
                mediaMetadata = getMediaMetadata(Constant.item_radio.get(Constant.position).radio_genre);

                if (url.startsWith("http") || url.startsWith("https")) {
                    if (url.contains(".m3u8")) {
                        MediaItem mediaItem = new MediaItem.Builder().setUri(url).setMediaMetadata(mediaMetadata).build();
                        mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
                    } else if (url.contains(".m3u") || url.contains("yp.shoutcast.com/sbin/tunein-station.m3u?id=")) {
                        url = ParserM3UToURL.parse(url, "m3u");
                        MediaItem mediaItem = new MediaItem.Builder().setUri(url).setMediaMetadata(mediaMetadata).build();
                        mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
                    } else if (url.contains(".pls") || url.contains("listen.pls?sid=") || url.contains("yp.shoutcast.com/sbin/tunein-station.pls?id=")) {
                        url = ParserM3UToURL.parse(url, "pls");
                        MediaItem mediaItem = new MediaItem.Builder().setUri(url).setMediaMetadata(mediaMetadata).build();
                        mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
                    } else {
                        MediaItem mediaItem = new MediaItem.Builder().setUri(url).setMediaMetadata(mediaMetadata).build();
                        mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
                    }
                    Log.d(TAG, "from url");
                } else {
                    AssetDataSource assetDataSource = new AssetDataSource(context);
                    DataSpec dataSpec = new DataSpec(Uri.parse("asset:///" + url));
                    assetDataSource.open(dataSpec);
                    MediaItem mediaItem = new MediaItem.Builder().setUri(dataSpec.uri).setMediaMetadata(mediaMetadata).build();
                    mediaSource = new ProgressiveMediaSource.Factory(() -> assetDataSource).createMediaSource(mediaItem);
                    Log.d(TAG, "from assets");
                }

                mediaItemsSource.add(mediaSource);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return params;
        }

        @OptIn(markerClass = UnstableApi.class)
        @Override
        protected void onPostExecute(Void result) {
            if (context != null && mediaItemsSource != null && !mediaItemsSource.isEmpty()) {
                Constant.exoPlayer.setMediaSources(mediaItemsSource);
                Constant.exoPlayer.seekTo(Constant.exoPlayer.getCurrentWindowIndex(), Constant.exoPlayer.getCurrentPosition());
                Constant.exoPlayer.prepare();
                Constant.exoPlayer.setPlayWhenReady(true);
            }
        }

    }

    public MediaMetadata getMediaMetadata(String metadata) {
        String description = "Media description for item";
        return new MediaMetadata.Builder()
                .setTitle(Constant.item_radio.get(Constant.position).radio_name)
                .setSubtitle(metadata)
                .setDescription(description + " " + Constant.item_radio.get(Constant.position).radio_name)
                .build();
    }

    Player.Listener listener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            Player.Listener.super.onPlaybackStateChanged(playbackState);
            if (playbackState == Player.STATE_ENDED) {
                next();
                Log.d(TAG, "player end");
            }
            if (playbackState == Player.STATE_READY) {
                if (!isCanceled) {
                    Constant.exoPlayer.play();
                    ((MainActivity) context).setBuffer(false);
                    if (builder == null) {
                        createNotification();
                    } else {
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                        updateNotificationPlay(Constant.exoPlayer.getPlayWhenReady());
                    }
                    updateNotificationAlbumArt(Constant.item_radio.get(Constant.position).radio_image_url);
                    updateNotificationMetadata(Constant.item_radio.get(Constant.position).radio_genre);
                    changePlayPause(true);
                    if (Config.ENABLE_RADIO_TIMEOUT) {
                        if (isCounterRunning) {
                            mCountDownTimer.cancel();
                        }
                    }
                } else {
                    isCanceled = false;
                    stopExoPlayer();
                }
                Log.d(TAG, "player ready");
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Player.Listener.super.onIsPlayingChanged(isPlaying);
            if (isPlaying) {
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire(60000);
                }
            } else {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
            Log.d("Rawr", "player changed");
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Player.Listener.super.onPlayerError(error);
            stop(true);
            if (Config.ENABLE_RADIO_TIMEOUT) {
                if (isCounterRunning) {
                    mCountDownTimer.cancel();
                }
            }
        }

        @Override
        public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) {
            Player.Listener.super.onPositionDiscontinuity(oldPosition, newPosition, reason);
        }

        @Override
        public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
            Player.Listener.super.onMediaMetadataChanged(mediaMetadata);
            //Log.d(TAG, "get mediaMetadata: " + Constant.exoPlayer.getMediaMetadata().subtitle);
        }

        @OptIn(markerClass = UnstableApi.class)
        @SuppressWarnings("ConstantValue")
        @Override
        public void onMetadata(@NonNull Metadata metadata) {
            Player.Listener.super.onMetadata(metadata);
            String category = Constant.item_radio.get(Constant.position).radio_genre;
            if (sharedPref.getSongMetadata().equals("true")) {
                if (metadata != null) {
                    for (int i = 0; i < metadata.length(); i++) {
                        Metadata.Entry entry = metadata.get(i);
                        if (entry instanceof TextInformationFrame textFrame) {
                            if (textFrame.id.equals("TIT2") || textFrame.id.equals("TIT1") || textFrame.id.equals("TPE1")) {
                                String title = textFrame.value;
                                if (!title.isEmpty()) {
                                    updateMetadata(title, true);
                                } else {
                                    updateMetadata(category, false);
                                }
                                Log.d(TAG, "Metadata (onMetadata) - Title: " + title);
                                return;
                            }
                        } else if (entry instanceof IcyInfo icyInfo) {
                            if (icyInfo.title != null) {
                                String title = icyInfo.title;
                                if (!title.isEmpty()) {
                                    updateMetadata(title, true);
                                } else {
                                    updateMetadata(category, false);
                                }
                                Log.d(TAG, "Metadata (onMetadata - ICY) - Title: " + title);
                                return;
                            }
                        }
                    }
                    updateMetadata(category, false);
                    Log.d(TAG, "Metadata (onMetadata) - Title not found in this metadata.");
                } else {
                    updateMetadata(category, false);
                    Log.d(TAG, "Metadata (onMetadata) - Received null metadata.");
                }
            } else {
                updateMetadata(category, false);
                Log.d(TAG, "Metadata is disabled");
            }
        }
    };

    private void updateMetadata(String metadata, boolean requestAlbumArt) {
        Tools.postDelayed(()-> {
            mediaMetadata = getMediaMetadata(metadata);
            updateNotificationMetadata(metadata);
            onMediaMetadataCompatChanged(metadata);
            if (requestAlbumArt) {
                requestAlbumArt(metadata);
            }
        }, 1000);
    }
    private void changePlayPause(Boolean play) {
        ((MainActivity) context).changePlayPause(play);
    }

    private void togglePlayPause() {
        if (Constant.exoPlayer.getPlayWhenReady()) {
            pause();
            updateNotificationAlbumArt(Constant.item_radio.get(Constant.position).radio_image_url);
            updateNotificationMetadata(Constant.item_radio.get(Constant.position).radio_genre);
        } else {
            if (Tools.isNetworkAvailable(context)) {
                newPlay();
            } else {
                Toast.makeText(context, getString(R.string.internet_not_connected), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pause() {
        Constant.exoPlayer.setPlayWhenReady(false);
        changePlayPause(false);
        updateNotificationPlay(Constant.exoPlayer.getPlayWhenReady());
    }

    private void newPlay() {
        loadSong = new LoadSong();
        loadSong.execute();

        if (Config.ENABLE_RADIO_TIMEOUT) {
            if (isCounterRunning) {
                mCountDownTimer.cancel();
            }
            mCountDownTimer.start();
        }

    }

    CountDownTimer mCountDownTimer = new CountDownTimer(Config.RADIO_TIMEOUT_CONNECTION, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            isCounterRunning = true;
            Log.d(TAG, "seconds remaining: " + millisUntilFinished / 1000);
        }

        @Override
        public void onFinish() {
            isCounterRunning = false;
            stop(true);
        }
    };

    private void next() {
        if (Constant.item_radio != null && !Constant.item_radio.isEmpty()) {
            RadioPlayerService.createInstance().initializeRadio(context, Constant.item_radio.get(Constant.position));
            Tools.getPosition(true);
            radio = Constant.item_radio.get(Constant.position);
            newPlay();
            //((MainActivity) context).hideSeekBar();
        }
    }

    private void previous() {
        if (Constant.item_radio != null && !Constant.item_radio.isEmpty()) {
            RadioPlayerService.createInstance().initializeRadio(context, Constant.item_radio.get(Constant.position));
            Tools.getPosition(false);
            radio = Constant.item_radio.get(Constant.position);
            newPlay();
            //((MainActivity) context).hideSeekBar();
        }
    }

    private void stop(boolean showMessage) {
        if (Constant.exoPlayer != null) {
            try {
                mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(onCallIncome);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(onHeadPhoneDetect);
                mAudioManager.unregisterMediaButtonEventReceiver(componentName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            changePlayPause(false);
            stopExoPlayer();
            service = null;
            stopForeground(true);
            stopSelf();
            ((MainActivity) context).setBuffer(false);
            ((MainActivity) context).changePlayPause(false);

            if (showMessage) {
                Toast.makeText(context, getString(R.string.error_loading_radio), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void stopExoPlayer() {
        if (Constant.exoPlayer != null) {
            Constant.exoPlayer.stop();
        }
//        stopCountDownTimerMetadata();
    }

    private PlaybackStateCompat createPlaybackState(int state) {
        long playbackPos = 0;
        playbackState = stateBuilder.setState(state, playbackPos, 1.0f).build();
        return playbackState;
    }

    @Override
    public void onFocusGained() {
        mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_PLAYING));
    }

    @Override
    public void onFocusLost() {
        mediaSessionCompat.setPlaybackState(createPlaybackState(PlaybackStateCompat.STATE_PAUSED));
    }

    private void onMediaMetadataCompatChanged(String metadata) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (mediaSessionCompat != null) {
                mediaSessionCompat.setMetadata(new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, Constant.item_radio.get(Constant.position).radio_name)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, metadata)
                        .build());
            }
        }
    }

    private void requestAlbumArt(String title) {
        if (sharedPref.getImageAlbumArt().equals("true")) {
            callbackCall = RestAdapter.createAlbumArtAPI().getAlbumArt(title, "music", 1);
            callbackCall.enqueue(new Callback<>() {
                public void onResponse(@NonNull Call<CallbackAlbumArt> call, @NonNull Response<CallbackAlbumArt> response) {
                    CallbackAlbumArt resp = response.body();
                    if (resp != null && resp.resultCount != 0) {
                        ArrayList<AlbumArt> albumArts = resp.results;
                        String artWorkUrl = albumArts.get(0).artworkUrl100.replace("100x100bb", "300x300bb");
                        ((MainActivity) context).changeAlbumArt(artWorkUrl);
                        updateNotificationAlbumArt(artWorkUrl);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> ((MainActivity) context).showImageAlbumArt(true), 100);
                        Log.d(TAG, "request album art success");
                    } else {
                        ((MainActivity) context).changeAlbumArt("");
                        updateNotificationAlbumArt(Constant.item_radio.get(Constant.position).radio_image_url);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> ((MainActivity) context).showImageAlbumArt(false), 100);
                        Log.d(TAG, "request album art failed");
                    }
                }

                public void onFailure(@NonNull Call<CallbackAlbumArt> call, @NonNull Throwable th) {
                    Log.d(TAG, "onFailure");
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    public Radio getPlayingRadioStation() {
        return radio;
    }

    private void getBitmapFromURL(String src) {
        try {
            URL url = new URL(src.replace(" ", "%20"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    BroadcastReceiver onCallIncome = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (isPlaying()) {
                if (state != null) {
                    if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) || state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        Intent intent_stop = new Intent(context, RadioPlayerService.class);
                        intent_stop.setAction(ACTION_TOGGLE);
                        startService(intent_stop);
                        Toast.makeText(context, "there is an call!!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "whoops!!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    BroadcastReceiver onHeadPhoneDetect = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.is_playing) {
                togglePlayPause();
            }
        }
    };

    AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (Config.RESUME_RADIO_ON_PHONE_CALL && Constant.isRadioPlaying) {
                    togglePlayPause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) {
                    togglePlayPause();
                    Constant.isRadioPlaying = true;
                } else {
                    Constant.isRadioPlaying = false;
                }
                break;
        }
    };

    @SuppressWarnings("UnusedReturnValue")
    private boolean createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            return true;
        }
        return false;
    }

    private void createNotification() {
        createNotificationChannel();
        buildNotification();
    }

    private void updateNotificationAlbumArt(String artWorkUrl) {
        new AsyncTaskExecutor<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void params) {
                try {
                    getBitmapFromURL(artWorkUrl);
                    if (builder != null) {
                        builder.setLargeIcon(bitmap);
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "error: " + e.getMessage());
                }
                return params;
            }

            @Override
            protected void onPostExecute(Void result) {

            }
        }.execute();
    }

    private void updateNotificationMetadata(String title) {
        if (builder != null) {
            ((MainActivity) context).changeSongName(title);
            builder.setContentTitle(Constant.item_radio.get(Constant.position).radio_name);
            builder.setContentText(title);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void buildNotification() {

        Log.d(TAG, "build notification");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String title = Constant.item_radio.get(Constant.position).radio_name;
        String artist = Constant.item_radio.get(Constant.position).radio_genre;

        builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentIntent(pendingIntent)
                .setLargeIcon(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_thumbnail), 128, 128, false))
                .setTicker(title)
                .setSmallIcon(R.drawable.ic_radio_notif)
                .setContentTitle(title)
                .setContentText(artist)
                .setPriority(Notification.PRIORITY_LOW)
                .setWhen(0)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionCompat.getSessionToken())
                        .setShowCancelButton(true)
                        .setShowActionsInCompactView(0, 1)
                )
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                //.addAction(R.drawable.ic_noti_previous, "previous", getPlaybackAction(ACTION_PREVIOUS))
                .addAction(R.drawable.ic_action_pause, "pause", getPlaybackAction(ACTION_TOGGLE))
                //.addAction(R.drawable.ic_noti_next, "next", getPlaybackAction(ACTION_NEXT))
                .addAction(R.drawable.ic_action_close, "close", getPlaybackAction(ACTION_STOP));

        startForeground(NOTIFICATION_ID, builder.build());
    }

    @SuppressLint("RestrictedApi")
    private void updateNotificationPlay(Boolean isPlay) {
        if (builder != null) {
            builder.mActions.remove(0);
            Intent playIntent = new Intent(getApplicationContext(), RadioPlayerService.class);
            playIntent.setAction(ACTION_TOGGLE);
            if (isPlay) {
                builder.mActions.add(0, new NotificationCompat.Action(R.drawable.ic_action_pause, "pause", getPlaybackAction(ACTION_TOGGLE)));
            } else {
                builder.mActions.add(0, new NotificationCompat.Action(R.drawable.ic_action_play, "Play", getPlaybackAction(ACTION_TOGGLE)));
            }
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private PendingIntent getPlaybackAction(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onDestroy() {
        try {
            mediaSessionCompat.release();
            unregisterReceiver(broadcastReceiver);

            Constant.exoPlayer.stop();
            Constant.exoPlayer.release();
            Constant.exoPlayer.removeListener(listener);
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            try {
                mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(onCallIncome);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(onHeadPhoneDetect);
                mAudioManager.unregisterMediaButtonEventReceiver(componentName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

}
