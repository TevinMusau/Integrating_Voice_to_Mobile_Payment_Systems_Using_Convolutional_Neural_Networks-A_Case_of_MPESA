package com.tevin.mvoice.Interceptor;

import androidx.annotation.NonNull;

import java.io.IOException;
import android.util.Base64;
import com.tevin.mvoice.Secrets;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AccessTokenInterceptor implements Interceptor
{
    // Instance of the Secrets Class (for secret credentials)
    Secrets secrets = new Secrets();

    // constructor
    public AccessTokenInterceptor(){}

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        // Getting the Daraja App Consumer Key and Consumer Secret
        String keys = new StringBuilder().append(secrets.getMVoice_CONSUMER_KEY()).append(":").append(secrets.getMVoice_CONSUMER_SECRET()).toString();

        // passing the credentials to a chain of requests
        // encode the credentials with Base64
        Request request = chain.request().newBuilder()
                .addHeader("Authorization", "Basic " + Base64.encodeToString(keys.getBytes(), Base64.NO_WRAP))
                .build();
        return chain.proceed(request);
    }
}
