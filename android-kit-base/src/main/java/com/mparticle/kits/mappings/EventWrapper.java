package com.mparticle.kits.mappings;

import com.mparticle.MPEvent;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.kits.CommerceEventUtils;
import com.mparticle.kits.KitUtils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Decorator classes for MPEvent and CommerceEvent. Used to extend functionality and to cache values for Projection processing.
 */
abstract class EventWrapper {
    public abstract Map<Integer, String> getAttributeHashes();

    protected Map<Integer, String> attributeHashes;

    public abstract int getEventTypeOrdinal();

    public abstract Object getEvent();

    public abstract int getMessageType();

    public abstract int getEventHash();

    protected static Map<Integer, String> getHashes(String hashPrefix, Map<String, String> map) {
        Map<Integer, String> hashedMap = new HashMap<Integer, String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            int hash = KitUtils.hashForFiltering(hashPrefix + entry.getKey());
            hashedMap.put(hash, entry.getKey());
        }
        return hashedMap;
    }

    public abstract Map.Entry<String, String> findAttribute(String propertyType, int hash, Product product, Promotion promotion);

    public abstract Map.Entry<String, String> findAttribute(String propertyType, String keyName, Product product, Promotion promotion);

    static class CommerceEventWrapper extends EventWrapper {
        private CommerceEvent mCommerceEvent;
        private Map<Integer, String> eventFieldHashes;
        private HashMap<String, String> eventFieldAttributes;

        public CommerceEventWrapper(CommerceEvent event) {
            this.mCommerceEvent = event;
        }

        @Override
        public Map<Integer, String> getAttributeHashes() {
            if (attributeHashes == null) {
                attributeHashes = new HashMap<Integer, String>();
                if (mCommerceEvent.getCustomAttributeStrings() != null) {
                    for (Map.Entry<String, String> entry : mCommerceEvent.getCustomAttributeStrings().entrySet()) {
                        int hash = KitUtils.hashForFiltering(getEventTypeOrdinal() + entry.getKey());
                        attributeHashes.put(hash, entry.getKey());
                    }
                }
            }
            return attributeHashes;
        }

        public int getEventTypeOrdinal() {
            return CommerceEventUtils.getEventType(mCommerceEvent);
        }

        public CommerceEvent getEvent() {
            return mCommerceEvent;
        }

        public void setEvent(CommerceEvent event) {
            mCommerceEvent = event;
        }

        public int getMessageType() {
            return 16;
        }

        public int getEventHash() {
            return KitUtils.hashForFiltering("" + getEventTypeOrdinal());
        }

        public Map.Entry<String, String> findAttribute(String propertyType, int hash, Product product, Promotion promotion) {
            if (CustomMapping.PROPERTY_LOCATION_EVENT_ATTRIBUTE.equalsIgnoreCase(propertyType)) {
                if (getEvent().getCustomAttributeStrings() == null || getEvent().getCustomAttributeStrings().size() == 0) {
                    return null;
                }
                String key = getAttributeHashes().get(hash);
                if (key != null) {
                    return new AbstractMap.SimpleEntry<String, String>(key, mCommerceEvent.getCustomAttributeStrings().get(key));
                }
            } else if (CustomMapping.PROPERTY_LOCATION_EVENT_FIELD.equalsIgnoreCase(propertyType)) {
                if (eventFieldHashes == null) {
                    if (eventFieldAttributes == null) {
                        eventFieldAttributes = new HashMap<String, String>();
                        CommerceEventUtils.extractActionAttributes(getEvent(), eventFieldAttributes);
                        CommerceEventUtils.extractTransactionAttributes(getEvent(), eventFieldAttributes);
                    }
                    eventFieldHashes = getHashes(getEventTypeOrdinal() + "", eventFieldAttributes);
                }
                String key = eventFieldHashes.get(hash);
                if (key != null) {
                    return new AbstractMap.SimpleEntry<String, String>(key, eventFieldAttributes.get(key));
                }
            } else if (CustomMapping.PROPERTY_LOCATION_PRODUCT_ATTRIBUTE.equalsIgnoreCase(propertyType)) {
                if (product == null || product.getCustomAttributes() == null || product.getCustomAttributes().size() == 0) {
                    return null;
                }
                Map<String, String> attributes = new HashMap<String, String>();
                CommerceEventUtils.extractProductAttributes(product, attributes);
                Map<Integer, String> hashes = getHashes(getEventTypeOrdinal() + "", attributes);
                String key = hashes.get(hash);
                if (key != null) {
                    return new AbstractMap.SimpleEntry<String, String>(key, attributes.get(key));
                }
            } else if (CustomMapping.PROPERTY_LOCATION_PRODUCT_FIELD.equalsIgnoreCase(propertyType)) {
                if (product == null) {
                    return null;
                }
                Map<String, String> attributes = new HashMap<String, String>();
                CommerceEventUtils.extractProductFields(product, attributes);
                Map<Integer, String> hashes = getHashes(getEventTypeOrdinal() + "", attributes);
                String key = hashes.get(hash);
                if (key != null) {
                    return new AbstractMap.SimpleEntry<String, String>(key, attributes.get(key));
                }
            } else if (CustomMapping.PROPERTY_LOCATION_PROMOTION_FIELD.equalsIgnoreCase(propertyType)) {
                if (promotion == null) {
                    return null;
                }
                Map<String, String> attributes = new HashMap<String, String>();
                CommerceEventUtils.extractPromotionAttributes(promotion, attributes);
                Map<Integer, String> hashes = getHashes(getEventTypeOrdinal() + "", attributes);
                String key = hashes.get(hash);
                if (key != null) {
                    return new AbstractMap.SimpleEntry<String, String>(key, attributes.get(key));
                }
            }
            return null;
        }

        @Override
        public Map.Entry<String, String> findAttribute(String propertyType, String keyName, Product product, Promotion promotion) {
            if (CustomMapping.PROPERTY_LOCATION_EVENT_ATTRIBUTE.equalsIgnoreCase(propertyType)) {
                if (getEvent().getCustomAttributeStrings() == null || getEvent().getCustomAttributeStrings().size() == 0) {
                    return null;
                }
                if (getEvent().getCustomAttributeStrings().containsKey(keyName)) {
                    return new AbstractMap.SimpleEntry<String, String>(keyName, getEvent().getCustomAttributeStrings().get(keyName));
                }
            } else if (CustomMapping.PROPERTY_LOCATION_EVENT_FIELD.equalsIgnoreCase(propertyType)) {
                if (eventFieldAttributes == null) {
                    eventFieldAttributes = new HashMap<String, String>();
                    CommerceEventUtils.extractActionAttributes(getEvent(), eventFieldAttributes);
                    CommerceEventUtils.extractTransactionAttributes(getEvent(), eventFieldAttributes);
                }
                if (eventFieldAttributes.containsKey(keyName)) {
                    return new AbstractMap.SimpleEntry<String, String>(keyName, eventFieldAttributes.get(keyName));
                }
            } else if (CustomMapping.PROPERTY_LOCATION_PRODUCT_ATTRIBUTE.equalsIgnoreCase(propertyType)) {
                if (product == null || product.getCustomAttributes() == null) {
                    return null;
                }
                Map<String, String> attributes = new HashMap<String, String>();
                CommerceEventUtils.extractProductAttributes(product, attributes);

                if (attributes.containsKey(keyName)) {
                    return new AbstractMap.SimpleEntry<String, String>(keyName, attributes.get(keyName));
                }
            } else if (CustomMapping.PROPERTY_LOCATION_PRODUCT_FIELD.equalsIgnoreCase(propertyType)) {
                if (product == null) {
                    return null;
                }
                Map<String, String> attributes = new HashMap<String, String>();
                CommerceEventUtils.extractProductFields(product, attributes);

                if (attributes.containsKey(keyName)) {
                    return new AbstractMap.SimpleEntry<String, String>(keyName, attributes.get(keyName));
                }
            } else if (CustomMapping.PROPERTY_LOCATION_PROMOTION_FIELD.equalsIgnoreCase(propertyType)) {
                if (promotion == null) {
                    return null;
                }
                Map<String, String> attributes = new HashMap<String, String>();
                CommerceEventUtils.extractPromotionAttributes(promotion, attributes);
                if (attributes.containsKey(keyName)) {
                    return new AbstractMap.SimpleEntry<String, String>(keyName, attributes.get(keyName));
                }
            }
            return null;
        }
    }

    static class MPEventWrapper extends EventWrapper {
        private final MPEvent mEvent;
        private boolean mScreenEvent;

        public MPEventWrapper(MPEvent event) {
            this(event, false);
        }

        public MPEventWrapper(MPEvent event, boolean isScreenEvent) {
            this.mEvent = event;
            this.mScreenEvent = isScreenEvent;
        }

        public Map<Integer, String> getAttributeHashes() {
            if (attributeHashes == null) {
                attributeHashes = new HashMap<Integer, String>();
                if (mEvent.getCustomAttributeStrings() != null) {
                    for (Map.Entry<String, String> entry : mEvent.getCustomAttributeStrings().entrySet()) {
                        int hash = KitUtils.hashForFiltering(getEventTypeOrdinal() + mEvent.getEventName() + entry.getKey());
                        attributeHashes.put(hash, entry.getKey());
                    }
                }

            }
            return attributeHashes;
        }

        public MPEvent getEvent() {
            return mEvent;
        }

        public int getEventTypeOrdinal() {
            if (mScreenEvent) {
                return 0;
            } else {
                return mEvent.getEventType().ordinal();
            }
        }

        public int getEventHash() {
            if (mScreenEvent) {
                return KitUtils.hashForFiltering(getEventTypeOrdinal() + mEvent.getEventName());
            } else {
                return mEvent.getEventHash();
            }
        }

        public int getMessageType() {
            if (mScreenEvent) {
                return 3;
            } else {
                return 4;
            }
        }

        public Map.Entry<String, String> findAttribute(String propertyType, String keyName, Product product, Promotion promotion) {
            if (CustomMapping.PROPERTY_LOCATION_EVENT_ATTRIBUTE.equalsIgnoreCase(propertyType)) {
                if (getEvent().getCustomAttributeStrings() == null) {
                    return null;
                }
                String value = getEvent().getCustomAttributeStrings().get(keyName);
                if (value != null) {
                    return new AbstractMap.SimpleEntry<String, String>(keyName, value);
                }
            }
            return null;
        }

        public Map.Entry<String, String> findAttribute(String propertyType, int hash, Product product, Promotion promotion) {
            if (CustomMapping.PROPERTY_LOCATION_EVENT_ATTRIBUTE.equalsIgnoreCase(propertyType)) {
                String key = getAttributeHashes().get(hash);
                if (key != null) {
                    return new AbstractMap.SimpleEntry<>(key, mEvent.getCustomAttributeStrings().get(key));
                }
            }
            return null;
        }

    }
}



