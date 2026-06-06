plugins {
    id("amulet.android.data")
}

android {
    namespace = "com.example.amulet.data.practices"
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:telemetry"))
    implementation(libs.kotlinx.datetime)

    // Только для тестов: проверка межмодульной целостности ссылок практик на паттерны.
    testImplementation(project(":data:patterns"))
}
