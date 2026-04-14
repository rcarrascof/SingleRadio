package com.app.AlofokeFm.utils;

import static com.app.AlofokeFm.utils.Constant.PERMISSIONS_REQUEST;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.app.AlofokeFm.BuildConfig;
import com.app.AlofokeFm.Config;
import com.app.AlofokeFm.R;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.fragments.FragmentWebView;
import com.app.AlofokeFm.listener.OnCompleteListener;
import com.app.AlofokeFm.listener.OnPositiveButtonListener;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.solodroid.push.sdk.provider.OneSignalPush;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class Tools {

    public static final String TAG = "Utils";
    Context context;
    SharedPref sharedPref;

    public Tools(Context context) {
        this.context = context;
        sharedPref = new SharedPref(context);
    }

    public static void requestPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, "android.permission.READ_PHONE_STATE") != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(new String[]{"android.permission.READ_PHONE_STATE"}, PERMISSIONS_REQUEST);
            }
        }
    }

    public static void darkStatusBar(Activity activity, boolean statusBarDark) {
        if (statusBarDark) {
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.color_black));
        } else {
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.color_light_status_bar));
        }
        EdgeToEdge.enable((ComponentActivity) activity);
    }

    public static void setNavigation(Activity activity) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(R.id.root_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView()).setAppearanceLightStatusBars(false);
    }

    public static void transparentStatusBar(Activity activity) {
        //activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setWindowFlag(activity, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        activity.getWindow().setNavigationBarColor(activity.getResources().getColor(R.color.color_black));
    }

    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    public static void notificationHandler(AppCompatActivity context, Intent getIntent) {
        String id = getIntent.getStringExtra(OneSignalPush.EXTRA_ID);
        String title = getIntent.getStringExtra(OneSignalPush.EXTRA_TITLE);
        String message = getIntent.getStringExtra(OneSignalPush.EXTRA_MESSAGE);
        String bigImage = getIntent.getStringExtra(OneSignalPush.EXTRA_IMAGE);
        String launchUrl = getIntent.getStringExtra(OneSignalPush.EXTRA_LAUNCH_URL);
        String link = getIntent.getStringExtra(OneSignalPush.EXTRA_LINK);
        String uniqueId = getIntent.getStringExtra(OneSignalPush.EXTRA_UNIQUE_ID);
        if (getIntent.hasExtra(OneSignalPush.EXTRA_UNIQUE_ID)) {
            if (launchUrl != null && !launchUrl.equals("")) {
                Tools.openActivityLaunchUrl(context, title, launchUrl);
            } else {
                if (link != null && !link.equals("")) {
                    if (!link.equals("0")) {
                        Tools.openActivityLaunchUrl(context, title, link);
                    }
                }
            }
        }
    }

    public static void openActivityLaunchUrl(AppCompatActivity context, String title, String url) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (url.contains("play.google.com") || url.contains("?target=external")) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } else {
                FragmentWebView fragmentWebView = new FragmentWebView();
                Bundle args = new Bundle();
                args.putString("title", title);
                args.putString("url", url);
                fragmentWebView.setArguments(args);
                FragmentManager fragmentManager = context.getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_up, 0, 0, R.anim.slide_down);
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.add(android.R.id.content, fragmentWebView).addToBackStack("page");
                transaction.commit();
            }
        }, 1000);
    }

    public static void getPosition(Boolean isNext) {
        if (isNext) {
            if (Constant.position != Constant.item_radio.size() - 1) {
                Constant.position = Constant.position + 1;
            } else {
                Constant.position = 0;
            }
        } else {
            if (Constant.position != 0) {
                Constant.position = Constant.position - 1;
            } else {
                Constant.position = Constant.item_radio.size() - 1;
            }
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String loadJSONFromAsset(Context context, String name) {
        String json;
        try {
            InputStream is = context.getAssets().open(name);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private static final double BITMAP_SCALE = 0.8;
    private static final float BLUR_RADIUS = 15;

    public static Bitmap blurImage(Activity activity, Bitmap bitmap) {
        try {
            RenderScript rsScript = RenderScript.create(activity);
            Allocation allocation = Allocation.createFromBitmap(rsScript, bitmap);

            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript));
            blur.setRadius(BLUR_RADIUS);
            blur.setInput(allocation);

            Bitmap result = Bitmap.createBitmap((int) (bitmap.getWidth() * BITMAP_SCALE), (int) (bitmap.getHeight() * BITMAP_SCALE), Bitmap.Config.ARGB_8888);
            Allocation outAlloc = Allocation.createFromBitmap(rsScript, result);

            blur.forEach(outAlloc);
            outAlloc.copyTo(result);

            rsScript.destroy();
            return result;
        } catch (Exception e) {
            return bitmap;
        }

    }

    @SuppressLint("SourceLockedOrientationActivity")
    public static void fullScreenMode(AppCompatActivity activity, boolean show) {
        if (show) {
            //activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            setWindowFlag(activity, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, false);
            int resultColor;
//            resultColor = ColorUtils.blendARGB(Color.BLACK, activity.getResources().getColor(R.color.color_light_status_bar), 0.6f);
//            activity.getWindow().setStatusBarColor(resultColor);
            //activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//            }
        }
    }

    public static void setNativeAdStyle(Context context, LinearLayout nativeAdView, String style) {
        switch (style) {
            case "small":
                nativeAdView.addView(View.inflate(context, com.solodroidx.ads.R.layout.view_native_ad_radio, null));
                break;
            case "medium":
                nativeAdView.addView(View.inflate(context, com.solodroidx.ads.R.layout.view_native_ad_news, null));
                break;
            default:
                nativeAdView.addView(View.inflate(context, com.solodroidx.ads.R.layout.view_native_ad_medium, null));
                break;
        }
    }

    public static String nativeAdStyleFormatter(String style) {
        return switch (style) {
            case "small" -> "radio";
            case "medium" -> "news";
            case "large" -> "medium";
            default -> "default";
        };
    }

    public static void postDelayed(OnCompleteListener onCompleteListener, int millisecond) {
        new Handler(Looper.getMainLooper()).postDelayed(onCompleteListener::onComplete, millisecond);
    }

    public static String formatSeconds(long timeInSeconds) {
        long secondsLeft = timeInSeconds % 3600 % 60;
        long minutes = (long) (double) (timeInSeconds % 3600 / 60);
        long hours = (long) (double) (timeInSeconds / 3600);
        String HH = ((hours < 10) ? "0" : "") + hours;
        String MM = ((minutes < 10) ? "0" : "") + minutes;
        String SS = ((secondsLeft < 10) ? "0" : "") + secondsLeft;
        return HH + ":" + MM + ":" + SS;
    }

    public void changeVolume(Activity activity, ImageView imgVolume) {
        final RelativePopupWindow popupWindow = new RelativePopupWindow(activity);
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        assert inflater != null;
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.lyt_volume, null);
        ImageView imageView1 = view.findViewById(R.id.img_volume_max);
        ImageView imageView2 = view.findViewById(R.id.img_volume_min);
        imageView1.setColorFilter(Color.BLACK);
        imageView2.setColorFilter(Color.BLACK);

        VerticalSeekBar seekBar = view.findViewById(R.id.seek_bar_volume);
        seekBar.getThumb().setColorFilter(sharedPref.getFirstColor(), PorterDuff.Mode.SRC_IN);
        seekBar.getProgressDrawable().setColorFilter(sharedPref.getSecondColor(), PorterDuff.Mode.SRC_IN);

        final AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
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

    @SuppressLint("RtlHardcoded")
    public static void dialogButtonSelected(Activity activity, View rootView, AlertDialog alertDialog, OnPositiveButtonListener onPositiveButtonListener) {

        LinearLayout lytButton = rootView.findViewById(R.id.lyt_button);
        Button btnPositive = rootView.findViewById(R.id.btn_positive);
        Button btnNegative = rootView.findViewById(R.id.btn_negative);

        btnPositive.setOnClickListener(view -> Tools.postDelayed(() -> {
            onPositiveButtonListener.onPositive();
            alertDialog.dismiss();
        }, Constant.DELAY_ACTION_CLICK));

        btnNegative.setOnClickListener(view -> Tools.postDelayed(alertDialog::dismiss, Constant.DELAY_ACTION_CLICK));

        btnPositive.setTextColor(ContextCompat.getColor(activity, R.color.color_light_primary));
        btnNegative.setTextColor(ContextCompat.getColor(activity, R.color.color_light_primary));

        if (Config.ENABLE_RTL_MODE) {
            lytButton.setGravity(Gravity.LEFT);
            Objects.requireNonNull(alertDialog.getWindow()).getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            lytButton.setGravity(Gravity.RIGHT);
            Objects.requireNonNull(alertDialog.getWindow()).getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

    }

    public static void openCustomTabs(Activity activity, String url) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(activity, Uri.parse(url));
    }

    public static String getApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }

    public static int getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    public static String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public static boolean isDebug() {
        return BuildConfig.DEBUG;
    }

}