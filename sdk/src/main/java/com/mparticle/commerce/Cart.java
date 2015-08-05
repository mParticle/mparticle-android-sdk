package com.mparticle.commerce;


import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.MParticle;
import com.mparticle.internal.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * The Cart is a utility singleton that abstracts the creation of {@link com.mparticle.commerce.CommerceEvent} objects and maintains
 * a state, containing a list of {@link com.mparticle.commerce.Product} objects.
 * <p/>
 * The Cart will persist state across app-restarts.
 * <p/>
 * You may access the cart via the {@link MParticle#Commerce()} object:
 * <p/>
 * <pre>
 * {@code
 * MParticle.getInstance().Commerce().cart()}
 * </pre>
 * <p/>
 * You can also access the Cart directly:
 * <p/>
 * <pre>
 * {@code
 * Cart.getInstance(context)}
 * </pre>
 */
public final class Cart {

    private final List<Product> productList;
    private final SharedPreferences prefs;
    private static Context mContext;

    //lazy-loaded singleton via the initialization-on-demand holder pattern
    private static class CartLoader {
        private static Cart INSTANCE = new Cart(mContext);
    }

    private Cart(Context context) {
        prefs = context.getSharedPreferences(Constants.CART_PREFS_FILE, Context.MODE_PRIVATE);
        productList = new LinkedList<Product>();
        String cart = prefs.getString(Constants.PrefKeys.CART, null);
        loadFromString(cart);
    }

    /**
     * Retrieve a reference to the global cart instance. The first time that this is called in an app's lifecycle,
     * the Cart will access the filesystem to check for and load prior state.
     *
     * @param context a Context object used to access SharedPreferences
     * @return the global Cart instance
     */
    public static Cart getInstance(Context context) {
        if (context != null) {
            context = context.getApplicationContext();
        }
        mContext = context;
        return CartLoader.INSTANCE;
    }

    /**
     * Replace the default equality comparator.
     * <p/>
     * By default, the Cart will only compare Product objects by reference. If you would like to consider all Products that
     * share the same SKU, for example, as the same Product, use this function to set your own comparator.
     *
     * @param comparator
     * @see #add(Product)
     * @see #remove(int)
     */
    public static void setProductEqualityComparator(Product.EqualityComparator comparator) {
        Product.setEqualityComparator(comparator);
    }

    /**
     * Add one or more products to the Cart and log a {@link CommerceEvent}.
     * <p/>
     * This method will log a {@link CommerceEvent} with the {@link Product#ADD_TO_CART} action. Products added here
     * will remain in the cart across app restarts, and will be included in future calls to {@link CommerceApi#purchase(TransactionAttributes)}
     * or {@link CommerceEvent}'s with a product action {@link Product#PURCHASE}
     * <p/>
     * If the Cart already contains a Product that is considered equal, this method is a no-op.
     *
     * @param product the product to add to the Cart
     * @return the Cart object, useful for chaining several commands
     * @see #setProductEqualityComparator(Product.EqualityComparator)
     */
    public synchronized Cart add(Product product) {
        return add(product, true);
    }

    /**
     * Add one or more products to the Cart and log a {@link CommerceEvent}.
     * <p/>
     * This method will log a {@link CommerceEvent} with the {@link Product#ADD_TO_CART} action. Products added here
     * will remain in the cart across app restarts, and will be included in future calls to {@link CommerceApi#purchase(TransactionAttributes)}
     * or {@link CommerceEvent}'s with a product action {@link Product#PURCHASE}
     * <p/>
     * If the Cart already contains a Product that is considered equal, this method is a no-op.
     *
     * @param product the product to add to the Cart
     * @return the Cart object, useful for chaining several commands
     * @see #setProductEqualityComparator(Product.EqualityComparator)
     */
    public synchronized Cart add(Product product, boolean logEvent) {
        if (product != null && !productList.contains(product)) {
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
     * Remove one or more products from the Cart and log a {@link CommerceEvent}.
     * <p/>
     * This method will log a {@link CommerceEvent} with the {@link Product#REMOVE_FROM_CART} action.
     * <p/>
     * If the Cart already contains a Product that is considered equal, the Product will be removed. Otherwise, this method is a no-op.
     *
     * @param product the product objects to remove from the Cart
     * @return the Cart object, useful for chaining several commands
     * @see #setProductEqualityComparator(Product.EqualityComparator)
     */
    public synchronized Cart remove(Product product) {
        return remove(product, true);
    }

    /**
     * Remove one or more products from the Cart and log a {@link CommerceEvent}.
     * <p/>
     * This method will log a {@link CommerceEvent} with the {@link Product#REMOVE_FROM_CART} action.
     * <p/>
     * If the Cart already contains a Product that is considered equal, the Product will be removed. Otherwise, this method is a no-op..
     *
     * @param product the product to remove from the Cart
     * @return the Cart object, useful for chaining several commands
     * @see #setProductEqualityComparator(Product.EqualityComparator)
     */
    public synchronized Cart remove(Product product, boolean logEvent) {
        if (product != null && productList.remove(product)) {
            save();
            if (logEvent) {
                CommerceEvent event = new CommerceEvent.Builder(Product.REMOVE_FROM_CART, product).build();
                MParticle.getInstance().logEvent(event);
            }
        }
        return this;
    }

    /**
     * Remove a product from the Cart by index and log a {@link CommerceEvent}.
     * <p/>
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
     * Reset the cart and re-populate it from a String representation. This method allows you to support
     * logout/multiple-user or other multiple-cart use-cases.
     *
     * @param cartJson a JSON-encoded string acquired from {@link #toString()}
     */
    public synchronized void loadFromString(String cartJson) {
        if (cartJson != null) {
            try {
                JSONObject cartJsonObject = new JSONObject(cartJson);
                JSONArray products = cartJsonObject.getJSONArray("pl");
                clear();
                for (int i = 0; i < products.length(); i++) {
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
        prefs.edit().putString(Constants.PrefKeys.CART, serializedCart).apply();
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
     * <p/>
     * Note that this returns an {@code UnmodifiableCollection} that will throw an {@code UnsupportedOperationException}
     * if you attempt to add or remove Products.
     *
     * @return an {@code UnmodifiableCollection} of Products in the Cart
     */
    public List<Product> products() {
        return Collections.unmodifiableList(productList);
    }

}
