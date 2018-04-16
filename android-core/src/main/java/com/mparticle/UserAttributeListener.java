package com.mparticle;

import java.util.List;
import java.util.Map;

public interface UserAttributeListener {
    void onUserAttributesReceived(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, Long mpid);
}