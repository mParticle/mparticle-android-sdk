package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by sdozor on 3/13/14.
 */
public class EmbeddedMAT extends EmbeddedProvider{

    public EmbeddedMAT(Context context) throws ClassNotFoundException {
        super(context);
        try {
            Class.forName("com.mobileapptracker.MobileAppTracker");
        }catch (ClassNotFoundException cnfe){
            if (MParticle.getInstance().getDebugMode()){
                Log.w(Constants.LOG_TAG, "Failed in initiate MAT - library not found. Have you added it to your application's classpath?");
            }
            throw cnfe;
        }
    }

    @Override
    public void logEvent() {

    }

    @Override
    public void onCreate(Activity activity) {
        com.mobileapptracker.MobileAppTracker.getInstance().setReferralSources(activity);
    }

    @Override
    public void onResume(Activity activity) {
        com.mobileapptracker.MobileAppTracker.getInstance().measureSession();
    }

    @Override
    public void logTransaction(MPTransaction transaction) {
        JSONObject transData = transaction.getData();
        com.mobileapptracker.MobileAppTracker instance = com.mobileapptracker.MobileAppTracker.getInstance();
        com.mobileapptracker.MATEventItem item = new com.mobileapptracker.MATEventItem(
                transData.optString(MPTransaction.NAME),
                transData.optInt(MPTransaction.QUANTITY, 1),
                transData.optDouble(MPTransaction.UNITPRICE,0),
                transData.optDouble(MPTransaction.REVENUE,0),
                transData.optString(MPTransaction.TAX),
                transData.optString(MPTransaction.SHIPPING),
                transData.optString(MPTransaction.SKU),
                transData.optString(MPTransaction.AFFILIATION),
                transData.optString(MPTransaction.CATEGORY)
                );
        instance.measureAction("purchase",
                item,
                transData.optDouble(MPTransaction.REVENUE, 0),
                transData.optString(MPTransaction.CURRENCY,"USD"),
                transData.optString(MPTransaction.TRANSACTION_ID, UUID.randomUUID().toString())
        );
    }

    @Override
    protected EmbeddedProvider init() {
        com.mobileapptracker.MobileAppTracker.init(context,properties.get("AdvertiserId"),properties.get("ConversionKey"));
        return this;
    }

    @Override
    public String getName() {
        return "Mobile App Tracking";
    }
}
