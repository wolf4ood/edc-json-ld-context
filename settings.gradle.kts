rootProject.name = "json-ld-context"



pluginManagement {
    repositories {
        val snapshotUrl = "https://oss.sonatype.org/content/repositories/snapshots/"

        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri(snapshotUrl)
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

include(":contexts:edc-context")