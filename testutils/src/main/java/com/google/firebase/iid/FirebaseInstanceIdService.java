package com.google.firebase.iid;

import com.mparticle.testutils.TestingUtils;

public class FirebaseInstanceIdService {

    static {
        if (!TestingUtils.isFirebasePresent()) {
            throw new RuntimeException(new ClassCastException());
        }
    }
}
