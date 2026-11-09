# Migration Guides

## Migrating from versions < 6.0.0

This guide covers the breaking changes introduced in mParticle Android SDK 6.0.0 and how to migrate your code to the new APIs.

---

### Removed `isAndroidIdDisabled()` / `androidIdDisabled(boolean)`

The `isAndroidIdDisabled()` method on `MParticle` and `MParticleOptions`, and the `androidIdDisabled(boolean)` builder method on `MParticleOptions.Builder`, have been removed. These were replaced by inverted equivalents with clearer semantics.

Replace `isAndroidIdDisabled()` with `isAndroidIdEnabled()` and invert the logic:

Java:

```java
// Before
if (MParticle.isAndroidIdDisabled()) { ... }
if (options.isAndroidIdDisabled()) { ... }

// After
if (!MParticle.isAndroidIdEnabled()) { ... }
if (!options.isAndroidIdEnabled()) { ... }
```

Kotlin:

```kotlin
// Before
if (MParticle.isAndroidIdDisabled()) { ... }
if (options.isAndroidIdDisabled()) { ... }

// After
if (!MParticle.isAndroidIdEnabled()) { ... }
if (!options.isAndroidIdEnabled()) { ... }
```

Replace `androidIdDisabled(boolean)` with `androidIdEnabled(boolean)` and invert the value:

Java:

```java
// Before
MParticleOptions.builder(context)
    .androidIdDisabled(true)
    .build();

// After
MParticleOptions.builder(context)
    .androidIdEnabled(false)
    .build();
```

Kotlin:

```kotlin
// Before
MParticleOptions.builder(context)
    .androidIdDisabled(true)
    .build()

// After
MParticleOptions.builder(context)
    .androidIdEnabled(false)
    .build()
```

---

### Removed `isAutoTrackingEnabled()`

The `isAutoTrackingEnabled()` method on `MParticle` has been removed. This method always returned `false` and automatic screen tracking via activity lifecycle hooks has been removed.

Remove any calls to `isAutoTrackingEnabled()`. If your code depends on this value, note that it was always `false` — no screen tracking was being performed automatically.

Java:

```java
// Before
if (MParticle.getInstance().isAutoTrackingEnabled()) {
    // this block was never executed
}

// After
// Remove the call entirely
```

Kotlin:

```kotlin
// Before
if (MParticle.getInstance()?.isAutoTrackingEnabled == true) {
    // this block was never executed
}

// After
// Remove the call entirely
```

---

### Removed `isProviderActive(int)`

The `isProviderActive(int)` method on `MParticle` has been removed. It has been renamed to `isKitActive(int)`.

Replace `isProviderActive(serviceProviderId)` with `isKitActive(serviceProviderId)`:

Java:

```java
// Before
boolean active = MParticle.getInstance().isProviderActive(MParticle.ServiceProviders.APPBOY);

// After
boolean active = MParticle.getInstance().isKitActive(MParticle.ServiceProviders.APPBOY);
```

Kotlin:

```kotlin
// Before
val active = MParticle.getInstance()?.isProviderActive(MParticle.ServiceProviders.APPBOY)

// After
val active = MParticle.getInstance()?.isKitActive(MParticle.ServiceProviders.APPBOY)
```

---

### Removed `MPEvent.setInfo()` / `getInfo()` / `Builder.info()`

The `setInfo(Map)`, `getInfo()`, and `Builder.info(Map)` methods on `MPEvent` have been removed. They have been renamed to `setCustomAttributes()`, `getCustomAttributes()`, and `Builder.customAttributes()`.

Replace usages with the new method names:

Java:

```java
// Before
MPEvent event = new MPEvent.Builder("Event Name", MParticle.EventType.Other)
    .info(attributes)
    .build();
Map<String, String> info = event.getInfo();
event.setInfo(attributes);

// After
MPEvent event = new MPEvent.Builder("Event Name", MParticle.EventType.Other)
    .customAttributes(attributes)
    .build();
Map<String, ?> customAttributes = event.getCustomAttributes();
event.setCustomAttributes(attributes);
```

Kotlin:

```kotlin
// Before
val event = MPEvent.Builder("Event Name", MParticle.EventType.Other)
    .info(attributes)
    .build()
val info = event.getInfo()
event.setInfo(attributes)

// After
val event = MPEvent.Builder("Event Name", MParticle.EventType.Other)
    .customAttributes(attributes)
    .build()
val customAttributes = event.getCustomAttributes()
event.setCustomAttributes(attributes)
```

---

### Removed `UserAttributeListener`

The `com.mparticle.UserAttributeListener` interface has been removed. It has been replaced by `com.mparticle.TypedUserAttributeListener`.

The key difference is that `onUserAttributesReceived` now receives `Map<String, Any?>` instead of `Map<String, String?>` for user attribute singles, allowing typed values (numbers, booleans) to be preserved rather than stringified. Additionally, the `mpid` parameter is non-nullable (`Long` instead of `Long?`).

Java:

```java
// Before
user.getUserAttributes(new UserAttributeListener() {
    @Override
    public void onUserAttributesReceived(
        @Nullable Map<String, String> userAttributes,
        @Nullable Map<String, List<String>> userAttributeLists,
        @Nullable Long mpid
    ) {
        // handle attributes
    }
});

// After
user.getUserAttributes(new TypedUserAttributeListener() {
    @Override
    public void onUserAttributesReceived(
        @NonNull Map<String, ?> userAttributes,
        @NonNull Map<String, ? extends List<String>> userAttributeLists,
        long mpid
    ) {
        // handle attributes — values may be String, Number, or null
    }
});
```

Kotlin:

```kotlin
// Before
user.getUserAttributes(UserAttributeListener { userAttributes, userAttributeLists, mpid ->
    // userAttributes: Map<String, String?>?
})

// After
user.getUserAttributes(object : TypedUserAttributeListener {
    override fun onUserAttributesReceived(
        userAttributes: Map<String, Any?>,
        userAttributeLists: Map<String, List<String?>?>,
        mpid: Long
    ) {
        // userAttributes values may be String, Number, Boolean, or null
    }
})
```

Note: numeric and boolean attribute values are no longer automatically converted to strings. If you need string values, call `.toString()` on each value manually.

---

### Removed `UserAliasHandler` / `IdentityApiRequest.Builder.userAliasHandler()`

The `com.mparticle.identity.UserAliasHandler` interface and the `IdentityApiRequest.Builder.userAliasHandler()` method have been removed. Use a success listener on the `BaseIdentityTask` returned by identity API calls instead.

Java:

```java
// Before
IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
    .email("user@example.com")
    .userAliasHandler(new UserAliasHandler() {
        @Override
        public void onUserAlias(MParticleUser previousUser, MParticleUser newUser) {
            // copy attributes from previousUser to newUser
        }
    })
    .build();
MParticle.getInstance().Identity().login(request);

// After
IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
    .email("user@example.com")
    .build();
MParticle.getInstance().Identity().login(request)
    .addSuccessListener(new TaskSuccessListener() {
        @Override
        public void onSuccess(IdentityApiResult result) {
            MParticleUser newUser = result.getUser();
            MParticleUser previousUser = result.getPreviousUser();
            if (previousUser != null) {
                // copy attributes from previousUser to newUser
            }
        }
    });
```

Kotlin:

```kotlin
// Before
val request = IdentityApiRequest.withEmptyUser()
    .email("user@example.com")
    .userAliasHandler { previousUser, newUser ->
        // copy attributes from previousUser to newUser
    }
    .build()
MParticle.getInstance()?.Identity()?.login(request)

// After
val request = IdentityApiRequest.withEmptyUser()
    .email("user@example.com")
    .build()
MParticle.getInstance()?.Identity()?.login(request)
    ?.addSuccessListener { result ->
        val newUser = result.user
        val previousUser = result.previousUser
        // copy attributes from previousUser to newUser
    }
```

---

### Removed `ConsentState.Builder.setCCPAConsent()` / `removeCCPAConsent()`

The `setCCPAConsent(CCPAConsent)` and `removeCCPAConsent()` methods on `ConsentState.Builder` have been removed. They have been renamed to `setCCPAConsentState(CCPAConsent)` and `removeCCPAConsentState()`.

Java:

```java
// Before
ConsentState state = ConsentState.builder()
    .setCCPAConsent(CCPAConsent.builder(true).build())
    .build();

ConsentState.builder().removeCCPAConsent();

// After
ConsentState state = ConsentState.builder()
    .setCCPAConsentState(CCPAConsent.builder(true).build())
    .build();

ConsentState.builder().removeCCPAConsentState();
```

Kotlin:

```kotlin
// Before
val state = ConsentState.builder()
    .setCCPAConsent(CCPAConsent.builder(true).build())
    .build()

ConsentState.builder().removeCCPAConsent()

// After
val state = ConsentState.builder()
    .setCCPAConsentState(CCPAConsent.builder(true).build())
    .build()

ConsentState.builder().removeCCPAConsentState()
```

---

### Rokt API moved from `android-core` to `rokt-kit`

Rokt APIs are now owned by the Rokt kit module, and `android-core` no longer has `MParticle.Rokt()`. From Kotlin, use the kit's `rokt` extension property. From Java, use `MParticleRokt.Rokt()`.

Kotlin:

```kotlin
// Before
import com.mparticle.Rokt

val rokt = MParticle.getInstance()?.Rokt()

// After
import com.mparticle.kits.rokt

val rokt = MParticle.getInstance()?.rokt
```

Java:

```java
// Before
com.mparticle.Rokt rokt = MParticle.getInstance().Rokt();

// After
com.mparticle.kits.Rokt rokt = com.mparticle.kits.MParticleRokt.Rokt();
```

---

### Rokt wrapper types replaced with native Rokt SDK types

The following mParticle wrapper types were removed:

- `com.mparticle.RoktEvent`
- `com.mparticle.MpRoktEventCallback`
- `com.mparticle.UnloadReasons`
- `com.mparticle.rokt.RoktConfig`
- `com.mparticle.rokt.PlacementOptions`

Use native Rokt SDK types instead:

- `com.rokt.roktsdk.RoktEvent`
- `com.rokt.roktsdk.RoktConfig`
- `com.rokt.roktsdk.PlacementOptions`

`MpRoktEventCallback` and `UnloadReasons` have no replacement type. Placement events now arrive as `com.rokt.roktsdk.RoktEvent` values. See [Placement callbacks replaced with `RoktEvent`](#placement-callbacks-replaced-with-roktevent).

Kotlin:

```kotlin
// Before
val config = com.mparticle.rokt.RoktConfig.Builder().build()

// After
val config = com.rokt.roktsdk.RoktConfig.Builder().build()
```

---

### Placement callbacks replaced with `RoktEvent`

`selectPlacements` no longer takes a `callbacks` argument, and `RoktLayout` no longer takes `mpRoktEventCallback`. Each callback is now a `RoktEvent`:

| Removed callback                 | `RoktEvent`                                                                                                       |
| -------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `onLoad()`                       | `PlacementReady`, then `PlacementInteractive` once the placement has rendered                                     |
| `onUnload(reason)`               | `PlacementClosed`, `PlacementCompleted` or `PlacementFailure`. The event type takes the place of `UnloadReasons`. |
| `onShouldShowLoadingIndicator()` | `ShowLoadingIndicator`                                                                                            |
| `onShouldHideLoadingIndicator()` | `HideLoadingIndicator`                                                                                            |

With `selectPlacements`, collect `events(identifier)` for the placement's identifier. Start collecting before you call `selectPlacements`, so you receive the first events.

Kotlin:

```kotlin
// Before
val callbacks = object : MpRoktEventCallback {
    override fun onLoad() { /* ... */ }
    override fun onUnload(reason: UnloadReasons) { /* ... */ }
    override fun onShouldShowLoadingIndicator() { /* ... */ }
    override fun onShouldHideLoadingIndicator() { /* ... */ }
}
MParticle.getInstance()?.Rokt()?.selectPlacements(
    identifier = "RoktExperience",
    attributes = attributes,
    callbacks = callbacks,
)

// After
import com.mparticle.kits.rokt
import com.rokt.roktsdk.RoktEvent

lifecycleScope.launch {
    MParticle.getInstance()?.rokt?.events("RoktExperience")?.collect { event ->
        when (event) {
            is RoktEvent.PlacementReady -> { /* was onLoad */ }
            is RoktEvent.PlacementClosed,
            is RoktEvent.PlacementCompleted,
            is RoktEvent.PlacementFailure -> { /* was onUnload */ }
            is RoktEvent.ShowLoadingIndicator -> { /* was onShouldShowLoadingIndicator */ }
            is RoktEvent.HideLoadingIndicator -> { /* was onShouldHideLoadingIndicator */ }
            else -> Unit
        }
    }
}
MParticle.getInstance()?.rokt?.selectPlacements(
    identifier = "RoktExperience",
    attributes = attributes,
)
```

With Jetpack Compose, pass `onEvent` to `RoktLayout`:

```kotlin
// Before
RoktLayout(
    sdkTriggered = true,
    identifier = "RoktExperience",
    attributes = attributes,
    location = "RoktEmbedded1",
    mpRoktEventCallback = callbacks,
)

// After
import com.mparticle.kits.RoktLayout
import com.rokt.roktsdk.RoktEvent

RoktLayout(
    sdkTriggered = true,
    identifier = "RoktExperience",
    attributes = attributes,
    location = "RoktEmbedded1",
    onEvent = { event: RoktEvent ->
        // Handle the event, as in the selectPlacements example above
    },
)
```
