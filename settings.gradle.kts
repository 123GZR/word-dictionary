pluginManagement {
    repositories {
        // 阿里云镜像，解决 SSL 握手失败和下载超时问题
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/public/") }

        // 官方源作为备用（如果镜像没有找到依赖，会尝试回退到这里）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 只允许在 settings 中声明仓库，禁止在子项目的 build.gradle 中单独声明
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像，解决 SSL 握手失败和下载超时问题
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/public/") }

        // 官方源作为备用
        google()
        mavenCentral()
    }
}

rootProject.name = "WordDictionary"
include(":app")
