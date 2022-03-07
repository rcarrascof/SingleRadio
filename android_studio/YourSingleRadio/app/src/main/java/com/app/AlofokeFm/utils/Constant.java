package com.app.AlofokeFm.utils;

import com.app.AlofokeFm.models.Radio;
import com.google.android.exoplayer2.SimpleExoPlayer;

import java.io.Serializable;
import java.util.ArrayList;

public class Constant implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final int PERMISSIONS_REQUEST = 102;

    public static final int DELAY_PERFORM_CLICK = 1000;

    public static final boolean CIRCLE = true;
    public static final boolean SQUARE = false;

    public static String metadata;
    public static String albumArt;
    public static SimpleExoPlayer simpleExoPlayer;
    public static Boolean is_playing = false;
    public static Boolean radio_type = true;
    public static Boolean is_app_open = false;
    public static ArrayList<Radio> item_radio = new ArrayList<>();
    public static int position = 0;

    public static final String AD_STATUS_ON = "on";
    public static final String ADMOB = "admob";
    public static final String STARTAPP = "startapp";
    public static final String UNITY = "unity";
    public static final String APPLOVIN = "applovin";

    //startapp native ad image parameters
    public static final int STARTAPP_IMAGE_XSMALL = 1; //for image size 100px X 100px
    public static final int STARTAPP_IMAGE_SMALL = 2; //for image size 150px X 150px
    public static final int STARTAPP_IMAGE_MEDIUM = 3; //for image size 340px X 340px
    public static final int STARTAPP_IMAGE_LARGE = 4; //for image size 1200px X 628px

    //unity banner ad size
    public static final int UNITY_ADS_BANNER_WIDTH = 320;
    public static final int UNITY_ADS_BANNER_HEIGHT = 50;

}