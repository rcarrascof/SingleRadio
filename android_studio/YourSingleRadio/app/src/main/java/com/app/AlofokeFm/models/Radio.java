package com.app.AlofokeFm.models;

import java.io.Serializable;

public class Radio implements Serializable {

    public long radio_id = -1;
    public String radio_name = "";
    public String radio_genre = "";
    public String radio_url = "";
    public String radio_image_url = "https://yt3.ggpht.com/ytc/AKedOLQejjicjkEJ1oXP96beCSIr3qWhy4-BwXiAcY-lag=s900-c-k-c0x00ffffff-no-rj";
    public String background_image_url = "";
    public String song_metadata = "";
    public String image_album_art = "";
    public String auto_play = "";

    public Radio() {
    }

    public long getRadio_id() {
        return radio_id;
    }

    public void setRadio_id(long radio_id) {
        this.radio_id = radio_id;
    }

    public String getRadio_name() {
        return radio_name;
    }

    public void setRadio_name(String radio_name) {
        this.radio_name = radio_name;
    }

    public String getRadio_genre() {
        return radio_genre;
    }

    public void setRadio_genre(String radio_genre) {
        this.radio_genre = radio_genre;
    }

    public String getRadio_url() {
        return radio_url;
    }

    public void setRadio_url(String radio_url) {
        this.radio_url = radio_url;
    }

    public String getRadio_image_url() {
        return radio_image_url;
    }

    public void setRadio_image_url(String radio_image_url) {
        this.radio_image_url = radio_image_url;
    }

    public String getBackground_image_url() {
        return background_image_url;
    }

    public void setBackground_image_url(String background_image_url) {
        this.background_image_url = background_image_url;
    }

}


