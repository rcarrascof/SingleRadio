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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.session.MediaButtonReceiver;

import com.app.AlofokeFm.BuildConfig;
import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.activities.MainActivity;
import com.app.AlofokeFm.callbacks.CallbackAlbumArt;
import com.app.AlofokeFm.models.AlbumArt;
import com.app.AlofokeFm.models.Radio;
import com.app.AlofokeFm.rests.RestAdapter;
import com.app.AlofokeFm.services.parser.URLParser;
import com.app.AlofokeFm.utils.Constant;
import com.app.AlofokeFm.utils.HttpsTrustManager;
import com.app.AlofokeFm.utils.Tools;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.DefaultHlsExtractorFactory;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressWarnings("deprecation")
public class RadioPlayerService extends Service {

    public static final String TAG = "RadioPlayerService";
    static private final int NOTIFICATION_ID = 1;
    @SuppressLint("StaticFieldLeak")
    static private RadioPlayerService service;
    @SuppressLint("StaticFieldLeak")
    static private Context context;
    static NotificationManager mNotificationManager;
    NotificationCompat.Builder mBuilder;
    static Radio radio;
    LoadSong loadSong;
    private Boolean isCanceled = false;
    RemoteViews bigViews, smallViews;
    Tools tools;
    Bitmap bitmap;
    ComponentName componentName;
    AudioManager mAudioManager;
    PowerManager.WakeLock mWakeLock;
    Call<CallbackAlbumArt> callbackCall = null;
    MediaSessionCompat mMediaSession;
    Radio obj;

    public static final String ACTION_STOP = BuildConfig.APPLICATION_ID + ".action.STOP";
    public static final String ACTION_PLAY = BuildConfig.APPLICATION_ID + ".action.PLAY";
    public static final String ACTION_PREVIOUS = BuildConfig.APPLICATION_ID + ".action.PREVIOUS";
    public static final String ACTION_NEXT = BuildConfig.APPLICATION_ID + ".action.NEXT";
    public static final String ACTION_TOGGLE = BuildConfig.APPLICATION_ID + ".action.TOGGLE_PLAYPAUSE";

    public void initialize(Context context, Radio station) {
        RadioPlayerService.context = context;
        radio = station;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    static public void initNewContext(Context context) {
        RadioPlayerService.context = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
            if (Constant.simpleExoPlayer != null) {
                return Constant.simpleExoPlayer.getPlayWhenReady();
            } else {
                return false;
            }
        }
    }

    @Override
    public void onCreate() {
        tools = new Tools(context);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        componentName = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(componentName);

        LocalBroadcastManager.getInstance(this).registerReceiver(onCallIncome, new IntentFilter("android.intent.action.PHONE_STATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(onHeadPhoneDetect, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        AdaptiveTrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context, trackSelectionFactory);
        Constant.simpleExoPlayer = new SimpleExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build();
        Constant.simpleExoPlayer.addListener(listener);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.setReferenceCounted(false);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null)
            try {
                switch (action) {
                    case ACTION_STOP:
                        stop(intent);
                        break;
                    case ACTION_PLAY:
                        newPlay();
                        break;
                    case ACTION_TOGGLE:
                        togglePlayPause();
                        break;
                    case ACTION_PREVIOUS:
                        if (tools.isNetworkAvailable()) {
                            previous();
                        } else {
                            tools.showToast(getString(R.string.internet_not_connected));
                        }
                        break;
                    case ACTION_NEXT:
                        if (tools.isNetworkAvailable()) {
                            next();
                        } else {
                            tools.showToast(getString(R.string.internet_not_connected));
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        return START_NOT_STICKY;
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadSong extends AsyncTask<String, Void, Boolean> {

        MediaSource mediaSource;

        protected void onPreExecute() {
            ((MainActivity) context).setBuffer(true);
            ((MainActivity) context).changeSongName(Constant.item_radio.get(Constant.position).getRadio_genre());
        }

        protected Boolean doInBackground(final String... args) {
            try {
                HttpsTrustManager.allowAllSSL();
                String url = radio.getRadio_url();

                HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory().setUserAgent(getUserAgent()).setAllowCrossProtocolRedirects(true);
                DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(), httpDataSourceFactory);
                MediaItem mMediaItem = MediaItem.fromUri(Uri.parse(url));
                MediaItem mMediaItemURLParser = MediaItem.fromUri(Uri.parse(URLParser.getUrl(url)));

                if (url.contains(".m3u8") || url.contains(".M3U8")) {
                    mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                            .setAllowChunklessPreparation(false)
                            .setExtractorFactory(new DefaultHlsExtractorFactory(DefaultTsPayloadReaderFactory.FLAG_IGNORE_H264_STREAM, false))
                            .createMediaSource(mMediaItem);
                } else if (url.contains(".m3u") || url.contains("yp.shoutcast.com/sbin/tunein-station.m3u?id=")) {
                    mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, new DefaultExtractorsFactory())
                            .createMediaSource(mMediaItemURLParser);
                } else if (url.contains(".pls") || url.contains("listen.pls?sid=") || url.contains("yp.shoutcast.com/sbin/tunein-station.pls?id=")) {
                    mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, new DefaultExtractorsFactory())
                            .createMediaSource(mMediaItemURLParser);
                } else {
                    mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, new DefaultExtractorsFactory())
                            .createMediaSource(mMediaItem);
                }
                return true;
            } catch (Exception e1) {
                e1.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (context != null) {
                super.onPostExecute(aBoolean);
                if (mBuilder == null) {
                    createNotification();
                    new Handler().postDelayed(RadioPlayerService.this::updateNotificationMetadata, 100);
                    Log.d(TAG, "create notification");
                } else {
                    updateNotification();
                    new Handler().postDelayed(RadioPlayerService.this::updateNotificationMetadata, 100);
                    Log.d(TAG, "update notification");
                }
                Constant.simpleExoPlayer.seekTo(Constant.simpleExoPlayer.getCurrentWindowIndex(), Constant.simpleExoPlayer.getCurrentPosition());
                Constant.simpleExoPlayer.setMediaSource(mediaSource);
                Constant.simpleExoPlayer.prepare();
                Constant.simpleExoPlayer.setPlayWhenReady(true);
                if (!aBoolean) {
                    ((MainActivity) context).setBuffer(false);
                    tools.showToast(getString(R.string.error_loading_radio));
                }
            }
        }
    }


    Player.Listener listener = new Player.Listener() {
        @Override
        public void onCues(@NonNull List<Cue> cues) {
        }

        @Override
        public void onMetadata(@NonNull Metadata metadata) {
            new Handler().postDelayed(() -> getMetadata(metadata), 1000);
        }

        @Override
        public void onTimelineChanged(@NonNull Timeline timeline, int reason) {

        }

        @Override
        public void onTracksChanged(@NonNull TrackGroupArray trackGroups, @NonNull TrackSelectionArray trackSelections) {

        }

        @Override
        public void onStaticMetadataChanged(@NonNull List<Metadata> metadataList) {
            //getMetadata(metadataList.get(0));
        }

        @Override
        public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
            //getMediaMetadata(mediaMetadata);
        }

        @Override
        public void onIsLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                next();
            } else if (playbackState == Player.STATE_READY) {
                if (!isCanceled) {
                    ((MainActivity) context).setBuffer(false);
                    if (mBuilder == null) {
                        createNotification();
                    } else {
                        updateNotification();
                    }
                    changePlayPause(true);
                } else {
                    isCanceled = false;
                    stopExoPlayer();
                }
            }
        }

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            if (playWhenReady) {
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire(60000);
                }
            } else {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
        }

        @Override
        public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {

        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {

        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(@NonNull ExoPlaybackException error) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Toast.makeText(context, context.getString(R.string.error_loading_radio), Toast.LENGTH_SHORT).show();
                stopExoPlayer();
                stopForeground(true);
                stopSelf();
                ((MainActivity) context).setBuffer(false);
                ((MainActivity) context).changePlayPause(false);
            }, 0);
        }

    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void getMetadata(Metadata metadata) {
        obj = Constant.item_radio.get(Constant.position);
        if (!metadata.get(0).toString().equals("")) {
            String data = metadata.get(0).toString().replace("ICY: ", "");
            ArrayList<String> arrayList = new ArrayList(Arrays.asList(data.split(",")));
            String[] mediaMetadata = arrayList.get(0).split("=");

            String title;
            if (arrayList.get(0).contains("null")) {
                title = obj.radio_genre;
            } else {
                title = mediaMetadata[1].replace("\"", "");
            }

            if ("".equalsIgnoreCase(title)) {
                ((MainActivity) context).changeSongName(obj.radio_genre);
            } else {
                ((MainActivity) context).changeSongName(title);
                if (mBuilder == null) {
                    createNotification();
                } else {
                    updateNotification();
                }
            }

            if (Config.ENABLE_ALBUM_ART_METADATA) {
                if (!arrayList.get(0).contains("null")) {
                    this.callbackCall = RestAdapter.createAlbumArtAPI().getAlbumArt(title, "music", 1);
                    this.callbackCall.enqueue(new Callback<CallbackAlbumArt>() {
                        public void onResponse(Call<CallbackAlbumArt> call, Response<CallbackAlbumArt> response) {
                            CallbackAlbumArt resp = response.body();
                            if (resp != null && resp.resultCount != 0) {
                                ArrayList<AlbumArt> albumArts = resp.results;
                                String artWorkUrl = albumArts.get(0).artworkUrl100.replace("100x100bb", "300x300bb");
                                ((MainActivity) context).changeAlbumArt(artWorkUrl);
                                updateNotificationImageAlbumArt(artWorkUrl);
                                updateNotificationMetadata(obj.radio_name, title);
                                Log.d(TAG, "request album art success : " + artWorkUrl);
                            } else {
                                ((MainActivity) context).changeAlbumArt("");
                                updateNotificationImageAlbumArt(obj.radio_image_url);
                                updateNotificationMetadata(obj.radio_name, title);
                                Log.d(TAG, "request album art failed");
                            }
                        }

                        public void onFailure(Call<CallbackAlbumArt> call, Throwable th) {
                            Log.e("onFailure", "" + th.getMessage());
                        }
                    });
                }
            } else {
                updateNotificationImageAlbumArt(obj.radio_image_url);
                updateNotificationMetadata(obj.radio_name, title);
            }

        }
    }

//    private void getMediaMetadata(MediaMetadata mediaMetadata) {
//        if (mediaMetadata.title != null) {
//            String title = (String) mediaMetadata.title;
//            if ("".equalsIgnoreCase(title)) {
//                ((MainActivity) context).changeSongName(Constant.item_radio.get(Constant.position).getRadio_genre());
//            } else {
//                ((MainActivity) context).changeSongName(title);
//                if (mBuilder == null) {
//                    createNotification();
//                } else {
//                    updateNotification();
//                }
//            }
//        }
//    }
//
//    private void requestAlbumArt(String term) {
//        this.callbackCall = RestAdapter.createAlbumArtAPI().getAlbumArt(term, "music", 1);
//        this.callbackCall.enqueue(new Callback<CallbackAlbumArt>() {
//            public void onResponse(Call<CallbackAlbumArt> call, Response<CallbackAlbumArt> response) {
//                CallbackAlbumArt resp = response.body();
//                if (resp != null && resp.resultCount != 0) {
//                    ArrayList<AlbumArt> albumArts = resp.results;
//                    String artWorkUrl = albumArts.get(0).artworkUrl100.replace("100x100bb", "300x300bb");
//                    ((MainActivity) context).changeAlbumArt(artWorkUrl);
//                    updateNotificationImageAlbumArt(artWorkUrl);
//                    updateNotificationMetadata(Constant.item_radio.get(Constant.position).radio_name, term);
//                } else {
//                    String artWorkUrl = "";
//                    ((MainActivity) context).changeAlbumArt(artWorkUrl);
//                    updateNotificationImageAlbumArt(Constant.item_radio.get(Constant.position).radio_image_url);
//                    updateNotificationMetadata(Constant.item_radio.get(Constant.position).radio_name, term);
//                }
//            }
//
//            public void onFailure(Call<CallbackAlbumArt> call, Throwable th) {
//                Log.e("onFailure", "" + th.getMessage());
//            }
//        });
//    }

    private void updateNotificationImageAlbumArt(String artWorkUrl) {
        new Thread(() -> {
            try {
                getBitmapFromURL(artWorkUrl);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mBuilder.setLargeIcon(bitmap);
                } else {
                    bigViews.setImageViewBitmap(R.id.img_notification, bitmap);
                    smallViews.setImageViewBitmap(R.id.status_bar_album_art, bitmap);
                }
                mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new Handler(Looper.getMainLooper()).post(() -> mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build()));
        }).start();
    }

    private void updateNotificationMetadata() {
        obj = Constant.item_radio.get(Constant.position);
        updateNotificationImageAlbumArt(obj.radio_image_url);
        updateNotificationMetadata(obj.radio_name, obj.radio_genre);
        ((MainActivity) context).changeSongName(obj.radio_genre);
        Log.d(TAG, "setDefaultImageIfMetadataIsEmpty");
    }

    private void updateNotificationMetadata(String radio_name, String metadata) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mMediaSession = new MediaSessionCompat(context, getString(R.string.app_name));
            mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, radio_name)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, metadata)
                    .build());
            mBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mMediaSession.getSessionToken())
                    .setShowCancelButton(true)
                    .setShowActionsInCompactView(0, 1)
                    .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)));
        }
    }

    private String getUserAgent() {

        StringBuilder result = new StringBuilder(64);
        result.append("Dalvik/");
        result.append(System.getProperty("java.vm.version"));
        result.append(" (Linux; U; Android ");

        String version = Build.VERSION.RELEASE;
        result.append(version.length() > 0 ? version : "1.0");

        if ("REL".equals(Build.VERSION.CODENAME)) {
            String model = Build.MODEL;
            if (model.length() > 0) {
                result.append("; ");
                result.append(model);
            }
        }

        String id = Build.ID;

        if (id.length() > 0) {
            result.append(" Build/");
            result.append(id);
        }

        result.append(")");
        return result.toString();
    }

    private void changePlayPause(Boolean play) {
        ((MainActivity) context).changePlayPause(play);
    }

    private void togglePlayPause() {
        if (Constant.simpleExoPlayer.getPlayWhenReady()) {
            pause();
        } else {
            if (tools.isNetworkAvailable()) {
                play();
            } else {
                tools.showToast(getString(R.string.internet_not_connected));
            }
        }
    }

    private void pause() {
        Constant.simpleExoPlayer.setPlayWhenReady(false);
        changePlayPause(false);
        updateNotificationPlay(false);
    }

    private void play() {
        Constant.simpleExoPlayer.setPlayWhenReady(true);
        Constant.simpleExoPlayer.seekTo(Constant.simpleExoPlayer.getCurrentWindowIndex(), Constant.simpleExoPlayer.getCurrentPosition());
        changePlayPause(true);
        updateNotificationPlay(true);
//        ((MainActivity) context).seekBarUpdate();
    }

    private void newPlay() {
        loadSong = new LoadSong();
        loadSong.execute();
    }

    private void next() {
        tools.getPosition(true);
        radio = Constant.item_radio.get(Constant.position);
        newPlay();
    }

    private void previous() {
        tools.getPosition(false);
        radio = Constant.item_radio.get(Constant.position);
        newPlay();
    }

    public void stop(Intent intent) {
        if (Constant.simpleExoPlayer != null) {
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
            stopService(intent);
            stopForeground(true);
        }
    }

    public void stopExoPlayer() {
        if (Constant.simpleExoPlayer != null) {
            Constant.simpleExoPlayer.stop();
            Constant.simpleExoPlayer.addListener(listener);
        }
    }

    private void createNotification() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(this, RadioPlayerService.class);
        playIntent.setAction(ACTION_TOGGLE);
        PendingIntent pendingIntentPlay = PendingIntent.getService(this, 0, playIntent, 0);

        Intent closeIntent = new Intent(this, RadioPlayerService.class);
        closeIntent.setAction(ACTION_STOP);
        PendingIntent pendingIntentClose = PendingIntent.getService(this, 0, closeIntent, PendingIntent.FLAG_CANCEL_CURRENT);


        String NOTIFICATION_CHANNEL_ID = "your_single_app_channel_001";
        mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setTicker(radio.getRadio_name())
                .setContentTitle(radio.getRadio_name())
                .setContentText(Constant.metadata)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_radio_notif)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setOnlyAlertOnce(true);

        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);// The user-visible name of the channel.
            mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(mChannel);

            mMediaSession = new MediaSessionCompat(context, getString(R.string.app_name));
            mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

            mBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mMediaSession.getSessionToken())
                    .setShowCancelButton(true)
                    .setShowActionsInCompactView(0, 1)
                    .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)))
                    .addAction(new NotificationCompat.Action(
                            R.drawable.ic_pause_white, "Pause",
                            pendingIntentPlay))
                    .addAction(new NotificationCompat.Action(
                            R.drawable.ic_noti_close, "Close",
                            pendingIntentClose));
        } else {
            bigViews = new RemoteViews(getPackageName(), R.layout.lyt_notification_large);
            smallViews = new RemoteViews(getPackageName(), R.layout.lyt_notification_small);
            bigViews.setOnClickPendingIntent(R.id.img_notification_play, pendingIntentPlay);
            smallViews.setOnClickPendingIntent(R.id.status_bar_play, pendingIntentPlay);

            bigViews.setOnClickPendingIntent(R.id.img_notification_close, pendingIntentClose);
            smallViews.setOnClickPendingIntent(R.id.status_bar_collapse, pendingIntentClose);

            bigViews.setImageViewResource(R.id.img_notification_play, android.R.drawable.ic_media_pause);
            smallViews.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_pause);

            bigViews.setTextViewText(R.id.txt_notification_name, Constant.item_radio.get(Constant.position).getRadio_name());
            bigViews.setTextViewText(R.id.txt_notification_category, Constant.metadata);
            smallViews.setTextViewText(R.id.status_bar_track_name, Constant.item_radio.get(Constant.position).getRadio_name());
            smallViews.setTextViewText(R.id.status_bar_artist_name, Constant.metadata);

            bigViews.setImageViewResource(R.id.img_notification, R.mipmap.ic_launcher);
            smallViews.setImageViewResource(R.id.status_bar_album_art, R.mipmap.ic_launcher);

            mBuilder.setCustomContentView(smallViews).setCustomBigContentView(bigViews);
        }

        startForeground(NOTIFICATION_ID, mBuilder.build());
    }

    private void updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBuilder.setContentTitle(Constant.item_radio.get(Constant.position).getRadio_name());
            mBuilder.setContentText(Constant.metadata);
        } else {
            bigViews.setTextViewText(R.id.txt_notification_name, Constant.item_radio.get(Constant.position).getRadio_name());
            bigViews.setTextViewText(R.id.txt_notification_category, Constant.metadata);
            smallViews.setTextViewText(R.id.status_bar_track_name, Constant.item_radio.get(Constant.position).getRadio_name());
            smallViews.setTextViewText(R.id.status_bar_artist_name, Constant.metadata);
        }
        updateNotificationPlay(Constant.simpleExoPlayer.getPlayWhenReady());
    }

    @SuppressLint("RestrictedApi")
    private void updateNotificationPlay(Boolean isPlay) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            mBuilder.mActions.remove(0);
            Intent playIntent = new Intent(this, RadioPlayerService.class);
            playIntent.setAction(ACTION_TOGGLE);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, playIntent, 0);
            if (isPlay) {
                mBuilder.mActions.add(0, new NotificationCompat.Action(R.drawable.ic_pause_white, "Pause", pendingIntent));
            } else {
                mBuilder.mActions.add(0, new NotificationCompat.Action(R.drawable.ic_play_arrow_white, "Play", pendingIntent));
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mBuilder.mActions.remove(0);
            Intent playIntent = new Intent(this, RadioPlayerService.class);
            playIntent.setAction(ACTION_TOGGLE);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, playIntent, 0);
            if (isPlay) {
                mBuilder.mActions.add(0, new NotificationCompat.Action(R.drawable.ic_pause_white, "Pause", pendingIntent));
            } else {
                mBuilder.mActions.add(0, new NotificationCompat.Action(R.drawable.ic_play_arrow_white, "Play", pendingIntent));
            }
        } else {
            if (isPlay) {
                bigViews.setImageViewResource(R.id.img_notification_play, android.R.drawable.ic_media_pause);
                smallViews.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_pause);
            } else {
                bigViews.setImageViewResource(R.id.img_notification_play, android.R.drawable.ic_media_play);
                smallViews.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_play);
            }
        }
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
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
            Log.d("getBitmap", "load bitmap url : " + src);
        } catch (IOException e) {
            // Log exception
            e.printStackTrace();
            Log.d("getBitmap", "error : " + src);
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
                // Resume your media player here
                if (Config.RESUME_RADIO_ON_PHONE_CALL) {
                    togglePlayPause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) {
                    togglePlayPause();
                }
                break;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            Constant.simpleExoPlayer.stop();
            Constant.simpleExoPlayer.release();
            Constant.simpleExoPlayer.removeListener(listener);
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