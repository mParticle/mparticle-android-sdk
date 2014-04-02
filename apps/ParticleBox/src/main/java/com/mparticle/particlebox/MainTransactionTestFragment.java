package com.mparticle.particlebox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.mparticle.MPProduct;
import com.mparticle.MParticle;

/**
 * Created by sdozor on 1/22/14.
 */
public class MainTransactionTestFragment extends Fragment implements View.OnClickListener {
    protected EditText productName, productSku, quantity, unitPrice, shippingAmount, taxAmount, revenueAmount, productCategory, currencyCode, transactionId, transactionAffiliation, ltvEditText;

    public MainTransactionTestFragment() {
        super();
    }

    private static final String ARG_SECTION_NUMBER = "section_number";

    public static MainTransactionTestFragment newInstance(int sectionNumber) {
        MainTransactionTestFragment fragment = new TransactionTestFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_transactions, container, false);
        v.findViewById(R.id.button).setOnClickListener(this);
        v.findViewById(R.id.ltvButton).setOnClickListener(this);
        productName = (EditText) v.findViewById(R.id.productName);
        productSku = (EditText) v.findViewById(R.id.productSku);
        quantity = (EditText) v.findViewById(R.id.quantity);
        unitPrice = (EditText) v.findViewById(R.id.unitPrice);
        shippingAmount = (EditText) v.findViewById(R.id.shipping);
        taxAmount = (EditText) v.findViewById(R.id.tax);
        revenueAmount = (EditText) v.findViewById(R.id.revenue);
        productCategory = (EditText) v.findViewById(R.id.category);
        currencyCode = (EditText) v.findViewById(R.id.currencyCode);
        transactionId = (EditText) v.findViewById(R.id.transactionId);
        transactionAffiliation = (EditText) v.findViewById(R.id.transAffiliation);
        ltvEditText = (EditText) v.findViewById(R.id.ltv);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ltvButton){
            MParticle.getInstance().logLtvIncrease(Double.parseDouble(ltvEditText.getText().toString()));
            Toast.makeText(v.getContext(), "LTV increase logged.", Toast.LENGTH_SHORT).show();
        }else{
            MPProduct transaction = new MPProduct.Builder(productName.getText().toString(), productSku.getText().toString())
                    .quantity(Integer.parseInt(quantity.getText().toString()))
                    .unitPrice(Double.parseDouble(unitPrice.getText().toString()))
                    .shippingAmount(Double.parseDouble(shippingAmount.getText().toString()))
                    .taxAmount(Double.parseDouble(taxAmount.getText().toString()))
                    .totalRevenue(Double.parseDouble(revenueAmount.getText().toString()))
                    .productCategory(productCategory.getText().toString())
                    .currencyCode(currencyCode.getText().toString())
                    .transactionId(transactionId.getText().toString())
                    .affiliation(transactionAffiliation.getText().toString())
                    .build();
            MParticle.getInstance().logTransaction(transaction);

            Toast.makeText(v.getContext(), "Transaction logged.", Toast.LENGTH_SHORT).show();
        }

    }
}
