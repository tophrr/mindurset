import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.mindurset.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mindurset.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The Worker endpoint the app talks to. Release builds take the deployed Worker declared in
        // buildTypes.release below; an empty value still means "offline only".
        buildConfigField("String", "GENERATION_BASE_URL", "\"\"")
    }

    /**
     * The release keystore. Two sources, because the two places that build a release cannot share a
     * file: a local `keystore.properties` (gitignored, never committed) and, in CI, the same four
     * values passed as environment variables from repository secrets. With neither, the release build
     * stays unsigned instead of failing, so a contributor without the key can still compile one.
     */
    signingConfigs {
        create("release") {
            val local = rootProject.file("keystore.properties")
            if (local.exists()) {
                val props = Properties()
                local.inputStream().use { props.load(it) }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            } else {
                val path = System.getenv("MINDURSET_KEYSTORE_PATH")
                if (path != null) {
                    storeFile = file(path)
                    storePassword = System.getenv("MINDURSET_KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("MINDURSET_KEY_ALIAS")
                    keyPassword = System.getenv("MINDURSET_KEY_PASSWORD")
                }
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true

            // Loopback plus `adb reverse tcp:8787 tcp:8787` is the default. A phone on the same
            // network can point at the host instead:
            //   ./gradlew :app:assembleDebug -Pmindurset.endpoint=http://192.168.1.10:8787
            val endpoint =
                (project.findProperty("mindurset.endpoint") as String?)
                    ?: "http://127.0.0.1:8787"
            buildConfigField("String", "GENERATION_BASE_URL", "\"$endpoint\"")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // The deployed Worker, live since 26 November. Override for a staging name or a rebuild:
            //   ./gradlew :app:assembleRelease -Pmindurset.endpoint=https://<worker>.<subdomain>.workers.dev
            val endpoint =
                (project.findProperty("mindurset.endpoint") as String?)
                    ?: "https://mindurset-worker.chrisg661.workers.dev"
            buildConfigField("String", "GENERATION_BASE_URL", "\"$endpoint\"")

            // Only sign when a keystore was actually found; otherwise the build stays unsigned.
            signingConfigs.findByName("release")?.takeIf { it.storeFile != null }?.let {
                signingConfig = it
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets
        .getByName("test")
        .resources.directories
        .add(rootProject.file("contract").absolutePath)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.kotlinx.coroutines.test)
}
