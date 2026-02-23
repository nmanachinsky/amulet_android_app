plugins {
    id("amulet.android.core")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.amulet.core.auth"
}

dependencies {
    api(project(":shared"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.supabase.auth)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
