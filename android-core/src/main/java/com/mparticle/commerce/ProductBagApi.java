package com.mparticle.commerce;

import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Entry point to create, read, update, and delete {@link ProductBag} objects. These objects are associated with a user
 * and can be used with mParticle's segmentation and targeting features.
 *
 * This class should not be directly instantiated, it can be accessed via <code>MParticle.getInstance().ProductBags()</code>.
 *
 */
public class ProductBagApi {
    private final Context mContext;
    private List<ProductBag> bags;

    private ProductBagApi() {
        mContext = null;
    }

    /**
     * This class should not be directly instantiated, it can be accessed via <code>MParticle.getInstance().ProductBags()</code>.
     *
     * @param context an context object used to persist ProductBags
     */
    public ProductBagApi(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Retrieve an <i>unmodifiable list</i> of Product Bag objects. Use this to iterate manually and inspect the current bags associated with the user.
     *
     * @return returns an <i>unmodifiable list</i> of Product Bags. This list will throw an {@code UnsupportedOperationException} if an attempt is made to modify it.
     */
    public List<ProductBag> getBags() {
        restoreBags();
        return Collections.unmodifiableList(bags);
    }

    /**
     * Add a new Product Bag or add a new Product to an existing bag.
     *
     * @param bagName the name of the bag to add or append to. Required.
     * @param product the name of the bag to add or append to. If the product is null, an empty bag will be created.
     *
     * @return returns true if the product was successfully added.
     */
    public boolean addProduct(String bagName, Product product) {
        if (MPUtility.isEmpty(bagName)) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Bag name must not be null or empty when calling ProductBags.addProduct()");
            return false;
        }
        if (product == null) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Null Product instance passed to ProductBags.addProduct(), creating empty bag.");
        }
        restoreBags();
        ProductBag bag = findBag(bagName);
        if (bag == null) {
            bag = new ProductBag(bagName);
            bags.add(bag);
        }
        if (product != null) {
            bag.products.add(product);
        }
        save();
        return true;
    }

    /**
     * Find a Product Bag by its name
     *
     * @param bagName the name of the Product Bag, case insensitive.
     * @return returns the Product Bag, or null if none were found.
     */
    public ProductBag findBag(String bagName) {
        if (MPUtility.isEmpty(bagName)) {
            return null;
        }
        for (int i = 0; i < bags.size(); i++) {
            if (bagName.equalsIgnoreCase(bags.get(i).bagName)) {
                return bags.get(i);
            }
        }
        return null;
    }

    /**
     * Remove a Product from an existing Product Bag
     *
     * By default, Product object equality is determined only by object reference. See {@link Product#setEqualityComparator(Product.EqualityComparator)} for how to change this behavior.
     *
     * @param bagName the name of the Product Bag from which to remove, this is case insensitive. Required.
     * @param product the product to remove from the Product Bag.
     *
     * @return returns true if the product was found and removed.
     */
    public boolean removeProduct(String bagName, Product product) {
        if (MPUtility.isEmpty(bagName)) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Bag name must not be null or empty when calling ProductBags.removeProduct()");
            return false;
        }
        if (product == null) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Null Product instance passed to ProductBags.removeProduct()");
            return false;
        }
        restoreBags();
        ProductBag bag = findBag(bagName);
        if (bag == null) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Could not find Product Bag: " + bagName + " while trying to remove Product.");
            return false;
        }
        if (!bag.products.remove(product)) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Failed to remove Product:\n" + product.toString() + "\n" + "from Product Bag: " + bagName + " - see Product.setEqualityComparator.");
            return false;
        } else {
            save();
            return true;
        }
    }

    /**
     * Remove all products from a Product Bag. Note that this will only empty the bag, rather than remove it.
     *
     * @param bagName the name of the bag to clear. Required.
     * @return returns true if the bag was found and cleared.
     */
    public boolean clearProductBag(String bagName) {
        if (MPUtility.isEmpty(bagName)) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Bag name should not be null or empty when calling ProductBags.clearProductBag()");
            return false;
        }
        restoreBags();
        ProductBag bag = findBag(bagName);
        if (bag != null) {
            bag.products.clear();
            save();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Completely remove a Product Bag.
     *
     * @param bagName the name of the bag to remove. Required.
     * @return returns true if the bag was found and removed.
     */
    public boolean removeProductBag(String bagName) {
        if (MPUtility.isEmpty(bagName)) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Bag name should not be null or empty when calling ProductBags.removeProductBag()");
            return false;
        }
        restoreBags();
        ProductBag bag = findBag(bagName);
        if (bag != null) {
            bags.remove(bag);
            save();
            return true;
        } else {
            return false;
        }
    }

    private void save() {
        if (bags != null) {
            mContext.getSharedPreferences(Constants.BAGS_FILE, Context.MODE_PRIVATE)
                    .edit()
                    .putString(Constants.PrefKeys.PRODUCT_BAGS, toString())
                    .apply();
        }
    }

    @Override
    public String toString() {
        restoreBags();
        JSONObject json = new JSONObject();
        if (bags != null) {
            for (int i = 0; i < bags.size(); i++) {
                try {
                    JSONObject object = new JSONObject();
                    json.put(bags.get(i).getName(), object);
                    JSONArray array = new JSONArray();
                    List<Product> products = bags.get(i).getProducts();
                    for (Product product : products) {
                        array.put(product.toJson());
                    }
                    object.put("pl", array);
                } catch (JSONException e) {

                }
            }
        }
        return json.toString();
    }

    private void restoreBags() {
        if (bags == null) {
            bags = new LinkedList<ProductBag>();
            SharedPreferences prefs = mContext.getSharedPreferences(Constants.BAGS_FILE, Context.MODE_PRIVATE);
            if (prefs.contains(Constants.PrefKeys.PRODUCT_BAGS)) {
                try {
                    JSONObject bagsJson = new JSONObject(prefs.getString(Constants.PrefKeys.PRODUCT_BAGS, null));
                    Iterator<String> keys = bagsJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        ProductBag bag = new ProductBag(key);
                        JSONArray productsJson = bagsJson.getJSONObject(key).optJSONArray("pl");
                        if (productsJson != null) {
                            for (int i = 0; i < productsJson.length(); i++) {
                                bag.products.add(Product.fromJson(productsJson.getJSONObject(i)));
                            }
                        }
                        bags.add(bag);
                    }
                } catch (Exception jse) {
                    prefs.edit().clear().apply();
                }
            }
        }
    }
}
