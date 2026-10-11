pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "additive-game"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:roll")
include(":core:database")
include(":core:data")
include(":core:ui")
include(":widget")
include(":notification")
include(":feature:expedition")
include(":feature:collection")
