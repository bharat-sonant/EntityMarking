package com.wevois.entityreverification;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface WardBoundaryAPI {

    @GET("Jaipur-Malviyanagar%2FWardBoundryJson%2F125-R1.json")
    Call<WardBoundaryModel> getSessionToken();

}
