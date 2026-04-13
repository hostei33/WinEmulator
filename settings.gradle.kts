pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
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

rootProject.name = "Win模拟器"
include(":app")
include(":tx11")
include(":shell-loader:stub")

// Termux 子模块
include(":termux-app")
project(":termux-app").projectDir = file("termux-app/app")
include(":termux-shared")
project(":termux-shared").projectDir = file("termux-app/termux-shared")
include(":terminal-emulator")
project(":terminal-emulator").projectDir = file("termux-app/terminal-emulator")
include(":terminal-view")
project(":terminal-view").projectDir = file("termux-app/terminal-view")