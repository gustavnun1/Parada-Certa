// build.gradle.kts (Module: app)
// Adicione as novas dependências ao seu arquivo existente

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

fun loadDotEnv(): Map<String, String> {
    val env = linkedMapOf<String, String>()
    var dir: File? = projectDir
    while (dir != null) {
        val file = File(dir, ".env")
        if (file.isFile) {
            file.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                .forEach { line ->
                    val key = line.substringBefore("=").trim()
                    val value = line.substringAfter("=").trim().trim('"').trim('\'')
                    env[key] = value
                }
        }
        dir = dir.parentFile
    }
    return env
}

fun envValue(name: String, defaultValue: String = ""): String {
    val dotEnv = loadDotEnv()
    return System.getenv(name)
        ?: dotEnv[name]
        ?: defaultValue
}

fun String.gradleStringLiteral(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.example.paradacerta"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.paradacerta"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "API_BASE_URL",
            envValue("ANDROID_API_BASE_URL", "http://10.0.2.2:8080/").gradleStringLiteral()
        )
        buildConfigField(
            "String",
            "GOOGLE_MAPS_API_KEY",
            envValue("GOOGLE_MAPS_API_KEY").gradleStringLiteral()
        )
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = envValue("GOOGLE_MAPS_API_KEY")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Dependências existentes
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Retrofit (já existe no seu projeto)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Navigation Compose (já existe)
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ===== NOVAS DEPENDÊNCIAS PARA SQL SERVER =====

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // SQL Server JDBC Driver - IMPORTANTE!

    // OkHttp Logging (útil para debug)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil — carregamento assíncrono e cache de imagens (Compose)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Gson (para parsing JSON)
    implementation("com.google.code.gson:gson:2.10.1")

    // Security (para criptografia de senha - RECOMENDADO em produção)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // ML Kit Barcode Scanning (QR Code)
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation(libs.play.services.maps)
    implementation("com.google.android.libraries.places:places:3.3.0")

    //Tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    testImplementation("junit:junit:4.13.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
