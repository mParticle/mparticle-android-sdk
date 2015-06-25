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

    static Cart getInstance(Context context) {
        mContext = context;
        return CartLoader.INSTANCE;
    }

    public static void setProductEqualityComparator(Product.EqualityComparator comparator) {
        Product.setEqualityComparator(comparator);
    }

    public synchronized Cart add(Product... products) {
        if (products != null) {
            for (Product product : products) {
                int index = productList.indexOf(product);
                if (index >= 0) {
                    Product currentProduct = productList.get(index);
                    Integer quantity = product.getQuantity();
                    if (quantity == null) {
                        quantity = 1;
                    }
                    Integer currentQuantity = currentProduct.getQuantity();
                    if (currentQuantity == null) {
                        currentQuantity = 1;
                    }
                    currentProduct.setQuantity(currentQuantity + quantity);
                } else {
                    productList.add(product);
                }
            }
            save();
            CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.ADD, products)
                    .build();
            MParticle.getInstance().logEvent(event);
        }
        return this;
    }

    public synchronized void remove(Product product) {
        if (product != null) {
            if (productList.contains(product)) {
                Product currentProduct = productList.get(productList.indexOf(product));
                int currentQuantity = currentProduct.getQuantity() == null ? 1 : currentProduct.getQuantity();
                int removeQuantity = product.getQuantity() == null ? 1 : product.getQuantity();
                int newQuantity = currentQuantity - removeQuantity;
                if (newQuantity < 0) {
                    newQuantity = 0;
                }
                if (newQuantity == 0) {
                    productList.remove(product);
                } else {
                    currentProduct.setQuantity(newQuantity);
                }
            }

            save();
            CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.REMOVE, product)
                    .build();
            MParticle.getInstance().logEvent(event);
        }
    }

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

    public synchronized void checkout(int step, String options) {
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, (Product[]) productList.toArray())
                .checkoutStep(step)
                .checkoutOptions(options)
                .build();
        MParticle.getInstance().logEvent(event);
    }

    public synchronized void checkout() {
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, (Product[]) productList.toArray())
                .build();
        MParticle.getInstance().logEvent(event);
    }

    public void purchase(TransactionAttributes attributes) {
        purchase(attributes, false);
    }

    public synchronized void purchase(TransactionAttributes attributes, boolean clearCart) {
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.PURCHASE, (Product[]) productList.toArray())
                .transactionAttributes(attributes)
                .build();
        if (clearCart) {
            productList.clear();
            save();
        }
        MParticle.getInstance().logEvent(event);
    }

    public void refund() {
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.REFUND, (Product[]) productList.toArray())
                .build();
        MParticle.getInstance().logEvent(event);
    }

    public void refund(TransactionAttributes attributes) {
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.REFUND, (Product[]) productList.toArray())
                .transactionAttributes(attributes)
                .build();
        MParticle.getInstance().logEvent(event);
    }

    public synchronized void loadFromString(String cartJson) {
        if (cartJson != null) {
            try {
                JSONObject cartJsonObject = new JSONObject(cartJson);
                JSONArray products = cartJsonObject.getJSONArray("pl");
                productList.clear();
                for (int i = 0; i < products.length(); i++) {
                    productList.add(Product.fromJson(products.getJSONObject(i)));
                }
                save();
            } catch (JSONException jse) {

            }
        }
    }

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

    public synchronized Cart clear() {
        productList.clear();
        save();
        return this;
    }

    private synchronized void save() {
        String serializedCart = toString();
        prefs.edit().putString(Constants.PrefKeys.CART, serializedCart).apply();
    }

    public synchronized int findProduct(String sku) {
        for (int i = 0; i < productList.size(); i++) {
            if (productList.get(i).getSku() != null && productList.get(i).getSku().equalsIgnoreCase(sku)) {
                return i;
            }
        }
        return -1;
    }

    public List<Product> products() {
        return Collections.unmodifiableList(productList);
    }

}
