package com.example.doineedto.data

// Small, intentionally non-exhaustive set of common banking/wallet apps. Users can add
// their own bank's app via the "Excluded apps" settings section, which is the primary
// mechanism -- this list just covers a few widely used ones out of the box.
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
)
