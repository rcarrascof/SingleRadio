package com.app.Ritmo96.utils;

import com.app.Ritmo96.models.Radio;
import com.vhall.android.exoplayer2.SimpleExoPlayer;

import java.io.Serializable;
import java.util.ArrayList;

public class Constant implements Serializable {

    public static final int IMMEDIATE_APP_UPDATE_REQ_CODE = 124;
    private static final long serialVersionUID = 1L;
    public static final int PERMISSIONS_REQUEST = 102;

    public static final int DELAY_PERFORM_CLICK = 1000;

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

    public static final int BANNER_AD = 1;
    public static final int INTERSTITIAL_AD = 1;
    public static final int NATIVE_AD = 1;
    public static final String NATIVE_AD_STYLE = "medium";
    public static boolean isAppOpen = false;
    public static boolean isRadioPlaying = false;

}