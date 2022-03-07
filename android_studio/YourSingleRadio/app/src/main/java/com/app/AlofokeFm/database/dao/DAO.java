package com.app.AlofokeFm.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DAO {

    @Query("INSERT INTO radio (radio_id, radio_name, radio_genre, radio_url, radio_image_url, background_image_url) VALUES (:radio_id, :radio_name, :radio_genre, :radio_url, :radio_image_url, :background_image_url)")
    void insertRadio(long radio_id, String radio_name, String radio_genre, String radio_url, String radio_image_url, String background_image_url);

    @Query("DELETE FROM radio WHERE radio_id = :radio_id")
    void deleteRadio(String radio_id);

    @Query("DELETE FROM radio")
    void deleteAllRadio();

    @Query("SELECT * FROM radio ORDER BY radio_id DESC")
    List<RadioEntity> getAllRadio();

    @Query("SELECT COUNT(radio_id) FROM radio")
    Integer getRadioCount();

    @Query("SELECT * FROM radio WHERE radio_id = :radio_id LIMIT 1")
    RadioEntity getRadio(String radio_id);


    @Query("INSERT INTO social (social_name, social_icon, social_url) VALUES (:social_name, :social_icon, :social_url)")
    void insertSocial(String social_name, String social_icon, String social_url);

    @Query("DELETE FROM social WHERE social_id = :social_id")
    void deleteSocial(long social_id);

    @Query("DELETE FROM social")
    void deleteAllSocial();

    @Query("SELECT * FROM social ORDER BY social_id ASC")
    List<SocialEntity> getAllSocial();

    @Query("SELECT COUNT(social_id) FROM social")
    Integer getSocialCount();

    @Query("SELECT * FROM social WHERE social_id = :social_id LIMIT 1")
    SocialEntity getSocial(long social_id);

}