import org.gradle.internal.os.OperatingSystem

val isWindows = OperatingSystem.current().isWindows
println("isWindows:$isWindows")
if (isWindows) {
    pluginManagement {
        repositories {
            maven {
                url = uri("https://maven.aliyun.com/repository/central")
            }
            maven {
                url = uri("https://maven.aliyun.com/repository/public/")
            }
            maven {
                url = uri("https://maven.aliyun.com/repository/gradle-plugin")
            }
            maven {
                url = uri("https://mirrors.cloud.tencent.com/repository/maven-public/")
            }

            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            maven {
                url = uri("https://www.jitpack.io")
            }
            maven {
                url = uri("https://maven.aliyun.com/repository/central")
            }
            maven {
                url = uri("https://maven.aliyun.com/repository/public/")
            }
            maven {
                url = uri("https://maven.aliyun.com/repository/gradle-plugin")
            }
            maven {
                url = uri("https://mirrors.cloud.tencent.com/repository/maven-public/")
            }
            google()
            mavenCentral()
        }
    }
} else {
    pluginManagement {
        repositories {
            google()
            mavenCentral()
            gradlePluginPortal()
            maven {
                url = uri("https://www.jitpack.io")
            }
        }
    }
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google()
            mavenCentral()
            maven {
                url = uri("https://www.jitpack.io")
            }
        }
    }
}
rootProject.name = "AndroidUVCCamera"
include(":app")
include(":libuvccamera")
