plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.kotlin.stdlib)
}
