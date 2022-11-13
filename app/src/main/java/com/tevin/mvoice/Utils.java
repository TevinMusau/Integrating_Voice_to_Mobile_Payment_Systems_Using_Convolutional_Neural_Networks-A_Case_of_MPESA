package com.tevin.mvoice;

import java.text.SimpleDateFormat;
import android.util.Base64;
import java.util.Date;
import java.util.Locale;

public class Utils {

    // get the current timestamp
    public static String getTimestamp(){
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
    }

    /*
     * The Password in DarajaAPI has a unique format
     * The Password changes after every request
     * It is set as the Business Short Code & the PassKey & the Timestamp
     * We encode the Password generated
    */
    public static String getPassword(String businessShortCode, String passkey, String timestamp){
        String str = businessShortCode + passkey + timestamp;

        // encode the password to Base64
        return Base64.encodeToString(str.getBytes(), Base64.NO_WRAP);
    }

}
