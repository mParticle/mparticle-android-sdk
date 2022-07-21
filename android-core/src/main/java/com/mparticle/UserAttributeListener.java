package com.mparticle;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @deprecated Use TypedUserAttributeListener instead
 */
@Deprecated
public interface UserAttributeListener extends UserAttributeListenerType {
    void onUserAttributesReceived(@Nullable Map<String, String> userAttributes, @Nullable Map<String, List<String>> userAttributeLists, @Nullable Long mpid);
}
