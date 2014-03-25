package com.mparticle.particlebox;

import android.view.View;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


/**
 * Created by sdozor on 1/22/14.
 */
public class TransactionTestFragment extends MainTransactionTestFragment implements View.OnClickListener {
    public TransactionTestFragment() {
        super();
    }


    @Override
    public void onClick(View v) {
        super.onClick(v);

        Tracker tracker = GoogleAnalytics.getInstance(v.getContext()).getInstance(v.getContext()).newTracker("UA-46924309-4");
        tracker.send(new HitBuilders.TransactionBuilder().setTransactionId(transactionId.getText().toString())
                .setAffiliation(transactionAffiliation.getText().toString())
                        .setRevenue(Double.parseDouble(revenueAmount.getText().toString()))
                        .setTax(Double.parseDouble(taxAmount.getText().toString()))
                        .setShipping(Double.parseDouble(shippingAmount.getText().toString()))
                        .setCurrencyCode(currencyCode.getText().toString()).
                        build());

    }
}
