package com.mparticle.particlebox;

import android.app.Activity;
import android.view.View;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONObject;


/**
 * Created by sdozor on 1/22/14.
 */
public class TransactionTestFragment extends MainTransactionTestFragment implements View.OnClickListener {
    private MixpanelAPI mixpanel;

    public TransactionTestFragment() {
        super();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mixpanel =
                MixpanelAPI.getInstance(activity, "b66214bab597da0085dfc3bcc1e44929");
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        try {
            JSONObject properties = new JSONObject();
            properties.put("productName", productName.getText().toString());
            properties.put("productSku", productSku.getText().toString());
            properties.put("quantity", quantity.getText().toString());
            properties.put("unitPrice", unitPrice.getText().toString());
            properties.put("shippingAmount", shippingAmount.getText().toString());
            properties.put("taxAmount", taxAmount.getText().toString());
            properties.put("revenueAmount", revenueAmount.getText().toString());
            properties.put("productCategory", productCategory.getText().toString());
            properties.put("currencyCode", currencyCode.getText().toString());
            properties.put("transactionId", transactionId.getText().toString());
            properties.put("transactionAffiliation", transactionAffiliation.getText().toString());
            mixpanel.getPeople().trackCharge(Double.parseDouble(revenueAmount.getText().toString()), properties);
        } catch (Exception e) {

        }
    }
}
