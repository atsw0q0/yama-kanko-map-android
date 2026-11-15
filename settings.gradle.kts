import java.util.Properties

pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

val localProperties = Properties().apply {
  val file = rootDir.resolve("local.properties")
  if (file.exists()) {
    file.inputStream().use { load(it) }
  }
}

val mapboxDownloadsToken: String = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN")
  ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")
  ?: ""

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
      credentials {
        username = "mapbox"
        password = mapboxDownloadsToken
      }
      authentication {
        create<BasicAuthentication>("basic")
      }
    }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "yama-kanko-map-android"

include(":app")
