plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    api(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    api(libs.okhttp)
    api(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

tasks.test {
    // Optional live/local end-to-end test against a running Nitaaq server:
    //   NITAAQ_E2E_BASE_URL=http://localhost:3000/ ./gradlew :core:test
    System.getenv("NITAAQ_E2E_BASE_URL")?.let { environment("NITAAQ_E2E_BASE_URL", it) }
    testLogging { events("passed", "failed", "skipped"); showStandardStreams = false }
}
