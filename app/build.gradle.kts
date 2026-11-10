plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)          // code generator used by Room
}

android {
    namespace = "uz.shushop.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "uz.shushop.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The shop server the app talks to (the same one that serves the website).
        // Change it here, in gradle.properties (shushopApiUrl=https://your-shop.onrender.com),
        // or without rebuilding: login screen -> "Server".
        val api = providers.gradleProperty("shushopApiUrl").getOrElse("https://shushop.onrender.com")
        buildConfigField("String", "API_BASE_URL", "\"${api.trimEnd('/')}\"")
    }

    buildTypes {
        release {
            // R8 optimises and shrinks the code: Compose runs MUCH smoother than in a debug build
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Lets you run the release build from Android Studio to test real speed.
            // "Generate Signed App Bundle / APK" still signs the final APK with YOUR keystore.
            signingConfig = signingConfigs.getByName("debug")
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room = SQLite database with Kotlin classes (an offline copy of the shop on the phone)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
