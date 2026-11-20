plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}
android {
  namespace = "com.rasaser.free.rasaserwidgets"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.rasaser.free.rasaserwidgets"
    minSdk = 26
    targetSdk = 36
    versionCode = 11
    versionName = "0.4.2"
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
}
