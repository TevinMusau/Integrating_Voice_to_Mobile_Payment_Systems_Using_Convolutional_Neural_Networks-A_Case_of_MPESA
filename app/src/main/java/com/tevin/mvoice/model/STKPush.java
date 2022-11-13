package com.tevin.mvoice.model;

import com.google.gson.annotations.SerializedName;

public class STKPush {
    @SerializedName("BusinessShortCode")
    private String BusinessShortCode;

    @SerializedName("Password")
    private String Password;

    @SerializedName("Timestamp")
    private String Timestamp;

    @SerializedName("TransactionType")
    private String TransactionType;

    @SerializedName("Amount")
    private String amount;

    @SerializedName("PartyA")
    private String PartyA;

    @SerializedName("PartyB")
    private String PartyB;

    @SerializedName("PhoneNumber")
    private String PhoneNumber;

    @SerializedName("CallBackURL")
    private String CallbackURL;

    @SerializedName("AccountReference")
    private String AccountReference;

    @SerializedName("TransactionDesc")
    private String TransactionDesc;

    public STKPush(String BusinessShortCode, String Password, String Timestamp,
                   String TransactionType, String amount, String PartyA,
                   String PartyB, String PhoneNumber, String CallbackURL,
                   String AccountReference, String TransactionDesc)
    {
        this.BusinessShortCode = BusinessShortCode;
        this.Password = Password;
        this.Timestamp = Timestamp;
        this.TransactionType = TransactionType;
        this.amount = amount;
        this.PartyA = PartyA;
        this.PartyB = PartyB;
        this.PhoneNumber = PhoneNumber;
        this.CallbackURL = CallbackURL;
        this.AccountReference = AccountReference;
        this.TransactionDesc = TransactionDesc;
    }
}