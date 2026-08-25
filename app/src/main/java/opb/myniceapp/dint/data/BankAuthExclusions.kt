package opb.myniceapp.dint.data

// Small, intentionally non-exhaustive set of apps where intervening is actively harmful
// (banking/wallet apps, push-approval authenticators). Users can add their own via the
// "Excluded apps" settings section, which is the primary mechanism -- this list just
// covers a few widely used ones out of the box.
val DEFAULT_EXCLUDED_PACKAGES: Set<String> = setOf(
    "com.samsung.android.spay", // Samsung Wallet
    "com.google.android.apps.walletnfcrel", // Google Wallet
    "com.paypal.android.p2pmobile", // PayPal
    "com.squareup.cash", // Cash App
    "com.venmo", // Venmo
    "com.chase.sig.android", // Chase
    "com.infonow.bofa", // Bank of America
    "com.wf.wellsfargomobile", // Wells Fargo
    "com.revolut.revolut", // Revolut
    "com.barclays.android.barclaysmobilebanking", // Barclays
    "de.number26.android", // N26
    "co.uk.getmondo", // Monzo
    // Push-approval authenticator apps -- intervening while the user is trying to approve
    // a sign-in (often triggered by tapping a lockscreen notification) is actively harmful.
    "com.azure.authenticator", // Microsoft Authenticator
    "com.google.android.apps.authenticator2", // Google Authenticator
    "com.duosecurity.duomobile", // Duo Mobile
    "com.okta.android.auth", // Okta Verify
    "com.authy.authy", // Authy
    "io.ente.auth", // Ente Authenticator
    "com.beemdevelopment.aegis", // Aegis Authenticator
)
