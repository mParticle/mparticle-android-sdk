package com.mparticle.commerce;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class representing an internal promotions within an app, such as banners.
 *
 * This class exposes constants to be used in conjection with a {@link CommerceEvent} to track
 * when a Promotion is viewed or tapped.
 *
 * Example {@link CommerceEvent} usage:
 *
 * <pre>
 *     {@code
 *     Promotion promotion = new Promotion()
 *              .setName("my banner ad")
 *              .setId("ad_001")
 *              .setCreative("hero image");
 *     CommerceEvent promotionEvent = new CommerceEvent.Builder(Promotion.CLICK, promotion).build();
 *     MParticle.getInstance().logEvent(promotionEvent);
 *     }
 * </pre>
 */
public class Promotion {

    @Retention(RetentionPolicy.CLASS)
    @StringDef({CLICK, VIEW})
    @interface PromotionConstant {
    }

    /**
     * The Promotion action constant used to track when a user taps or clicks on a Promotion.
     */
    @NonNull
    public static final String CLICK = "click";
    /**
     * The Promotion action constant used to track when a user views a Promotion.
     */
    @NonNull
    public static final String VIEW = "view";

    private String mCreative = null;
    private String mId = null;
    private String mName = null;
    private String mPosition = null;

    /**
     * Create an empty Promotion. You should typically at least
     * set the ID or Name of a Promotion.
     */
    public Promotion() {
        super();
    }

    /**
     * Copy constructor to duplicate an existing Promotion.
     *
     * @param promotion return the duplicated Promotion object
     */
    public Promotion(@NonNull Promotion promotion) {
        if (promotion != null) {
            mCreative = promotion.getCreative();
            mId = promotion.getId();
            mName = promotion.getName();
            mPosition = promotion.getPosition();
        }
    }

    /**
     * Get the description of the creative in this Promotion.
     *
     * @return returns the name of the creative associated with this Promotion
     */
    @Nullable
    public String getCreative() {
        return mCreative;
    }

    /**
     * Set the name of the creative to associate with this Promotion.
     *
     * @param creative
     * @return returns this Promotion object for easy method chaining
     */
    @NonNull
    public Promotion setCreative(@Nullable String creative) {
        mCreative = creative;
        return this;
    }

    /**
     * Get the unique ID of this Promotion.
     *
     * @return
     */
    @Nullable
    public String getId() {
        return mId;
    }

    /**
     * Set a unique ID to associate with this Promotion.
     *
     * @param id
     * @return returns this Promotion object for easy method chaining
     */
    @NonNull
    public Promotion setId(@Nullable String id) {
        mId = id;
        return this;
    }

    /**
     * Get the name associated with this Promotion.
     *
     * @return
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Set a name to associate with this Promotion.
     *
     * @param name
     * @return returns this Promotion object for easy method chaining
     */
    @NonNull
    public Promotion setName(@Nullable String name) {
        mName = name;
        return this;
    }

    /**
     * Get the position of this Promotion on the page.
     *
     * @return
     */
    @Nullable
    public String getPosition() {
        return mPosition;
    }

    /**
     * Set the description of the position of this Promotion on the page.
     *
     * @param position
     * @return returns this Promotion object for easy method chaining
     */
    @NonNull
    public Promotion setPosition(@Nullable String position) {
        mPosition = position;
        return this;
    }

    @Override
    @NonNull
    public String toString() {
        JSONObject json = new JSONObject();
        try {
            if (!MPUtility.isEmpty(getId())) {
                json.put("id", getId());
            }
            if (!MPUtility.isEmpty(getName())) {
                json.put("nm", getName());
            }
            if (!MPUtility.isEmpty(getCreative())) {
                json.put("cr", getCreative());
            }
            if (!MPUtility.isEmpty(getPosition())) {
                json.put("ps", getPosition());
            }
        } catch (JSONException jse) {

        }
        return json.toString();
    }
}
