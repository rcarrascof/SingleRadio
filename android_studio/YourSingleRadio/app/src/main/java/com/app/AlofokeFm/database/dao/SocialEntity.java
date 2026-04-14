package com.app.AlofokeFm.database.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

@Entity(tableName = "social")
public class SocialEntity {

    @PrimaryKey
    public long social_id = System.currentTimeMillis();

    @Expose
    @ColumnInfo(name = "social_name")
    public String social_name = "";

    @Expose
    @ColumnInfo(name = "social_icon")
    public String social_icon = "";

    @Expose
    @ColumnInfo(name = "social_url")
    public String social_url = "";

    @Ignore
    public SocialEntity() {
    }

    public SocialEntity(String social_name, String social_icon, String social_url) {
        this.social_name = social_name;
        this.social_icon = social_icon;
        this.social_url = social_url;
    }

}