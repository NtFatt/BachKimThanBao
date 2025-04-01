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

    // Make sure this is NOT commented out if you need BuildConfig
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Các thư viện cơ bản cho UI và các thành phần hệ thống
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Các thư viện kiểm thử
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // CameraX dependencies (sử dụng phiên bản mới nhất)
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // Các thư viện hỗ trợ Android KTX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // Firebase (sử dụng BOM để quản lý phiên bản)
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Các thư viện bổ sung (ví dụ: OpenCV nếu cần thiết)
    // implementation("org.opencv:opencv-android:4.5.1")
}
