pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "RaSaSeRMizac"
include(":app")
include(":engine-contracts")
project(":engine-contracts").projectDir = file("../../engine-contracts")
