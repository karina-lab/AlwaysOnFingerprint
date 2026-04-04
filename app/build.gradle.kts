plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.klab.alwaysonfps"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.klab.alwaysonfps"
        minSdk = 36
        targetSdk = 36

        versionCode = project.property("versionCode").toString().toInt()
        versionName = project.property("versionName").toString()
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
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

}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
}
