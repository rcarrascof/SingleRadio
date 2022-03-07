package com.app.AlofokeFm.rests;

import com.app.AlofokeFm.callbacks.CallbackAlbumArt;
import com.app.AlofokeFm.callbacks.CallbackConfig;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface ApiInterface {

    String CACHE = "Cache-Control: max-age=0";
    String AGENT = "Data-Agent: Your Single Radio";

    @Headers({"Cache-Control: max-age=0"})
    @GET
    Call<CallbackConfig> getConfig(@Url String url);

    @Headers({CACHE, AGENT})
    @GET("search")
    Call<CallbackAlbumArt> getAlbumArt(
            @Query("term") String term,
            @Query("media") String media,
            @Query("limit") int limit
    );

}
