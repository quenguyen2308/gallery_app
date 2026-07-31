import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android)
}

// Read GEMINI_API_KEY from local.properties (gitignored) so it never lands in source control.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY", "")
val geminiModel: String = localProperties.getProperty("GEMINI_MODEL", "gemini-2.5-flash-image")

// Release signing (keystore + credentials are gitignored, see local.properties). Falls back to
// unsigned if not configured, so a fresh checkout without local.properties still builds release.
val releaseStoreFile: String = localProperties.getProperty("RELEASE_STORE_FILE", "")
val releaseStorePassword: String = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
val releaseKeyAlias: String = localProperties.getProperty("RELEASE_KEY_ALIAS", "")
val releaseKeyPassword: String = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
val hasReleaseSigning = releaseStoreFile.isNotBlank() && rootProject.file(releaseStoreFile).exists()

android {
    namespace = "com.gallery"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gallery"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GEMINI_MODEL", "\"$geminiModel\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Coil3 pulls in Compose Multiplatform's "org.jetbrains.compose.*" artifacts transitively.
// On Android those duplicate the androidx.compose.* classes with mismatched internal metadata
// (e.g. ColumnScope.weight resolution breaks), so force pure AndroidX Compose everywhere.
configurations.all {
    exclude(group = "org.jetbrains.compose.foundation")
    exclude(group = "org.jetbrains.compose.material3")
    exclude(group = "org.jetbrains.compose.ui")
    exclude(group = "org.jetbrains.compose.runtime")
    exclude(group = "org.jetbrains.compose.animation")
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.network.okhttp)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.work.runtime.ktx)
    implementation(libs.biometric)
    implementation(libs.security.crypto)
    implementation(libs.exifinterface)
    implementation(libs.fragment.ktx)
    implementation(libs.material.components)
    implementation(libs.compose.cropper)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
