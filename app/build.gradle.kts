plugins {
    id("com.android.application") version "8.7.3"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.mydoan.bachkimthanbao"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mydoan.bachkimthanbao"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
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

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    dependencies {
        // UI and system dependencies
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.activity:activity-ktx:1.10.1")
        implementation("androidx.constraintlayout:constraintlayout:2.2.1")

        // Testing dependencies
        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.2.1")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

        // CameraX
        implementation("androidx.camera:camera-core:1.4.1")
        implementation("androidx.camera:camera-camera2:1.4.1")
        implementation("androidx.camera:camera-lifecycle:1.4.1")
        implementation("androidx.camera:camera-view:1.4.1")

        // Android KTX
        implementation("androidx.core:core-ktx:1.15.0")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

        // Firebase dependencies (using BOM)
        implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
        implementation("com.google.firebase:firebase-analytics")
        implementation("com.google.firebase:firebase-auth") // Firebase Authentication
        implementation("com.google.firebase:firebase-firestore") // Firestore

        // Other dependencies
        implementation("com.github.bumptech.glide:glide:4.15.0")
        annotationProcessor("com.github.bumptech.glide:compiler:4.15.0")  // For Glide annotations, if needed
    }
}
