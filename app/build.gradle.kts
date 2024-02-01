import org.jetbrains.kotlin.types.checker.TypeRefinementSupport.Disabled.isEnabled

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.bodygym"
    compileSdk = 34

    defaultConfig {
        applicationId = "my_app.bodygym"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.ads:mediation-test-suite:3.0.0")
    implementation("androidx.databinding:library:3.2.0-alpha11")
    implementation("androidx.media3:media3-ui:1.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.google.android.gms:play-services-ads:22.6.0")
    implementation (platform("com.google.firebase:firebase-bom:26.2.0"))
    implementation ("com.google.firebase:firebase-auth:22.3.0")
    implementation ("com.google.firebase:firebase-auth-ktx:22.3.0")
    implementation ("com.google.firebase:firebase-firestore:24.10.0")
    implementation ("androidx.recyclerview:recyclerview:1.1.0")
    implementation ("com.journeyapps:zxing-android-embedded:4.1.0")
    implementation ("com.google.firebase:firebase-database-ktx:20.3.0")
    implementation ("com.google.firebase:firebase-database:20.3.0")
    implementation ("com.google.firebase:firebase-storage-ktx:19.2.2")  // Firebase Storage 라이브러리 추가
    implementation ("com.google.firebase:firebase-storage:19.2.2")
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation ("com.google.android.gms:play-services-ads:21.0.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
}