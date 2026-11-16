plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.rasaser.free.navigasyon"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.rasaser.free.navigasyon"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.10.0-dev"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // PUBLIC repository contains no signing configuration.
        }
        // Installable, minimized FIELD TEST only. Debug key changes between
        // GitHub runners; NEVER use this variant for Google Play or updates.
        create("fieldTest") {
            initWith(getByName("release"))
            applicationIdSuffix = ".fieldtest10"
            versionNameSuffix = "-fieldtest"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("org.maplibre.gl:android-sdk-opengl:13.7.1")
    // MIT licensed, embedded Android Valhalla routing engine.
    implementation("io.github.rallista:valhalla-mobile:0.6.1")
    implementation("io.github.rallista:valhalla-models:0.5.2")
    implementation("io.github.rallista:valhalla-models-config:0.5.2")
    implementation("com.stadiamaps:osrm-openapi:0.0.10")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    testImplementation("junit:junit:4.13.2")
}
