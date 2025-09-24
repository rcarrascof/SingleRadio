package com.app.Ritmo96.utils;

import static com.app.Ritmo96.utils.Constant.PERMISSIONS_REQUEST;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.app.Ritmo96.BuildConfig;
import com.app.Ritmo96.Config;
import com.app.Ritmo96.R;
import com.app.Ritmo96.database.prefs.SharedPref;
import com.app.Ritmo96.fragments.FragmentWebView;
import com.app.Ritmo96.listener.OnCompleteListener;
import com.app.Ritmo96.listener.OnNeutralButtonListener;
import com.app.Ritmo96.listener.OnPositiveButtonListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.solodroid.push.sdk.provider.OneSignalPush;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public long convertToMilliSeconds(String s) {

        long ms = 0;
        Pattern p;
        if (s.contains(("\\:"))) {
            p = Pattern.compile("(\\d+):(\\d+)");
        } else {
            p = Pattern.compile("(\\d+).(\\d+)");
        }
        Matcher m = p.matcher(s);
        if (m.matches()) {
            int h = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            // int sec = Integer.parseInt(m.group(2));
            ms = (long) h * 60 * 60 * 1000 + min * 60 * 1000;
        }
        return ms;
    }

    public void getPosition(Boolean isNext) {
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

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isConnect(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                return activeNetworkInfo.isConnected() || activeNetworkInfo.isConnectedOrConnecting();
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static String getUserAgent() {
        return Constant.USER_AGENT;
    }

    public static String getUserAgentLegacy() {

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

    //Get response from an URL request (GET)
    public static String getDataFromUrl(String url) {
        // Making HTTP request
        Log.v("INFO", "Requesting: " + url);

        StringBuffer chaine = new StringBuffer("");
        try {
            URL urlCon = new URL(url);

            //Open a connection
            HttpURLConnection connection = (HttpURLConnection) urlCon
                    .openConnection();
            connection.setRequestProperty("User-Agent", "Your Single Radio");
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();

            //Handle redirecti
            int status = connection.getResponseCode();
            if ((status != HttpURLConnection.HTTP_OK)
                    && (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)) {

                // get redirect url from "location" header field
                String newUrl = connection.getHeaderField("Location");
                // get the cookie if need, for login
                String cookies = connection.getHeaderField("Set-Cookie");

                // open the new connnection again
                connection = (HttpURLConnection) new URL(newUrl).openConnection();
                connection.setRequestProperty("Cookie", cookies);
                connection.setRequestProperty("User-Agent", "Your Single Radio");
                connection.setRequestMethod("GET");
                connection.setDoInput(true);

                System.out.println("Redirect to URL : " + newUrl);
            }

            //Get the stream from the connection and read it
            InputStream inputStream = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while ((line = rd.readLine()) != null) {
                chaine.append(line);
            }

        } catch (IOException e) {
            // writing exception to log
        }

        return chaine.toString();
    }

    //Get JSON from an url and parse it to a JSON Object.
    public static JSONObject getJSONObjectFromUrl(String url) {
        String data = getDataFromUrl(url);

        try {
            return new JSONObject(data);
        } catch (Exception e) {
            Log.e("INFO", "Error parsing JSON. Printing stacktrace now");
        }

        return null;
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

    public static void startWebActivity(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    public static void startEmailActivity(Context context, String email, String subject, String text) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("mailto:");
            builder.append(email);
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(builder.toString()));
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, text);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void startCallActivity(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void startSmsActivity(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void startMapSearchActivity(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void startIntentChooserActivity(Context context, String url) {
        try {
            String[] results = url.split("target=");
            String type = results[1];
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setDataAndType(Uri.parse(url), type + "/*");
            context.startActivity(Intent.createChooser(intent, "Complete action using"));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void startExternalApplication(Context context, String url) {
        try {
            String[] results = url.split("package=");
            String packageName = results[1];
            boolean isAppInstalled = appInstalledOrNot(context, packageName);
            if (isAppInstalled) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setPackage(packageName);
                intent.setData(Uri.parse(url));
                context.startActivity(intent);
                Log.i(TAG, "Application is already installed.");
            } else {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                Log.i(TAG, "Application is not currently installed.");
            }
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static boolean appInstalledOrNot(Context context, String uri) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "NameNotFoundException");
        }
        return false;
    }

    public static void slideUp(Context context, View view) {
        view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_up));
        view.setVisibility(View.VISIBLE);
    }

    public static void slideDown(Context context, View view) {
        //view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_down));
        view.setVisibility(View.GONE);
        view.clearAnimation();
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
                nativeAdView.addView(View.inflate(context, com.solodroid.ads.sdk.R.layout.view_native_ad_radio, null));
                break;
            case "medium":
                nativeAdView.addView(View.inflate(context, com.solodroid.ads.sdk.R.layout.view_native_ad_news, null));
                break;
            default:
                nativeAdView.addView(View.inflate(context, com.solodroid.ads.sdk.R.layout.view_native_ad_medium, null));
                break;
        }
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

    public void inAppReview(Activity activity) {
        if (sharedPref.getInAppReviewToken() <= 3) {
            sharedPref.updateInAppReviewToken(sharedPref.getInAppReviewToken() + 1);
            Log.d(TAG, "in app update token");
        } else {
            ReviewManager manager = ReviewManagerFactory.create(activity);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(activity, reviewInfo).addOnFailureListener(e -> {
                    }).addOnCompleteListener(complete -> Log.d(TAG, "Success")
                    ).addOnFailureListener(failure -> Log.d(TAG, "Rating Failed"));
                }
            }).addOnFailureListener(failure -> Log.d(TAG, "In-App Request Failed " + failure));
            Log.d(TAG, "in app token complete, show in app review if available");
        }
        Log.d(TAG, "in app review token : " + sharedPref.getInAppReviewToken());
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

    public void showDialogAbout(Activity activity) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.custom_dialog_about, null);
        ((TextView) view.findViewById(R.id.txt_app_version)).setText(activity.getString(R.string.sub_about_app_version) + " " + BuildConfig.VERSION_NAME);

        final MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(activity);
        alert.setView(view);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.option_ok, (dialog, which) -> dialog.dismiss());
        alert.show();
    }

    @SuppressLint("RtlHardcoded")
    public static void dialogExitButtonSelected(Activity activity, View rootView, AlertDialog alertDialog, OnPositiveButtonListener onPositiveButtonListener, OnNeutralButtonListener onNeutralButtonListener) {

        LinearLayout lytButton = rootView.findViewById(R.id.lyt_button);
        Button btnPositive = rootView.findViewById(R.id.btn_positive);
        Button btnNeutral = rootView.findViewById(R.id.btn_neutral);
        Button btnNegative = rootView.findViewById(R.id.btn_negative);

        btnPositive.setOnClickListener(view -> Tools.postDelayed(() -> {
            onPositiveButtonListener.onPositive();
            alertDialog.dismiss();
        }, Constant.DELAY_ACTION_CLICK));

        btnNeutral.setOnClickListener(view -> Tools.postDelayed(() -> {
            onNeutralButtonListener.onNeutral();
            alertDialog.dismiss();
        }, Constant.DELAY_ACTION_CLICK));

        btnNegative.setOnClickListener(view -> Tools.postDelayed(alertDialog::dismiss, Constant.DELAY_ACTION_CLICK));

        btnPositive.setTextColor(ContextCompat.getColor(activity, R.color.color_light_primary));
        btnNeutral.setTextColor(ContextCompat.getColor(activity, R.color.color_light_primary));
        btnNegative.setTextColor(ContextCompat.getColor(activity, R.color.color_light_primary));

        if (Config.ENABLE_RTL_MODE) {
            lytButton.setGravity(Gravity.LEFT);
            Objects.requireNonNull(alertDialog.getWindow()).getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            lytButton.setGravity(Gravity.RIGHT);
            Objects.requireNonNull(alertDialog.getWindow()).getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

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

    public static void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            view.requestLayout();
        }
    }

    public static void openCustomTabs(Activity activity, String url) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(activity, Uri.parse(url));
    }

    public void slideUp(Activity activity, View view) {
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(0, 0, activity.findViewById(R.id.root_view).getHeight(), 0);
        animate.setDuration(300);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    public void slideDown(Activity activity, View view) {
        TranslateAnimation animate = new TranslateAnimation(0, 0, 0, activity.findViewById(R.id.root_view).getHeight());
        animate.setDuration(300);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

}