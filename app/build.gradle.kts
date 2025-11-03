plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.capturametadatos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.capturametadatos"
        minSdk = 24 // Se necesita minSdk 24 para ExifInterface.setGpsInfo
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // RecyclerView para la galería
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Glide para cargar imágenes
    // ¡¡AQUÍ ESTÁ LA CORRECCIÓN!!
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Activity KTX (para registerForActivityResult)
    implementation("androidx.activity:activity-ktx:1.9.0")

    // Coroutines (para cargar galería en background)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Localización (Play Services)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ExifInterface (para leer y escribir metadatos)
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}

