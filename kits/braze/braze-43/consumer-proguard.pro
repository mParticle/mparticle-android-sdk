# Braze ships its own consumer ProGuard/R8 rules via android-sdk-ui.
# Because this kit declares Braze as an `api` dependency, those rules are
# applied transitively to consuming apps — no kit-level Braze keeps needed.
