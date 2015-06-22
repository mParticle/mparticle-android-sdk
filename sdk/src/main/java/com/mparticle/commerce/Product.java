package com.mparticle.commerce;

import java.lang.Object;import java.lang.String;import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;


public final class Product extends HashMap<String, Object> {


    private Product(){}

    public Product(Builder builder) {

    }

    public static class Builder {
        public Builder(){}
        public Builder(String name){

        }

        public Builder(Product product){

        }

        public Builder setCategory(String category) {
            //  this.mName = name;
            return this;
        }

        public Builder setCouponCode(String couponCode) {
            //  this.mName = name;
            return this;
        }

        public Builder setId(String id) {
            //  this.mName = name;
            return this;
        }

        public Builder setName(String name) {
            return this;
        }

        public Builder setPosition(int position) {
            // this.mName = name;
            return this;
        }

        public Builder setPrice(BigDecimal price) {
            // this.mName = name;
            return this;
        }

        public Builder setQuantity(int quantity) {
            //   this.mName = name;
            return this;
        }

        public Builder setBrand(String name) {
            return this;
        }

        public Builder setVariant(String variant){
            return this;
        }

        public Product build() {
            return new Product(this);
        }
    }
}
