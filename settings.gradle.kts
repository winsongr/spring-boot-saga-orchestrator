rootProject.name = "spring-boot-saga-orchestrator"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

include(
    ":common",
    ":orchestrator",
    ":services:inventory",
    ":services:payment",
    ":services:shipping",
    ":services:notification",
    ":tests:orchestrator-it"
)
