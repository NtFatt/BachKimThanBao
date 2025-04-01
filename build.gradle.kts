plugins {
    id("com.android.application") version "8.7.3" apply false // Plugin của Android Application
    id("com.android.library") version "8.7.3" apply false // Plugin của Android Library
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // Plugin của Kotlin
    id("com.google.gms.google-services") version "4.4.2" apply false // Plugin Firebase
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
