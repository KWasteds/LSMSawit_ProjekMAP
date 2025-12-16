import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.lsmsawit_projekmap"
    compileSdk = 36

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.example.lsmsawit_projekmap"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Cloudinary
        buildConfigField(
            "String",
            "CLOUDINARY_CLOUD_NAME",
            "\"${localProperties.getProperty("cloudinary.cloud_name")}\""
        )
        buildConfigField(
            "String",
            "CLOUDINARY_UPLOAD_PRESET",
            "\"${localProperties.getProperty("cloudinary.upload_preset")}\""
        )

        // ✅ Tambahkan ini untuk Google Maps API Key dari local.properties
        val mapsApiKey = localProperties.getProperty("maps.api_key") ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Default libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Glide & CardView
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("androidx.cardview:cardview:1.0.0")

    implementation(platform(libs.firebase.bom))
    implementation("com.google.android.material:material:1.12.0")

    // Firebase
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation("com.google.firebase:firebase-storage-ktx")

    // GPS Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Activity, Fragment, Cloudinary
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("com.cloudinary:cloudinary-android:2.4.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    // Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Download to pdf
    implementation("com.itextpdf:itextg:5.5.10")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Room Kotlin Extensions (Coroutines)
    implementation("androidx.room:room-ktx:2.6.1")

    // Work Manager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // GSON Converter (untuk konversi JSON ke Kotlin Data Class)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coroutine (untuk ViewModel Scope)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Diagram ML
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
