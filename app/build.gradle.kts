plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.audioplayer"
    compileSdk = 36
    compileSdkMinor = 1
    ndkVersion = "29.0.14206865"
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.example.audioplayer"
        minSdk = 32
        versionCode = 20100
        versionName = "2.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}