package com.tevin.mvoice.Services;

import com.tevin.mvoice.model.AccessToken;
import com.tevin.mvoice.model.STKPush;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.Call;

public interface STKPushService {
    // POST Request URL
    @POST("mpesa/stkpush/v1/processrequest")
    Call<STKPush> sendPush(@Body STKPush stkPush);

    // GET Request URL
    @GET("oauth/v1/generate?grant_type=client_credentials")
    Call<AccessToken> getAccessToken();
}
