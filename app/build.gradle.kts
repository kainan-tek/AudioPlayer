plugins {
    id("com.android.application")
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
        versionCode = 20001
        versionName = "2.0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}