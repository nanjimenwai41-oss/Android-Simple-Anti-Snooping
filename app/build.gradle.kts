plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.antisnooping.hook"
    compileSdk = 34

    defaultConfig {
        // Use a neutral applicationId so the package name shown in system settings
        // does not contain offensive words.
        applicationId = "com.antisnooping.hook"
        minSdk = 29
        targetSdk = 34
        versionCode = 78
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    compileOnly("de.robv.android.xposed:api:82")
}
