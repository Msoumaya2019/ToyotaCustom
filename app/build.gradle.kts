plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("org.jetbrains.kotlin.kapt") }
android {
 namespace = "org.toyotacustom.app"
 compileSdk = 36
 defaultConfig { applicationId = "org.toyotacustom.app"; minSdk = 29; targetSdk = 36; versionCode = 5; versionName = "0.5.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
 implementation(project(":core"))
 implementation(platform("androidx.compose:compose-bom:2026.05.01"))
 implementation("androidx.activity:activity-compose:1.12.4")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("androidx.room:room-runtime:2.8.5")
 implementation("androidx.room:room-ktx:2.8.5")
 kapt("androidx.room:room-compiler:2.8.5")
 // Room's transitive metadata reader predates Kotlin 2.4; keep the reader aligned.
 kapt("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation(platform("androidx.compose:compose-bom:2026.05.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
