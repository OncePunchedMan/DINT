plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "opb.myniceapp.dint"
    compileSdk = 34

    defaultConfig {
        applicationId = "opb.myniceapp.dint"
        minSdk = 29
        targetSdk = 34
        versionCode = providers.environmentVariable("DINT_VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.environmentVariable("DINT_VERSION_NAME").orNull?.takeIf { it.isNotBlank() } ?: "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    val ciKeystorePath = providers.environmentVariable("DINT_KEYSTORE_FILE").orNull?.takeIf { it.isNotBlank() }
    val ciKeystorePassword = providers.environmentVariable("DINT_KEYSTORE_PASSWORD").orNull?.takeIf { it.isNotBlank() }
    val ciKeyAlias = providers.environmentVariable("DINT_KEY_ALIAS").orNull?.takeIf { it.isNotBlank() }
    val ciKeyPassword = providers.environmentVariable("DINT_KEY_PASSWORD").orNull?.takeIf { it.isNotBlank() }

    if (
        ciKeystorePath != null &&
        ciKeystorePassword != null &&
        ciKeyAlias != null &&
        ciKeyPassword != null
    ) {
        signingConfigs.create("release") {
            storeFile = file(ciKeystorePath)
            storePassword = ciKeystorePassword
            keyAlias = ciKeyAlias
            keyPassword = ciKeyPassword
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(composeBom)

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
