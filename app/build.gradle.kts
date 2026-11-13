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

        // The Worker endpoint the app talks to. Release builds leave it empty on purpose: there is
        // no deployed endpoint yet, so the client takes the offline path instead of calling an
        // address that cannot exist on a user's phone.
        buildConfigField("String", "GENERATION_BASE_URL", "\"\"")
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
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.test.manifest)
}
