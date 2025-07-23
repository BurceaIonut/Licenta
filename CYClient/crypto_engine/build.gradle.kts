plugins {
    id("com.android.library")
}

android {
    namespace = "atm.licenta.crypto_engine"
    compileSdk = 34

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/java/atm/licenta/crypto_engine/cpp/CMakeLists.txt")
        }
    }

    sourceSets.getByName("main") {
        jniLibs.srcDirs("src/main/java/atm/licenta/crypto_engine/jniLibs")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("org.whispersystems:signal-protocol-java:2.8.1")
    implementation("com.google.code.gson:gson:2.8.6")
}