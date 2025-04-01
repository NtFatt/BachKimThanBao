pluginManagement {
    repositories {
        google {
            content {
                // Chỉ bao gồm các plugin của Android
                includeGroupByRegex("com\\.android.*")
                // Chỉ bao gồm các plugin của Google
                includeGroupByRegex("com\\.google.*")
                // Chỉ bao gồm các plugin của AndroidX
                includeGroupByRegex("androidx.*")
            }
        }
        // Đảm bảo có Maven Central để tìm các dependencies
        mavenCentral()
        // Đảm bảo có Gradle Plugin Portal để quản lý các plugin
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Cấu hình không cho phép khai báo repositories ở project-level
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Sử dụng Google repository cho các dependencies
        google()
        // Sử dụng Maven Central cho các dependencies
        mavenCentral()
    }
}

// Đặt tên cho root project
rootProject.name = "BachKimThanBao"

// Bao gồm module `app` trong dự án
include(":app")
