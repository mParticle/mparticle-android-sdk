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
 *
 * The Cart will persist state across app-restarts.
 *
 * You may access the cart via the {@link MParticle#Commerce()} object:
 *
 * <pre>
 * {@code
 * MParticle.getInstance().Commerce().cart()}
 * </pre>
 *
 * You can also access the Cart directly:
 *
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
        if (context != null){
            context = context.getApplicationContext();
        }
        mContext = context;
        return CartLoader.INSTANCE;
    }

    /**
     * Replace the default equality comparator. The Cart compares products as they are added and removed. For example
     * in the case of adding a Product, if there's already a Product in the cart that is considered equal, the Cart
     * will adjust the quantity, rather than creating a new line-item. Similary, when removing from the Cart, the
     * equality comparator is used to ensure the correct Product is removed.
     *
     * By default, the Cart will only compare Product objects by reference. If you would like to consider all Products that
     * share the same SKU, for example, as the same Product, use this function to set your own comparator.
     *
     *
     * @param comparator
     */
    public static void setProductEqualityComparator(Product.EqualityComparator comparator) {
        Product.setEqualityComparator(comparator);
    }

    /**
     * Add one or more products to the Cart and log a {@link CommerceEvent}.
     *
     * This method will log a {@link CommerceEvent} with the {@link CommerceEvent#ADD} action. Products added here
     * will remain in the cart across app restarts, and will be included in future calls to {@link #purchase(TransactionAttributes)}
     *
     * You can use this method to adjust the quantity of a Product in the cart, by passing a Product that is already contained
     * in the Cart.
     *
     * @see #setProductEqualityComparator(Product.EqualityComparator)
     *
     * @param products the product objects to add to the Cart
     * @return the Cart object, useful for chaining several commands
     */
    public synchronized Cart add(Product... products) {
        if (products != null) {
            for (Product product : products) {
                if (product != null) {
                    int index = productList.indexOf(product);
                    if (index >= 0) {
                        Product currentProduct = productList.get(index);
                        double quantity = product.getQuantity();

                        double currentQuantity = currentProduct.getQuantity();

                        currentProduct.setQuantity(currentQuantity + quantity);
                    } else {
                        productList.add(product);
                    }
                }
            }
            save();
            CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.ADD, products)
                    .build();
            MParticle.getInstance().logEvent(event);
        }
        return this;
    }

    /**
     * Remove one or more products from the Cart and log a {@link CommerceEvent}.
     *
     * This method will log a {@link CommerceEvent} with the {@link CommerceEvent#REMOVE} action.
     *
     * You can use this method to adjust the quantity of a Product in the cart, by passing a Product that is already contained
     * in the Cart.
     *
     * @see #setProductEqualityComparator(Product.EqualityComparator)
     *
     * @param products the product objects to remove from the Cart
     * @return the Cart object, useful for chaining several commands
     */
    public synchronized void remove(Product... products) {
        if (products != null) {
            for (Product product : products) {
                if (product != null) {
                    if (productList.contains(product)) {
                        Product currentProduct = productList.get(productList.indexOf(product));
                        double currentQuantity = currentProduct.getQuantity();
                        double removeQuantity = product.getQuantity();
                        double newQuantity = currentQuantity - removeQuantity;
                        if (newQuantity < 0) {
                            newQuantity = 0;
                        }
                        if (newQuantity == 0) {
                            productList.remove(product);
                        } else {
                            currentProduct.setQuantity(newQuantity);
                        }
                    }
                }
            }

            save();
            CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.REMOVE, products)
                    .build();
            MParticle.getInstance().logEvent(event);
        }
    }

    /**
     * Remove a product from the Cart by index and log a {@link CommerceEvent}.
     *
     * This method will log a {@link CommerceEvent} with the {@link CommerceEvent#REMOVE} action.
     *
     *
     * @param index of the Product to remove
     * @return boolean determining if a product was actually removed.
     *
     * @see #products()
     */
    public synchronized boolean remove(int index) {
        boolean removed = false;
        if (index >= 0 && productList.size() > index) {
            Product product = productList.remove(index);
            save();
            CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.REMOVE, product)
                    .build();
            MParticle.getInstance().logEvent(event);
        }
        return removed;
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#CHECKOUT} action, including the Products that are
     * currently in the Cart.
     *
     * You should call {@link #add(Product...)} prior to this method.
     *
     * @param step the checkout progress/step for apps that have a multi-step checkout process
     * @param options a label to associate with the checkout event
     */
    public synchronized void checkout(int step, String options) {
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, productList.toArray(new Product[productList.size()]))
                .checkoutStep(step)
                .checkoutOptions(options)
                .build();
        MParticle.getInstance().logEvent(event);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#CHECKOUT} action, including the Products that are
     * currently in the Cart.
     *
     * You should call {@link #add(Product...)} prior to this method.
     *
     */
    public synchronized void checkout() {
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, productList.toArray(new Product[productList.size()]))
                .build();
        MParticle.getInstance().logEvent(event);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#PURCHASE} action for the Products that are
     * currently in the Cart.
     *
     * By default, this method will *not* clear the cart. You must manually call {@link #clear()}.
     *
     * You should call {@link #add(Product...)} prior to this method.
     *
     * @param attributes the attributes to associate with this purchase
     */
    public void purchase(TransactionAttributes attributes) {
        purchase(attributes, false);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#PURCHASE} action for the Products that are
     * currently in the Cart.
     *
     * You should call {@link #add(Product...)} prior to this method.
     *
     * @param attributes the attributes to associate with this purchase
     * @param clearCart boolean determining if the cart should remove its contents after the purchase
     */
    public synchronized void purchase(TransactionAttributes attributes, boolean clearCart) {
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.PURCHASE, productList.toArray(new Product[productList.size()]))
                .transactionAttributes(attributes)
                .build();
        if (clearCart) {
            productList.clear();
            save();
        }
        MParticle.getInstance().logEvent(event);
    }

    /**
     * Log a {@link CommerceEvent} with the {@link CommerceEvent#REFUND} action for the Products that are
     * currently in the Cart.
     *
     * @param attributes the attributes to associate with this refund. Typically at least the transaction ID is required.
     */
    public void refund(TransactionAttributes attributes) {
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.REFUND, productList.toArray(new Product[productList.size()]))
                .transactionAttributes(attributes)
                .build();
        MParticle.getInstance().logEvent(event);
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
     *
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
     *
     * Note that this returns an {@code UnmodifiableCollection} that will throw an {@code UnsupportedOperationException}
     * if you attempt to add or remove Products.
     *
     * @return an {@code UnmodifiableCollection} of Products in the Cart
     */
    public List<Product> products() {
        return Collections.unmodifiableList(productList);
    }

}
