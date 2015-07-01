package com.mparticle.commerce;

public class TransactionAttributes {
    private String mAffiliation = null;
    private Double mRevenue;
    private Double mShipping;
    private Double mTax;
    private String mCouponCode = null;
    private String mId = null;

    public String getId() {
        return mId;
    }

    /**
     * Create a TransactionAttributes object to be associated with a {@link CommerceEvent}.
     *
     * Note that transaction ID is required for {@link CommerceEvent#PURCHASE} and {@link CommerceEvent#REFUND} events.
     *
     */
    public TransactionAttributes() {
        super();
    }

    /**
     * Create a TransactionAttributes object to be associated with a {@link CommerceEvent}.
     *
     * Note that transaction ID is required for {@link CommerceEvent#PURCHASE} and {@link CommerceEvent#REFUND} events.
     *
     * @param transactionId a unique ID for this transaction
     */
    public TransactionAttributes(String transactionId) {
        super();
        setId(transactionId);
    }


    public TransactionAttributes setId(String id) {
        mId = id;
        return this;
    }

    public String getCouponCode() {
        return mCouponCode;
    }

    public TransactionAttributes setCouponCode(String couponCode) {
        this.mCouponCode = couponCode;
        return this;
    }

    public Double getTax() {
        return mTax;
    }

    public TransactionAttributes setTax(double tax) {
        this.mTax = tax;
        return this;
    }

    public Double getShipping() {
        return mShipping;
    }

    public TransactionAttributes setShipping(double shipping) {
        this.mShipping = shipping;
        return this;
    }

    public Double getRevenue() {
        return mRevenue;
    }

    public TransactionAttributes setRevenue(double revenue) {
        this.mRevenue = revenue;
        return this;
    }

    public String getAffiliation() {
        return mAffiliation;
    }

    public TransactionAttributes setAffiliation(String affiliation) {
        this.mAffiliation = affiliation;
        return this;
    }
}