plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "id.kalen3r"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.kalen3r"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    // Release signing is optional. Set these four values (Gradle property -P... or environment
    // variable) to get a signed release APK. If any is missing, assembleRelease still works and
    // produces an UNSIGNED apk. Never commit a keystore or these values.
    fun signingValue(name: String): String? =
        (providers.gradleProperty(name).orNull ?: providers.environmentVariable(name).orNull)
            ?.takeIf { it.isNotBlank() }

    val releaseKeystore = signingValue("KALEN3R_KEYSTORE")
    val releaseStorePassword = signingValue("KALEN3R_KEYSTORE_PASSWORD")
    val releaseKeyAlias = signingValue("KALEN3R_KEY_ALIAS")
    val releaseKeyPassword = signingValue("KALEN3R_KEY_PASSWORD") ?: releaseStorePassword
    val hasReleaseSigning =
        releaseKeystore != null && releaseStorePassword != null && releaseKeyAlias != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystore!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // R8/shrinking stays OFF for v1: the app looks up resources by name at runtime
            // (qris_donasi, tri_sandya) and was never tested with shrinking on a device.
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":calendar-core"))
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}
