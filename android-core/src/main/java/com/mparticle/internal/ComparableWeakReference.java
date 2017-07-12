package com.mparticle.internal;

import java.lang.ref.WeakReference;

public class ComparableWeakReference<T> extends WeakReference<T> {

    public ComparableWeakReference(T referent) {
        super(referent);
    }

    @Override
    public int hashCode() {
        if (get() == null) {
            return super.hashCode();
        } else {
            return get().hashCode();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WeakReference) {
            try {
                WeakReference<T> ref = (WeakReference<T>) o;
                if (get() == null && ref.get() == null) {
                    return true;
                }
                if (get() == null || ref.get() == null) {
                    return false;
                }
                return get().equals(ref.get());
            } catch (ClassCastException ignored) {
                return false;
            }
        }
        return false;
    }

    public boolean isEqualTo(T t) {
        if (get() == null && t == null) {
            return true;
        }
        if (get() == null || t == null) {
            return false;
        }
        return get().equals(t);
    }
}