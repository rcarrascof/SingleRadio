package com.app.AlofokeFm;

import com.app.AlofokeFm.utils.Constant;

public class Config {

    //set true to enable remote config from json url, or set false for offline json config from assets
    public static final boolean ENABLE_REMOTE_JSON = false;

    //only used when remote json is enabled
    public static final String JSON_URL = "http://10.0.2.2/your_single_radio/config.json";

    //auto play configuration
    public static final boolean ENABLE_AUTOPLAY = false;

    //display album art metadata
    public static final boolean ENABLE_ALBUM_ART_METADATA = true;

    //display album art metadata with parameter : Constant.CIRCLE or Constant.SQUARE
    public static final boolean CIRCULAR_RADIO_IMAGE_ALBUM_ART = Constant.CIRCLE;

    //radio will stop when receiving a phone call and will resume when the call ends
    public static final boolean RESUME_RADIO_ON_PHONE_CALL = true;

    //splash screen duration in millisecond
    public static final int SPLASH_DURATION = 1000;

    //set true if you want to enable RTL (Right To Left) mode, e.g : Arabic Language
    public static final boolean ENABLE_RTL_MODE = false;

    //GDPR EU Consent
    public static final boolean USE_LEGACY_GDPR_EU_CONSENT = true;

    //social menu open url
    public static final boolean OPEN_SOCIAL_MENU_IN_EXTERNAL_BROWSER = false;

}