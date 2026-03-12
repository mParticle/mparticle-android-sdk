package com.mparticle.branchsample.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.branchsample.R;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.identity.IdentityApiRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.SharingHelper;
import io.branch.referral.util.BranchContentSchema;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ProductCategory;
import io.branch.referral.util.ShareSheetStyle;

public class HomeActivity extends AppCompatActivity {
    public static final String BRANCH_PARAMS = "branch_params";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        ((ToggleButton) findViewById(R.id.tracking_cntrl_btn)).setChecked(MParticle.getInstance().getOptOut());

        findViewById(R.id.cmdSetIdentity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MParticle.getInstance().Identity().login(IdentityApiRequest.withEmptyUser()
                        .email("foo@example.com")
                        .customerId("12332424555")
                        .build());
            }
        });

        findViewById(R.id.cmdLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MParticle.getInstance().Identity().logout();
            }
        });

        findViewById(R.id.cmdTrackView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logScreen();
            }
        });

        findViewById(R.id.cmdLogSimpleEvent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logSimpleEvent();
            }
        });

        findViewById(R.id.cmdShareBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareBranchLink();
            }
        });

        findViewById(R.id.cmdTrackEvent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logCommerceEvent((String) ((Spinner) findViewById(R.id.event_name_spinner)).getSelectedItem());
            }
        });

        ((ToggleButton) findViewById(R.id.tracking_cntrl_btn)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MParticle.getInstance().setOptOut(isChecked);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        ((TextView) findViewById(R.id.deep_link_params_txt)).setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (!TextUtils.isEmpty(intent.getStringExtra(BRANCH_PARAMS))) {
            String deepLinkParams = getIntent().getStringExtra(BRANCH_PARAMS);
            try {
                String deepLikMsg = "Branch Deep Link Params \n\n" + new JSONObject(deepLinkParams).toString(4);
                ((TextView) findViewById(R.id.deep_link_params_txt)).setText(deepLikMsg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            intent.removeExtra(BRANCH_PARAMS);
            setIntent(intent);
        } else {
            ((TextView) findViewById(R.id.deep_link_params_txt)).setText("");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    private void logScreen() {
        Map<String, String> eventInfo = new HashMap<>(2);
        eventInfo.put("screen_attr_key1", "screen_attr_val1");
        eventInfo.put("screen_attr_key2", "screen_attr_val2");
        MParticle.getInstance().logScreen("SecondActivity", eventInfo);
    }

    private void logSimpleEvent() {
        Map<String, String> eventInfo = new HashMap<>(2);
        eventInfo.put("custom_attr_key1", "custom_attr_val1");
        eventInfo.put("custom_attr_key2", "custom_attr_val2");

        MPEvent event = new MPEvent.Builder("Simple Event", MParticle.EventType.Transaction)
                .duration(100)
                .info(eventInfo)
                .category("Food and Beverages")
                .build();
        MParticle.getInstance().logEvent(event);
    }

    private void logCommerceEvent(String eventName) {

        Map<String, String> customAttr = new HashMap<>(2);
        customAttr.put("custom_attr_key1", "custom_attr_val1");
        customAttr.put("custom_attr_key2", "custom_attr_val2");

        Product product1 = new Product.Builder("Prod1", "my_sku", 100.00)
                .brand("my_prod_brand")
                .category("my_prod_category")
                .couponCode("my_coupon_code")
                .customAttributes(customAttr)
                .name("my_prod_name")
                .position(1)
                .quantity(2.5)
                .sku("my_sku")
                .unitPrice(12.5)
                .variant("my_variant")
                .quantity(4)
                .build();

        Product product2 = new Product.Builder("Impression_prod", "my_sku", 100.00)
                .brand("my_prod_brand")
                .category("my_prod_category")
                .couponCode("my_coupon_code")
                .customAttributes(customAttr)
                .name("my_prod_name")
                .position(1)
                .quantity(2.5)
                .sku("my_sku")
                .unitPrice(12.5)
                .variant("my_variant")
                .quantity(4)
                .build();

        Product product3 = new Product.Builder("prod3", "my_sku", 100.00)
                .brand("my_prod_brand")
                .category("my_prod_category")
                .couponCode("my_coupon_code")
                .customAttributes(customAttr)
                .name("my_prod_name")
                .position(1)
                .quantity(2.5)
                .sku("my_sku")
                .unitPrice(12.5)
                .variant("my_variant")
                .quantity(4)
                .build();

        TransactionAttributes attributes = new TransactionAttributes("foo-transaction-id")
                .setCouponCode("transaction_coupon_code")
                .setAffiliation("transaction_affiliation")
                .setId("transaction_id")
                .setRevenue(13.5)
                .setShipping(3.5)
                .setTax(4.5);

        Impression impression = new Impression("Impression", product2);

        CommerceEvent commerceEvent = new CommerceEvent.Builder(eventName, product1)
                .currency("USD")
                .customAttributes(customAttr)
                .transactionAttributes(attributes)
                .addImpression(impression)
                .productListName("my_commerce_event_prod_list")
                .addProduct(product3)
                .build();
        MParticle.getInstance().logEvent(commerceEvent);
    }

    private void shareBranchLink() {
        BranchUniversalObject buo = new BranchUniversalObject()
                .setCanonicalIdentifier("item/12345")
                .setCanonicalUrl("https://branch.io/deepviews")
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
                .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                .setTitle("My Content Title")
                .setContentDescription("my_product_description1")
                .setContentImageUrl("https://example.com/mycontent-12345.png")
                .setContentExpiration(new Date(1573415635000L))
                .setContentImageUrl("https://test_img_url")
                .addKeyWord("My_Keyword1")
                .addKeyWord("My_Keyword2")
                .setContentMetadata(
                        new ContentMetadata().setProductName("my_product_name1")
                                .setProductBrand("my_prod_Brand1")
                                .setProductVariant("3T")
                                .setProductCategory(ProductCategory.BABY_AND_TODDLER)
                                .setProductCondition(ContentMetadata.CONDITION.EXCELLENT)
                                .setAddress("Street_name1", "city1", "Region1", "Country1", "postal_code")
                                .setLocation(12.07, -97.5)
                                .setSku("1994320302")
                                .setRating(6.0, 5.0, 7.0, 5)
                                .addImageCaptions("my_img_caption1", "my_img_caption_2")
                                .setQuantity(2.0)
                                .setPrice(23.2, CurrencyType.USD)
                                .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
                                .addCustomMetadata("Custom_Content_metadata_key1", "Custom_Content_metadata_val1")
                );


        LinkProperties linkProperties = new LinkProperties()
                .addTag("Tag1")
                .setChannel("Sharing_Channel_name")
                .setFeature("my_feature_name")
                .addControlParameter("$android_deeplink_path", "custom/path/*")
                .addControlParameter("$ios_url", "http://example.com/ios")
                .setDuration(100);

        ShareSheetStyle shareSheetStyle = new ShareSheetStyle(this, "My Sharing Message Title", "My Sharing message body")
                .setCopyUrlStyle(getResources().getDrawable(android.R.drawable.ic_menu_send), "Save this URl", "Link added to clipboard")
                .setMoreOptionStyle(getResources().getDrawable(android.R.drawable.ic_menu_search), "Show more")
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.MESSAGE)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.TWITTER)
                .setAsFullWidthStyle(true)
                .setSharingTitle("Share With");

        buo.showShareSheet(this, linkProperties, shareSheetStyle, null);
    }

}
