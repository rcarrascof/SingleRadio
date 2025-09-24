package com.app.AlofokeFm.utils;

import com.app.AlofokeFm.models.Radio;
import com.vhall.android.exoplayer2.SimpleExoPlayer;

import java.io.Serializable;
import java.util.ArrayList;

public class Constant implements Serializable {

    public static final String USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";

    public static final boolean BANNER_AD = true;
    public static final boolean INTERSTITIAL_AD = true;
    public static final boolean NATIVE_AD_DRAWER_MENU = true;
    public static final boolean NATIVE_AD_EXIT_DIALOG = true;

    public static final String NATIVE_AD_STYLE_DRAWER_MENU = "small";
    public static final String NATIVE_AD_STYLE_EXIT_DIALOG = "small";

    public static final int IMMEDIATE_APP_UPDATE_REQ_CODE = 124;
    private static final long serialVersionUID = 1L;
    public static final int PERMISSIONS_REQUEST = 102;

    public static final int DELAY_PERFORM_CLICK = 1000;
    public static final int DELAY_ACTION_CLICK = 250;

    public static final boolean CIRCLE = true;
    public static final boolean SQUARE = false;

    public static String metadata;
    public static String albumArt;
    public static SimpleExoPlayer exoPlayer;
    public static Boolean is_playing = false;
    public static Boolean radio_type = true;
    public static Boolean is_app_open = false;
    public static ArrayList<Radio> item_radio = new ArrayList<>();
    public static int position = 0;

    public static final String LOCALHOST_ADDRESS = "https://drive.google.com/file/d/1vgUP-0c2pkDjff_59bSIX6O0KVDx9veN/view?usp=share_link";

    public static boolean isAppOpen = false;
    public static boolean isRadioPlaying = false;

}