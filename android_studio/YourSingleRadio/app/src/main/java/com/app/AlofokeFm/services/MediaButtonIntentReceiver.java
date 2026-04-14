package com.app.AlofokeFm.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

    public MediaButtonIntentReceiver() {
        super();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(context, "Received!", Toast.LENGTH_SHORT).show();
        String intentAction = intent.getAction();
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            return;
        }
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event == null) {
            return;
        }
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            if (RadioPlayerService.getInstance() != null) {
                Intent intent_pause = new Intent(context, RadioPlayerService.class);
                intent_pause.setAction(RadioPlayerService.ACTION_TOGGLE);
                context.startService(intent_pause);
            }
        }
//        else if (action == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
//            Radio radio = radios.get(0);
//            final Intent intent = new Intent(context, RadioPlayerService.class);
//            if (RadioPlayerService.getInstance() != null) {
//                Radio playerCurrentRadio = RadioPlayerService.getInstance().getPlayingRadioStation();
//                if (playerCurrentRadio != null) {
//                    if (radio.getRadio_id() != RadioPlayerService.getInstance().getPlayingRadioStation().getRadio_id()) {
//                        RadioPlayerService.getInstance().initializeRadio(context, radio);
//                        intent.setAction(RadioPlayerService.ACTION_PLAY);
//                    } else {
//                        intent.setAction(RadioPlayerService.ACTION_TOGGLE);
//                    }
//                } else {
//                    RadioPlayerService.getInstance().initializeRadio(context, radio);
//                    intent.setAction(RadioPlayerService.ACTION_PLAY);
//                }
//            } else {
//                RadioPlayerService.createInstance().initializeRadio(context, radio);
//                intent.setAction(RadioPlayerService.ACTION_PLAY);
//            }
//            context.startService(intent);
//        }
        abortBroadcast();
    }
}