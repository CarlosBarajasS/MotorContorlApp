plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

android {
    namespace   = "com.arranquesuave.motorcontrolapp"
    compileSdk  = 35

    defaultConfig {
        applicationId             = "com.arranquesuave.motorcontrolapp"
        minSdk                    = 21
        targetSdk                 = 35
        versionCode               = 1
        versionName               = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions("mode")
    productFlavors {
        create("demo") {
                applicationIdSuffix = ".demo"
            dimension = "mode"
            buildConfigField("boolean", "NO_AUTH", "true")
        }
        create("prod") {
            dimension = "mode"
            buildConfigField("boolean", "NO_AUTH", "false")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled  = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    // ✅ CONFIGURACIÓN PACKAGING - Resolver conflictos Netty/HiveMQ
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
            
            // ✅ EXCLUSIONES ESPECÍFICAS NETTY (HiveMQ)
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/native-image/**"
            excludes += "META-INF/services/**"
            excludes += "META-INF/maven/**"
            excludes += "META-INF/gradle/**"
            
            // ✅ EXCLUSIÓN AMPLIA PARA EVITAR FUTUROS CONFLICTOS
            pickFirsts += "**/META-INF/io.netty.versions.properties"
            pickFirsts += "**/META-INF/INDEX.LIST"
        }
    }
    
    // ✅ CONFIGURACIÓN LINT - Deshabilitar regla problemática
    lint {
        disable += "InvalidFragmentVersionForActivityResult"
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // ✅ FRAGMENT - Requerido para ActivityResult APIs
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // BOM primero, para que material3 tome la versión correcta
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.jetbrains.kotlinx.coroutines.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose.v253)

    // Plan B: Eclipse Paho con workaround (si HiveMQ falla)
    // implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    // implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    
    // MQTT Dependencies - HiveMQ (Moderno y compatible con AndroidX)
    implementation("com.hivemq:hivemq-mqtt-client:1.3.3")
    
    // ✅ WIFI CONFIGURATION DEPENDENCIES
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // ❌ Eclipse Paho MQTT (Deprecated - causa crashes)
    // implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    // implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    // implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    // implementation("androidx.legacy:legacy-support-v4:1.0.0")



    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
