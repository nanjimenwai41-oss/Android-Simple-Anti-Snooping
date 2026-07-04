plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.antisnooping.hook"
    compileSdk = 37

    defaultConfig {
        // Use a neutral applicationId so the package name shown in system settings
        // does not contain offensive words.
        applicationId = "com.antisnooping.hook"
        minSdk = 29
        targetSdk = 37
        versionCode = 91
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    compileOnly(libs.xposed.api)
}
