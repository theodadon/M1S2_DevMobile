pluginManagement {
    repositories {
        google()
        mavenCentral()   // indispensable pour JMRTD & Scuba
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "cnireader"
include(":app")
