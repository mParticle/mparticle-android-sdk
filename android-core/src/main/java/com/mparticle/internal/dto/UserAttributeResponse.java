package com.mparticle.internal.dto;

import java.util.List;
import java.util.Map;

public class UserAttributeResponse {
    public Map<String, String> attributeSingles;
    public Map<String, List<String>> attributeLists;
    public long time;
    public long mpId;
}
