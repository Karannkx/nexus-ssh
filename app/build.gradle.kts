import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Download open-source variable fonts into generated Android resources. Keeping these out of
// source control avoids checking in binary font blobs while ensuring the APK bundles them offline.
val generatedDesignRes = layout.buildDirectory.dir("generated/res/designFonts")
val downloadDesignFonts by tasks.registering {
    val fontFiles = mapOf(
        "font/geist_sans.ttf" to "https://raw.githubusercontent.com/google/fonts/718e1db4deb9e4d9d85a0ead1b9f5fde2761ccfd/ofl/geist/Geist%5Bwght%5D.ttf",
        "font/geist_mono.ttf" to "https://raw.githubusercontent.com/google/fonts/718e1db4deb9e4d9d85a0ead1b9f5fde2761ccfd/ofl/geistmono/GeistMono%5Bwght%5D.ttf",
        "font/bricolage_grotesque.ttf" to "https://raw.githubusercontent.com/google/fonts/718e1db4deb9e4d9d85a0ead1b9f5fde2761ccfd/ofl/bricolagegrotesque/BricolageGrotesque%5Bopsz%2Cwdth%2Cwght%5D.ttf"
    )
    outputs.dir(generatedDesignRes)
    doLast {
        val root = generatedDesignRes.get().asFile
        fontFiles.forEach { (relative, source) ->
            val target = root.resolve(relative)
            if (!target.exists() || target.length() < 10_000L) {
                target.parentFile.mkdirs()
                val connection: HttpURLConnection = URL(source).openConnection() as HttpURLConnection
                connection.connectTimeout = 20_000
                connection.readTimeout = 60_000
                connection.inputStream.use { input: InputStream ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                connection.disconnect()
                check(target.length() > 10_000L) { "Downloaded font is unexpectedly small: $relative" }
                val signature = target.inputStream().use { input -> ByteArray(4).also { input.read(it) } }
                check(signature.contentEquals(byteArrayOf(0, 1, 0, 0)) || signature.contentEquals("OTTO".toByteArray())) {
                    "Downloaded asset is not a valid OpenType / TrueType font: $relative"
                }
            }
        }
    }
}

android {
    namespace = "com.nexus.ssh"
    sourceSets.getByName("main").res.srcDir(generatedDesignRes)
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexus.ssh"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.ui.text.ExperimentalTextApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/*.version",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/versions/**",
                "META-INF/OSGI-INF/**",
                "**/OSGI-INF/**"
            )
            pickFirsts += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/versions/11/OSGI-INF/MANIFEST.MF",
                "META-INF/versions/15/OSGI-INF/MANIFEST.MF",
                "META-INF/versions/21/OSGI-INF/MANIFEST.MF"
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.named("preBuild").configure { dependsOn(downloadDesignFonts) }

dependencies {
    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Jetpack Compose & Material 3
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended") // retained for legacy/specialized screen glyphs
    implementation("com.adamglin:phosphor-icon:1.0.0") // Phosphor Compose icon family
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Local Storage - Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Android Keystore & Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    // Biometric pulls in an old fragment whose FragmentActivity rejects ActivityResult request codes
    // ("Can only use lower 16 bits for requestCode") and crashes every file picker. Pin a modern one.
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // SSH Client - SSHJ (Supports Password, RSA, ED25519, SFTP, Port Forwarding)
    implementation("com.hierynomus:sshj:0.38.0")
    // Cryptography Providers (BouncyCastle for ED25519 & modern ciphers)
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")

    // Slf4j
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
