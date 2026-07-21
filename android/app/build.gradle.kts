plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "io.agora.recipe.androidquickstart"
    compileSdk = 35
    defaultConfig {
        applicationId = "io.agora.recipe.androidquickstart"
        minSdk = 26
        targetSdk = 35
        versionCode = 1; versionName = "1.0"
        buildConfigField("String", "AGENT_BACKEND_URL", "\"http://10.0.2.2:8000\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation(libs.agora.rtc)
    implementation(libs.agora.rtm)
    implementation("io.agora.agents:agora-agent-client-toolkit:2.9.0")
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.okhttp.mockwebserver)
}
