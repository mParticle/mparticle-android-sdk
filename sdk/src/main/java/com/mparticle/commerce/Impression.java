package com.mparticle.commerce;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by sdozor on 8/5/15.
 */
public class Impression {
    private String mListName = null;
    private List<Product> mProducts;

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