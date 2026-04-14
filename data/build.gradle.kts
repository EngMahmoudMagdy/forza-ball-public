import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/**
 * Reads the **root** [local.properties] (standard Android gitignored file next to `settings.gradle.kts`).
 *
 * Define API credentials there, not in source control:
 * ```
 * API_FOOTBALL_KEY=...
 * API_FOOTBALL_BASE_URL=https://v3.football.api-sports.io/
 * ```
 */
private fun loadLocalProperties(): Properties {
    val props = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { props.load(it) }
    }
    return props
}

val generateObfuscatedSecrets = tasks.register("generateObfuscatedSecrets") {
    val outDir = layout.buildDirectory.dir("generated/secrets")
    outputs.dir(outDir)

    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        inputs.file(localPropsFile)
    }

    doLast {
        val props = loadLocalProperties()
        val apiKey = props.getProperty("API_FOOTBALL_KEY").orEmpty()
        val baseUrl = props.getProperty(
            "API_FOOTBALL_BASE_URL",
            "https://v3.football.api-sports.io/",
        )
        val mask = 0x5A
        fun obf(s: String): List<Int> =
            s.toByteArray(Charsets.UTF_8).map { (it.toInt() xor mask) and 0xff }
        val keyBytes = obf(apiKey)
        val urlBytes = obf(baseUrl)
        val dir = outDir.get().asFile
        dir.mkdirs()
        val header = buildString {
            appendLine("#ifndef FORZA_OBFUSCATED_SECRETS_H")
            appendLine("#define FORZA_OBFUSCATED_SECRETS_H")
            appendLine("#define FORZA_XOR_MASK 0x5A")
            appendLine("#define API_KEY_OBF_LEN ${keyBytes.size}")
            if (keyBytes.isEmpty()) {
                appendLine("static const unsigned char API_KEY_OBF[1] = { 0 };")
            } else {
                appendLine(
                    "static const unsigned char API_KEY_OBF[] = { ${
                        keyBytes.joinToString(", ") { "0x%02x".format(it) }
                    } };",
                )
            }
            appendLine("#define BASE_URL_OBF_LEN ${urlBytes.size}")
            appendLine(
                "static const unsigned char BASE_URL_OBF[] = { ${
                    urlBytes.joinToString(", ") { "0x%02x".format(it) }
                } };",
            )
            appendLine("#endif")
        }
        dir.resolve("obfuscated_secrets.h").writeText(header)
    }
}

android {
    namespace = "com.forzaball.data"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 27
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
        externalNativeBuild {
            cmake {
                val secretsDir =
                    project.layout.buildDirectory.get().asFile.resolve("generated/secrets")
                arguments += listOf("-DSECRETS_GEN_DIR=${secretsDir.absolutePath}")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    buildFeatures {
        buildConfig = true
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
}

tasks.named("preBuild").configure {
    dependsOn(generateObfuscatedSecrets)
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)

    // Coroutines & Flow
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

    // DI
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.no.op)

    // Paging
    implementation(libs.androidx.paging.runtime)

    // Room (with SQLCipher-ready configuration)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Timber for logging (data layer)
    implementation(libs.timber)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
}
