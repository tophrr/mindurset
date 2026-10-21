plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    id("com.diffplug.spotless") version "8.10.4"
}

spotless {
    kotlin {
        target("app/src/**/*.kt")
        // Compose composables are PascalCase by convention; ktlint's rule assumes plain functions.
        ktlint("1.8.0").editorConfigOverride(
            mapOf("ktlint_standard_function-naming" to "disabled"),
        )
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktlint("1.8.0")
    }
    format("markdown") {
        target("*.md", "docs/**/*.md", ".github/**/*.md")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
