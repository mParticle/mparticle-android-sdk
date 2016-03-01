package com.mparticle.commerce;

import java.util.LinkedList;
import java.util.List;

/**
 * Class representing an impression of one of more {@link Product} objects
 *
 */
public class Impression {
    private String mListName = null;
    private List<Product> mProducts;

    /**
     * Create an Impression object.
     *
     * @param listName a string name given to the list where the given Products displayed
     * @param product a Product to associate with the Impression
     */
    public Impression(String listName, Product product) {
        super();
        mListName = listName;
        addProduct(product);
    }

    public String getListName() {
        return mListName;
    }

    public List<Product> getProducts() {
        return mProducts;
    }

    /**
     * Add a Product to this Impression
     *
     * @param product
     * @return
     */
    public Impression addProduct(Product product) {
        if (mProducts == null) {
            mProducts = new LinkedList<Product>();
        }
        if (product != null) {
            mProducts.add(product);
        }
        return this;
    }

    public Impression(Impression impression) {
        mListName = impression.mListName;
        if (impression.mProducts != null) {
            mProducts = impression.mProducts;
        }
    }
}