package com.mparticle;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

public interface UserAttributeListener {
    void onUserAttributesReceived(@Nullable Map<String, String> userAttributes, @Nullable Map<String, List<String>> userAttributeLists, @Nullable Long mpid);
}