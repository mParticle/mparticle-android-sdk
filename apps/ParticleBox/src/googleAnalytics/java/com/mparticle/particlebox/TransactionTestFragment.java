package com.mparticle.particlebox;

import android.view.View;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;


/**
 * Created by sdozor on 1/22/14.
 */
public class TransactionTestFragment extends MainTransactionTestFragment implements View.OnClickListener {
    public TransactionTestFragment() {
        super();
    }


    @Override
    public void onClick(View v) {
      //  super.onClick(v);
        if (v.getId() == R.id.button) {
            EasyTracker tracker = EasyTracker.getInstance(getActivity());

            tracker.send(MapBuilder.createTransaction(transactionId.getText().toString(),
                    transactionAffiliation.getText().toString(),
                    Double.parseDouble(revenueAmount.getText().toString()),
                    Double.parseDouble(taxAmount.getText().toString()),
                    Double.parseDouble(shippingAmount.getText().toString()),
                    currencyCode.getText().toString()).
                            build());

            tracker.send(MapBuilder
                    .createItem(transactionId.getText().toString(),               // (String) Transaction ID
                            productName.getText().toString(),      // (String) Product name
                            productSku.getText().toString(),                  // (String) Product SKU
                            productCategory.getText().toString(),        // (String) Product category
                            Double.parseDouble(unitPrice.getText().toString()),                    // (Double) Product price
                            Long.parseLong(quantity.getText().toString()),                       // (Long) Product quantity
                            currencyCode.getText().toString())                    // (String) Currency code
                    .build());
        }

    }
}
