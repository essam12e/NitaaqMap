import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Backend settings come from gradle.properties (or -P / ORG_GRADLE_PROJECT_* in CI).
// Nothing secret lives here: TRIP_TOKEN_SECRET stays on the server only.
fun prop(name: String, default: String = "") = (project.findProperty(name) as String?) ?: default

// Optional release signing from android-app/keystore.properties (never committed).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.nitaaq.map"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nitaaq.map"
        minSdk = 29 // Android 10: MediaStore saving without any storage permission.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"${prop("nitaaq.apiBaseUrl", "https://nitaaq-map.vercel.app/")}\"")
        buildConfigField("String", "TRUSTED_TRIP_HOSTS", "\"${prop("nitaaq.trustedTripHosts", "nitaaq-map.vercel.app")}\"")
        buildConfigField("String", "QR_PUBLIC_ORIGIN", "\"${prop("nitaaq.qrPublicOrigin")}\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${prop("nitaaq.privacyPolicyUrl")}\"")
        buildConfigField("String", "WEBSITE_URL", "\"https://nitaaq-map.vercel.app/\"")
        buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Lets a developer point the debug build at `next dev` on the host:
            //   ./gradlew assembleDebug -Pnitaaq.debugApiBaseUrl=http://10.0.2.2:3000/ -Pnitaaq.debugTrustedTripHosts=10.0.2.2
            prop("nitaaq.debugApiBaseUrl").takeIf { it.isNotBlank() }?.let { url ->
                buildConfigField("String", "API_BASE_URL", "\"$url\"")
                buildConfigField("String", "TRUSTED_TRIP_HOSTS", "\"${prop("nitaaq.debugTrustedTripHosts", "10.0.2.2")}\"")
                buildConfigField("boolean", "ALLOW_CLEARTEXT", "${url.startsWith("http://")}")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // The UI is Arabic-only by design (see locales_config.xml).
        disable += setOf("MissingTranslation")
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
