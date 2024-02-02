pluginManagement {
    repositories {
        google()
        mavenCentral()
//        maven { url = uri("https://maven.aliyun.com/repository/central")}
//        maven { url = uri("https://maven.aliyun.com/repository/public")}
//        maven { url = uri("https://maven.aliyun.com/repository/google")}
//        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin")}
//        maven { url = uri("https://maven.aliyun.com/repository/jcenter")}
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
//        maven { url = uri("https://maven.aliyun.com/repository/central")}
//        maven { url = uri("https://maven.aliyun.com/repository/public")}
//        maven { url = uri("https://maven.aliyun.com/repository/google")}
//        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin")}
//        maven { url = uri("https://maven.aliyun.com/repository/jcenter")}
    }
}

rootProject.name = "AudioPlayer"
include(":app")
 