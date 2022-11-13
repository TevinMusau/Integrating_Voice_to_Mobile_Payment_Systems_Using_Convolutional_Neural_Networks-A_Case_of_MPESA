package com.tevin.mvoice;

public class Constants
{
    // define request connection, read and write timeouts
    public static final int CONNECT_TIMEOUT = 60 * 1000;
    public static final int READ_TIMEOUT = 60 * 1000;
    public static final int WRITE_TIMEOUT = 60 * 1000;

    // Base URL
    public static final String BASE_URL = "https://sandbox.safaricom.co.ke/";

    // define the parameters for payment
    public static final String BUSINESS_SHORT_CODE = "174379";

    // client's phone number
    public static final String PHONE_NUMBER = "254722158350";
    public static final String PASSKEY = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919";
    public static final String TRANSACTION_TYPE = "CustomerPayBillOnline";

    // Daraja Sandbox
    public static final String PARTYB = "174379";

    // online request bin
    public static final String CALLBACKURL = "https://eot4v7bfc2ghens.m.pipedream.net/";
}
