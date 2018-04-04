package com.mparticle.commerce;


import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * The Cart has a one-to-one relationship with MParticleUsers.
 * <p></p>
 * The Cart will persist state across app-restarts.
 * <p></p>
 * You may access the cart via the {@link MParticleUser} object:
 * <p></p>
 * <pre>
 * {@code
 * MParticle.getInstance().Identity().getCurrentUser().cart()}
 * </pre>
 * <p></p>
 * You should not instantiate this class directly
 * <p></p>
 */
public final class Cart {

    private final List<Product> productList;
    public final static int DEFAULT_MAXIMUM_PRODUCT_COUNT = 30;
    private static int MAXIMUM_PRODUCT_COUNT = DEFAULT_MAXIMUM_PRODUCT_COUNT;
    private long userId;
    private Context mContext;

    public Cart(Context context, long userId) {
        mContext = context;
        productList = new LinkedList<Product>();
        this.userId = userId;
        loadFromString(ConfigManager.getUserStorage(context, this.userId).getSerializedCart());
    }

    /**
     * Set the maximum product count to hold in the cart. On memory constrained devices/apps,
     * this value can be lowered to avoid possible memory exceptions.
     *
     * @param maximum
     */
    public static void setMaximumProductCount(final int maximum) {
        MAXIMUM_PRODUCT_COUNT = maximum;
    }

    /**
     * Reset the cart and re-populate it from a String representation. This method allows you to support
     * multiple carts per user.
     *
     * @param cartJson a JSON-encoded string acquired from {@link #toString()}
     */
    public synchronized void loadFromString(String cartJson) {
        if (cartJson != null) {
            try {
                JSONObject cartJsonObject = new JSONObject(cartJson);
                JSONArray products = cartJsonObject.getJSONArray("pl");
                clear();
                for (int i = 0; i < products.length() && i < MAXIMUM_PRODUCT_COUNT; i++) {
                    productList.add(Product.fromJson(products.getJSONObject(i)));
                }
                save();
            } catch (JSONException jse) {

            }
        }
    }

    /**
     * In addition to providing a human-readable JSON representation of the cart, the output
     * of this method can be stored and then later passed into {@link #loadFromString(String)} to
     * support multiple-cart use-cases.
     *
     * @return a JSON representation of the Cart
     * @see #loadFromString(String)
     */
    @Override
    public synchronized String toString() {
        JSONObject cartJsonObject = new JSONObject();
        JSONArray products = new JSONArray();
        if (productList.size() > 0) {
            for (int i = 0; i < productList.size(); i++) {
                products.put(productList.get(i).toJson());
            }
            try {
                cartJsonObject.put("pl", products);
            } catch (JSONException e) {

            }
        }
        return cartJsonObject.toString();
    }

    /**
     * Remove all Products from the Cart. This will not log an event.
     *
     * @return the Cart object for method chaining
     */
    public synchronized Cart clear() {
        productList.clear();
        save();
        return this;
    }

    private synchronized void save() {
        String serializedCart = toString();
        ConfigManager.getUserStorage(mContext, this.userId).setSerializedCart(serializedCart);
    }

    /**
     * Find a product by name
     *
     * @param name the product name
     * @return a Product object, or null if no matching Products were found.
     */
    public synchronized Product getProduct(String name) {
        for (int i = 0; i < productList.size(); i++) {
            if (productList.get(i).getName() != null && productList.get(i).getName().equalsIgnoreCase(name)) {
                return productList.get(i);
            }
        }
        return null;
    }

    /**
     * Retrieve the current list of Products in the Cart.
     * <p></p>
     * Note that this returns an {@code UnmodifiableCollection} that will throw an {@code UnsupportedOperationException}
     * if you attempt to add or remove Products.
     *
     * @return an {@code UnmodifiableCollection} of Products in the Cart
     */
    public List<Product> products() {
        return Collections.unmodifiableList(productList);
    }

    /**
     * Remove one or more products from the Cart and log a {@link CommerceEvent}.
     * <p></p>
     * This method will log a {@link CommerceEvent} with the {@link Product#REMOVE_FROM_CART} action.
     * <p></p>
     * If the Cart already contains a Product that is considered equal, the Product will be removed.
     *
     * @param product the product objects to remove from the Cart
     * @return the Cart object, useful for chaining several commands
     *
     */
    public synchronized Cart remove(Product product) {
        return remove(product, true);
    }

    /**
     * Remove one or more products from the Cart and log a {@link CommerceEvent}.
     * <p></p>
     * This method will log a {@link CommerceEvent} with the {@link Product#REMOVE_FROM_CART} action.
     * <p></p>
     * If the Cart already contains a Product that is considered equal, the Product will be removed.
     *
     * @param product the product to remove from the Cart
     * @return the Cart object, useful for chaining several commands
     *
     */
    public synchronized Cart remove(Product product, boolean logEvent) {
        if (product != null && productList.remove(product)) {
            save();
        }
        if (logEvent) {
            CommerceEvent event = new CommerceEvent.Builder(Product.REMOVE_FROM_CART, product).build();
            MParticle.getInstance().logEvent(event);
        }
        return this;
    }

    /**
     * Remove a product from the Cart by index and log a {@link CommerceEvent}.
     * <p></p>
     * This method will log a {@link CommerceEvent} with the {@link Product#REMOVE_FROM_CART} action.
     *
     * @param index of the Product to remove
     * @return boolean determining if a product was actually removed.
     * @see #products()
     */
    public synchronized boolean remove(int index) {
        boolean removed = false;
        if (index >= 0 && productList.size() > index) {
            Product product = productList.remove(index);
            save();
            CommerceEvent event = new CommerceEvent.Builder(Product.REMOVE_FROM_CART, product)
                    .build();
            MParticle.getInstance().logEvent(event);
        }
        return removed;
    }

    /**
     * Add one or more products to the Cart and optionally log a {@link CommerceEvent}.
     * <p></p>
     * This method will log a {@link CommerceEvent} with the {@link Product#ADD_TO_CART} action based on the logEvent parameter. Products added here
     * will remain in the cart across app restarts, and will be included in future calls to {@link Cart#purchase(TransactionAttributes)}
     * or {@link CommerceEvent}'s with a product action {@link Product#PURCHASE}
     * <p></p>
     *
     * @param newProducts the products to add to the Cart
     * @return the Cart object, useful for chaining several commands
     *
     */
    public synchronized Cart addAll(List<Product> newProducts, boolean logEvent) {
        if (newProducts != null && newProducts.size() > 0 && productList.size() < MAXIMUM_PRODUCT_COUNT) {
            for (Product product : newProducts) {
                if (product != null && !productList.contains(product)){
                    product.updateTimeAdded();
                    productList.add(product);
                    save();

                }
            }
            if (logEvent) {
                MParticle.getInstance().logEvent(
                        new CommerceEvent.Builder(Product.ADD_TO_CART, newProducts.get(0)).products(newProducts).build()
                );
            }
        }
        return this;
    }

    /**
     * Add one or more products to the Cart and log a {@link CommerceEvent}.
     * <p></p>
     * This method will log a {@link CommerceEvent} with the {@link Product#ADD_TO_CART} action. Products added here
     * will remain in the cart across app restarts, and will be included in future calls to {@link Cart#purchase(TransactionAttributes)}
     * or {@link CommerceEvent}'s with a product action {@link Product#PURCHASE}
     * <p></p>
     * If the Cart already contains a Product that is considered equal, this method is a no-op.
     *
     * @param product the product to add to the Cart
     * @return the Cart object, useful for chaining several commands
     *
     */
    public synchronized Cart add(Product product) {
        return add(product, true);
    }

    /**
     * Add one or more products to the Cart and log a {@link CommerceEvent}.
     * <p></p>
     * This method will log a {@link CommerceEvent} with the {@link Product#ADD_TO_CART} action. Products added here
     * will remain in the cart across app restarts, and will be included in future calls to {@link Cart#purchase(TransactionAttributes)}
     * or {@link CommerceEvent}'s with a product action {@link Product#PURCHASE}
     * <p></p>
     * If the Cart already contains a Product that is considered equal, this method is a no-op.
     *
     * @param product the product to add to the Cart
     * @return the Cart object, useful for chaining several commands
     *
     */
    public synchronized Cart add(Product product, boolean logEvent) {
        if (product != null && productList.size() < MAXIMUM_PRODUCT_COUNT && !productList.contains(product)) {
            product.updateTimeAdded();
            productList.add(product);
            save();
            if (logEvent) {
                MParticle.getInstance().logEvent(
                        new CommerceEvent.Builder(Product.ADD_TO_CART, product).build()
                );
            }
        }
        return this;
    }

    /**
     * Log a {@link CommerceEvent} with the {@link Product#CHECKOUT} action, including the Products that are
     * currently in the Cart.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     * @param step the checkout progress/step for apps that have a multi-step checkout process
     * @param options a label to associate with the checkout event
     */
    public synchronized void checkout(int step, String options) {
        if (productList != null && productList.size() > 0) {
            CommerceEvent event = new CommerceEvent.Builder(Product.CHECKOUT, productList.get(0))
                    .checkoutStep(step)
                    .checkoutOptions(options)
                    .products(productList)
                    .build();
            MParticle.getInstance().logEvent(event);
        } else {
            Logger.error("checkout() called but there are no Products in the Cart, no event was logged.");
        }
    }

    /**
     * Log a {@link CommerceEvent} with the {@link Product#CHECKOUT} action, including the Products that are
     * currently in the Cart.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     */
    public synchronized void checkout() {
        if (productList != null && productList.size() > 0) {
            CommerceEvent event = new CommerceEvent.Builder(Product.CHECKOUT, productList.get(0))
                    .products(productList)
                    .build();
            MParticle.getInstance().logEvent(event);
        }else {
            Logger.error("checkout() called but there are no Products in the Cart, no event was logged.");
        }
    }

    /**
     * Log a {@link CommerceEvent} with the {@link Product#PURCHASE} action for the Products that are
     * currently in the Cart.
     *
     * By default, this method will *not* clear the cart. You must manually call {@link Cart#clear()}.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     * @param attributes the attributes to associate with this purchase
     */
    public void purchase(TransactionAttributes attributes) {
        purchase(attributes, false);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link Product#PURCHASE} action for the Products that are
     * currently in the Cart.
     *
     * You should call {@link Cart#add(Product)} prior to this method.
     *
     * @param attributes the attributes to associate with this purchase
     * @param clearCart boolean determining if the cart should remove its contents after the purchase
     */
    public synchronized void purchase(TransactionAttributes attributes, boolean clearCart) {
        if (productList != null && productList.size() > 0) {
            CommerceEvent event = new CommerceEvent.Builder(Product.PURCHASE, productList.get(0))
                    .products(productList)
                    .transactionAttributes(attributes)
                    .build();
            if (clearCart) {
                clear();
            }
            MParticle.getInstance().logEvent(event);
        }else {
            Logger.error("purchase() called but there are no Products in the Cart, no event was logged.");
        }
    }

    /**
     * Log a {@link CommerceEvent} with the {@link Product#REFUND} action for the Products that are
     * currently in the Cart.
     *
     * @param attributes the attributes to associate with this refund. Typically at least the transaction ID is required.
     */
    public void refund(TransactionAttributes attributes, boolean clearCart) {
        if (productList != null && productList.size() > 0) {
            CommerceEvent event = new CommerceEvent.Builder(Product.REFUND, productList.get(0))
                    .products(productList)
                    .transactionAttributes(attributes)
                    .build();
            if (clearCart) {
                clear();
            }
            MParticle.getInstance().logEvent(event);
        } else {
            Logger.error("refund() called but there are no Products in the Cart, no event was logged.");
        }
    }
}