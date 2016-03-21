package com.mparticle.kits;

import android.app.Activity;
import android.app.Application;
import android.location.Location;
import android.text.TextUtils;

import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.ApptentiveInternal;
import com.apptentive.android.sdk.lifecycle.ApptentiveActivityLifecycleCallbacks;
import com.apptentive.android.sdk.model.CommerceExtendedData;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ApptentiveKit extends AbstractKit implements ClientSideForwarder, ECommerceForwarder, ActivityLifecycleForwarder {
	private static final String API_KEY = "appKey";
	private ApptentiveActivityLifecycleCallbacks callbacks;

	@Override
	protected AbstractKit update() {
		if (callbacks == null) {
			callbacks = new ApptentiveActivityLifecycleCallbacks();
		}
		/* Note mParticle will delegate lifecycle management to the above callbacks. No need to
		 * register ApptentiveActivityLifecycleCallbacks through Apptentive. But do need to initialize Apptentive
         *
         */
		ApptentiveInternal.createInstance(context.getApplicationContext(), properties.get(API_KEY));

		return this;

	}

	@Override
	public Object getInstance(Activity activity) {
		return null;
	}

	@Override
	public String getName() {
		return "Apptentive";
	}

	@Override
	public boolean isOriginator(String uri) {
		return uri != null && uri.toLowerCase().contains("Apptentive.com");
	}


	@Override
	public void setUserIdentity(String id, MParticle.IdentityType identityType) {
		if (identityType.equals(MParticle.IdentityType.Email)) {
			Apptentive.setPersonEmail(id);
		} else if (identityType.equals(MParticle.IdentityType.CustomerId)) {
			if (TextUtils.isEmpty(Apptentive.getPersonName())) {
				// Use id as customer name iff no full name is set yet.
				Apptentive.setPersonName(id);
			}
		}
	}


	@Override
	void setUserAttributes(JSONObject mUserAttributes) {
		String firstName = "";
		String lastName = "";

		Iterator<String> iterator = mUserAttributes.keys();
		while (iterator.hasNext()) {
			String attributeKey = iterator.next();
			try {
				String attributeValue = mUserAttributes.getString(attributeKey);
				if (attributeKey.equalsIgnoreCase(MParticle.UserAttributes.FIRSTNAME)) {
					firstName = attributeValue;
				} else if (attributeKey.equalsIgnoreCase(MParticle.UserAttributes.LASTNAME)) {
					lastName = attributeValue;
				} else {
					Apptentive.addCustomPersonData(attributeKey, attributeValue);
				}
			} catch (JSONException e) {
				ConfigManager.log(MParticle.LogLevel.DEBUG, "Exception while mapping mParticle user attributes to Apptentive custom data: " + e.toString());
			}
		}

		String fullName;
		if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName)) {
			fullName = firstName + " " + lastName;
		} else {
			fullName = firstName + lastName;
		}
		Apptentive.setPersonName(fullName.trim());
	}

	@Override
	void removeUserAttribute(String key) {
		Apptentive.removeCustomPersonData(key);
	}

	@Override
	public List<ReportingMessage> logEvent(MPEvent event) throws Exception {
		Map<String, String> customData = event.getInfo();
		if (customData != null) {
			Apptentive.engage(context, event.getEventName(), Collections.<String, Object>unmodifiableMap(customData));
		} else {
			Apptentive.engage(context, event.getEventName());
		}
		List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
		messageList.add(ReportingMessage.fromEvent(this, event));
		return messageList;
	}

	@Override
	public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
		return null;
	}

	@Override
	public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo) {
		return null;
	}

	@Override
	public List<ReportingMessage> logEvent(CommerceEvent event) throws Exception {
		if (!TextUtils.isEmpty(event.getProductAction())) {
			Map<String, String> eventActionAttributes = new HashMap<String, String>();
			CommerceEventUtil.extractActionAttributes(event, eventActionAttributes);

			CommerceExtendedData apptentiveCommerceData = null;

			TransactionAttributes transactionAttributes = event.getTransactionAttributes();
			if (transactionAttributes != null) {
				apptentiveCommerceData = new CommerceExtendedData();

				String transaction_id = transactionAttributes.getId();
				if (!TextUtils.isEmpty(transaction_id)) {
					apptentiveCommerceData.setId(transaction_id);
				}
				Double transRevenue = transactionAttributes.getRevenue();
				if (transRevenue != null) {
					apptentiveCommerceData.setRevenue(transRevenue);
				}
				Double transShipping = transactionAttributes.getShipping();
				if (transShipping != null) {
					apptentiveCommerceData.setShipping(transShipping);
				}
				Double transTax = transactionAttributes.getTax();
				if (transTax != null) {
					apptentiveCommerceData.setTax(transTax);
				}
				String transAffiliation = transactionAttributes.getAffiliation();
				if (!TextUtils.isEmpty(transAffiliation)) {
					apptentiveCommerceData.setAffiliation(transAffiliation);
				}
				String transCurrency = eventActionAttributes.get(Constants.Commerce.ATT_ACTION_CURRENCY_CODE);
				if (TextUtils.isEmpty(transCurrency)) {
					transCurrency = Constants.Commerce.DEFAULT_CURRENCY_CODE;
				}
				apptentiveCommerceData.setCurrency(transCurrency);

				// Add each item
				List<Product> productList = event.getProducts();
				if (productList != null) {
					for (Product product : productList) {
						CommerceExtendedData.Item item = new CommerceExtendedData.Item();
						item.setId(product.getSku());
						item.setName(product.getName());
						item.setCategory(product.getCategory());
						item.setPrice(product.getUnitPrice());
						item.setQuantity(product.getQuantity());
						item.setCurrency(transCurrency);
						apptentiveCommerceData.addItem(item);
					}
				}
			}

			if (apptentiveCommerceData != null) {
				Map<String, String> customData = event.getCustomAttributes();
				Apptentive.engage(context, String.format("eCommerce - %s", event.getProductAction()),
						Collections.<String, Object>unmodifiableMap(customData), apptentiveCommerceData);
				List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
				messages.add(ReportingMessage.fromEvent(this, event));
				return messages;
			}

		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityCreated(Activity activity, int i) {
		if (callbacks != null) {
			callbacks.onActivityCreated(activity, null);
		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityResumed(Activity activity, int i) {
		if (callbacks != null) {
			callbacks.onActivityResumed(activity);
		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityPaused(Activity activity, int i) {
		if (callbacks != null) {
			callbacks.onActivityPaused(activity);
		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityStopped(Activity activity, int i) {
		if (callbacks != null) {
			callbacks.onActivityStopped(activity);
		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityStarted(Activity activity, int i) {
		if (callbacks != null) {
			callbacks.onActivityStarted(activity);
		}
		return null;
	}
}