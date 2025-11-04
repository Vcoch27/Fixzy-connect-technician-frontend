plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
//    id ("com.android.application")
    id ("com.google.gms.google-services")
    id ("kotlin-kapt")

}
android {
    namespace = "com.example.fixzy_ketnoikythuatvien"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.fixzy_ketnoikythuatvien"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            //wifi
            buildConfigField("String", "BASE_URL", "\"http://10.50.101.25:3000/\"")
            buildConfigField("String", "WEB_CLIENT_ID", "\"417602227592-dr9pfbsml00qndmie5rr42i9rchmr1km.apps.googleusercontent.com\"")

        }
        getByName("release") {
            buildConfigField("String", "BASE_URL", "\"http://10.50.101.25:3000/\"")
            buildConfigField("String", "WEB_CLIENT_ID", "\"417602227592-dr9pfbsml00qndmie5rr42i9rchmr1km.apps.googleusercontent.com\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

    }

    buildFeatures {
        buildConfig = true  // Đảm bảo tính năng BuildConfig được bật
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.constraintlayout.compose)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.volley)
    implementation(libs.play.services.cast.tv)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.exposed.core)
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.2.1")
    implementation("androidx.navigation:navigation-compose:2.7.3")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.compose.material:material:1.8.0-beta01")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation ("androidx.compose.foundation:foundation:1.7.8")
    implementation ("androidx.compose.ui:ui:1.7.8")
    implementation ("com.google.accompanist:accompanist-flowlayout:0.30.1")
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("androidx.compose.runtime:runtime-livedata:1.5.4")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation ("org.reduxkotlin:redux-kotlin-threadsafe:0.5.5")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation ("com.cloudinary:cloudinary-android:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation ("androidx.compose.runtime:runtime-livedata:1.7.4")
    implementation ("androidx.compose.runtime:runtime:1.5.0")
    implementation ("com.google.firebase:firebase-messaging:24.0.0")
    implementation("com.google.firebase:firebase-auth:23.2.0")
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
}
