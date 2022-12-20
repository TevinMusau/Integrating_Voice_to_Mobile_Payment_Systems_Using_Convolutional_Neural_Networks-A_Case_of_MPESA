package com.tevin.mvoice.Services;

import static com.tevin.mvoice.Constants.BASE_URL;
import static com.tevin.mvoice.Constants.CONNECT_TIMEOUT;
import static com.tevin.mvoice.Constants.READ_TIMEOUT;
import static com.tevin.mvoice.Constants.WRITE_TIMEOUT;

import com.tevin.mvoice.Interceptor.AccessTokenInterceptor;
import com.tevin.mvoice.Interceptor.AuthInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DarajaApiClient {

    // variable initialization
    private Retrofit retrofit;
    private boolean isDebug;
    private boolean isGetAccessToken;
    private String mAuthToken;
    private HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();

    // Check if we are in Debug or Production mode
    public DarajaApiClient setIsDebug(boolean isDebug){
        this.isDebug = isDebug;
        return this;
    }

    // Setting the Auth Token
    public DarajaApiClient setAuthToken(String authToken){
        this.mAuthToken = authToken;
        return this;
    }

    public DarajaApiClient setGetAccessToken(boolean getAccessToken){
        this.isGetAccessToken = getAccessToken;
        return this;
    }

    private OkHttpClient.Builder okHttpClient() {
        OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder();
        okHttpClient
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(httpLoggingInterceptor);

        return okHttpClient;
    }

    private Retrofit getRestAdapter(){
        Retrofit.Builder builder = new Retrofit.Builder();

        // setting base URL
        builder.baseUrl(BASE_URL);

        builder.addConverterFactory(GsonConverterFactory.create());

        if (isDebug){
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        }

        OkHttpClient.Builder okHttpBuilder = okHttpClient();

        // check of we got an access token
        if (isGetAccessToken){
            okHttpBuilder.addInterceptor(new AccessTokenInterceptor());
        }

        if (mAuthToken != null && !mAuthToken.isEmpty()){
            okHttpBuilder.addInterceptor(new AuthInterceptor(mAuthToken));
        }

        builder.client(okHttpBuilder.build());

        retrofit = builder.build();

        return retrofit;
    }

    // sending request to the Daraja API
    public STKPushService mpesaService()
    {
        return getRestAdapter().create(STKPushService.class);
    }

}