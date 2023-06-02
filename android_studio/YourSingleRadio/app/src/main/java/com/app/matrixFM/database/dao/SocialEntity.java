package com.app.matrixFM.database.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.app.matrixFM.models.Social;
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

    public SocialEntity() {
    }

    public Social original() {
        Social social = new Social();
        social.social_name = social_name;
        social.social_icon = social_icon;
        social.social_url = social_url;
        return social;
    }
}