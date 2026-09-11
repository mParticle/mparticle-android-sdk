# The Rokt kit and the Core SDK force-keep their own packages through consumer rules, so the
# measured numbers are the unshrinkable floor rather than a tree-shaken best case. The payment
# extension ships no consumer rules and its Stripe/Google Pay surface is only reached at runtime
# through the PaymentExtension interface, so without this rule R8 strips most of Stripe and the
# `plus` flavor under-reports by megabytes.
-keep class com.rokt.payment.extension.** { *; }
