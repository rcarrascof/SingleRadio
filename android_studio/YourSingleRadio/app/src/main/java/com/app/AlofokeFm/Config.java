package com.app.AlofokeFm;

import com.app.AlofokeFm.utils.Constant;

public class Config {

    //set true to enable json remote configuration with access key
    //set false for offline json configuration from assets folder
    public static final boolean ENABLE_REMOTE_JSON = false;

    //only used when remote json is enabled
    //generate your access key using the link below or check the documentation for more detailed instructions
    //https://services.solodroid.co.id/access-key/generate
    public static final String ACCESS_KEY = "WVVoU01HTklUVFpNZVRsclkyMXNNbHBUTlc1aU1qbHVZa2RWZFZreU9YUk1NbHB3WWtkVmRscERPSGhhYlUxMFpHdE5NbUpJUWsxTk1WWkNWR3hhZGt4WFJrMVNhM1JDVFZkM2RGVnNWa2xOVjJocFQwUlJkbVJ0Ykd4a2Vqa3hZek5CT1dNeWFHaGpiV3gxV2pFNWFHTklRbk5oVjA1b1pFZHNkbUpyYkd0WU1rNTJZbE0xYUdOSVFYVlJWM2gyV20wNWNscFZXblE9";

    //auto play configuration
    public static final boolean ENABLE_AUTOPLAY = false;

    //song metadata
    public static final boolean ENABLE_SONG_METADATA = true;

    //album art metadata, if song metadata is disabled, album art will also disabled
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
    public static final boolean LEGACY_GDPR = true;

    //social menu open url
    public static final boolean OPEN_SOCIAL_MENU_IN_EXTERNAL_BROWSER = false;

}