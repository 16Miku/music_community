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

        // 将JitPack存储库添加到您的构建文件中,RecyclerViewAdapter的3.0.13 及以后版本不再需要
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "music_community"
include(":app")
