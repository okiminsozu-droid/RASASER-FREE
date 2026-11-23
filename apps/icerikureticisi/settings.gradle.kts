// Google Cloud's public Maven Central mirror is used at BUILD TIME only.
// This keeps GitHub Actions independent of shared-egress HTTP 429 limits.
// It is not a runtime network dependency and no user data is sent to it.
pluginManagement {
    repositories {
        google()
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}
rootProject.name = "RaSaSeRIcerikUreticisi"
include(":app")
