plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.hyeprsoft.picker"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}


publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.hypersoftdev"
            artifactId = "ColorPicker"
            version = "1.0.4"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}