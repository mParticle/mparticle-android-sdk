package com.mparticle;

public interface Configuration<T> {
    Class<T> configures();

    void apply(T t);
}
