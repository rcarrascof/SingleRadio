package com.app.Ritmo96.database.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.app.Ritmo96.models.Radio;
import com.google.gson.annotations.Expose;

@Entity(tableName = "radio")
public class RadioEntity {

    @PrimaryKey
    public long radio_id = -1;

    @Expose
    @ColumnInfo(name = "radio_name")
    public String radio_name = "";

    @Expose
    @ColumnInfo(name = "radio_genre")
    public String radio_genre = "";

    @Expose
    @ColumnInfo(name = "radio_url")
    public String radio_url = "";

    @Expose
    @ColumnInfo(name = "radio_image_url")
    public String radio_image_url = "";


    @Expose
    @ColumnInfo(name = "background_image_url")
    public String background_image_url = "";

    public RadioEntity() {
    }

    public static RadioEntity entity(Radio radio) {
        RadioEntity entity = new RadioEntity();
        entity.radio_id = radio.radio_id;
        entity.radio_name = radio.radio_name;
        entity.radio_genre = radio.radio_genre;
        entity.radio_url = radio.radio_url;
        entity.radio_image_url = radio.radio_image_url;
        entity.background_image_url = radio.background_image_url;
        return entity;
    }

    public Radio original() {
        Radio radio = new Radio();
        radio.radio_id = radio_id;
        radio.radio_name = radio_name;
        radio.radio_genre = radio_genre;
        radio.radio_url = radio_url;
        radio.radio_image_url = radio_image_url;
        radio.background_image_url = background_image_url;
        return radio;
    }
}