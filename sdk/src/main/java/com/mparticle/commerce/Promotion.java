package com.mparticle.commerce;


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

    /**
     * The Promotion action constant used to track when a user taps or clicks on a Promotion
     *
     */
    public static final String CLICK = "click";
    /**
     * The Promotion action constant used to track when a user views a Promotion
     *
     */
    public static final String VIEW = "view";

    private String mCreative = null;
    private String mId = null;
    private String mName = null;
    private String mPosition = null;

    /**
     * Create an empty Promotion. You should typically at least
     * set the ID or Name of a Promotion
     */
    public Promotion() {
        super();
    }

    /**
     * Copy constructor to duplicate an existing Promotion
     *
     * @param promotion return the duplicated Promotion object
     */
    public Promotion(Promotion promotion) {
        if (promotion != null) {
            mCreative = promotion.getCreative();
            mId = promotion.getId();
            mName = promotion.getName();
            mPosition = promotion.getPosition();
        }
    }

    /**
     * Get the description of the creative in this Promotion
     *
     * @return returns the name of the creative associated with this Promotion
     */
    public String getCreative() {
        return mCreative;
    }

    /**
     * Set the name of the creative to associate with this Promotion
     *
     * @param creative
     * @return returns this Promotion object for easy method chaining
     */
    public Promotion setCreative(String creative) {
        mCreative = creative;
        return this;
    }

    /**
     * Get the unique ID of this Promotion
     *
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * Set a unique ID to associate with this Promotion
     *
     * @param id
     * @return returns this Promotion object for easy method chaining
     */
    public Promotion setId(String id) {
        mId = id;
        return this;
    }

    /**
     * Get the name associated with this Promotion
     *
     * @return
     */
    public String getName() {
        return mName;
    }

    /**
     * Set a name to associate with this Promotion
     *
     * @param name
     * @return returns this Promotion object for easy method chaining
     */
    public Promotion setName(String name) {
        mName = name;
        return this;
    }

    /**
     * Get the position of this Promotion on the page
     *
     * @return
     */
    public String getPosition() {
        return mPosition;
    }

    /**
     * Set the description of the position of this Promotion on the page
     *
     * @param position
     * @return returns this Promotion object for easy method chaining
     */
    public Promotion setPosition(String position) {
        mPosition = position;
        return this;
    }

}
