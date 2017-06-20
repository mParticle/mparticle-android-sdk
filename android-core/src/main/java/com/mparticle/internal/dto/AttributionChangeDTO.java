package com.mparticle.internal.dto;

public class AttributionChangeDTO {
    private String key;
    private Object newValue;
    private Object oldValue;
    private boolean deleted;
    private boolean isNewAttribute;
    private long time;
    private long mpId;

    public AttributionChangeDTO(String key, Object newValue, Object oldValue, boolean deleted, boolean isNewAttribute, long time, long mpId) {
        this.key = key;
        this.newValue = newValue;
        this.oldValue = oldValue;
        this.deleted = deleted;
        this.isNewAttribute = isNewAttribute;
        this.time = time;
        this.mpId = mpId;
    }

    public String getKey() {
        return key;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isNewAttribute() {
        return isNewAttribute;
    }

    public long getTime() {
        return time;
    }

    public long getMpId() {
        return mpId;
    }
}
