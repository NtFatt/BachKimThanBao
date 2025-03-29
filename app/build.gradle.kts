plugins {
    id("com.android.application") version "8.7.3" // Hoặc phiên bản mới hơn
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
}

dependencies {
    // Các dependencies cơ bản
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // CameraX dependencies - Sử dụng phiên bản mới nhất (1.4.1)
    implementation("androidx.camera:camera-core:1.4.1") // CameraX Core
    implementation("androidx.camera:camera-camera2:1.4.1") // Camera2 API
    implementation("androidx.camera:camera-lifecycle:1.4.1") // Lifecycle của camera
    implementation("androidx.camera:camera-view:1.4.1") // Camera View (nếu cần)

    // Các thư viện hỗ trợ từ Android KTX
    implementation("androidx.core:core-ktx:1.15.0")

    // Các thư viện khác nếu cần (ví dụ: Permissions)
    implementation("androidx.activity:activity-ktx:1.10.1")

    // Thêm các thư viện hỗ trợ khác nếu cần
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
/*
    implementation("org.opencv:opencv-android:4.5.1")
*/

}
