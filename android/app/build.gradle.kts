plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

fun env(name: String): String? = providers.environmentVariable(name).orNull

val apiBaseUrl: String? = providers.gradleProperty("laporlh.apiBaseUrl").orNull
val releaseKeystore: String? = env("LAPORLH_KEYSTORE_PATH")

android {
    namespace = "id.lapor.lh"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.lapor.lh"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseKeystore?.let(::file)
            storePassword = env("LAPORLH_KEYSTORE_PASSWORD")
            keyAlias = env("LAPORLH_KEY_ALIAS")
            keyPassword = env("LAPORLH_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl ?: "http://10.0.2.2:8080/api/v1/"}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl ?: "https://laporlh-api.onrender.com/api/v1/"}\"")
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ktlint {
    version.set("1.8.0")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.kotlinx.serialization)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
}
