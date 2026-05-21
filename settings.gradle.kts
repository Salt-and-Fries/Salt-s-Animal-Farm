pluginManagement {
    repositories {
        maven {
            name = "Prism"
            url = uri("https://maven.leclowndu93150.dev/releases")
        }
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForge"
            url = uri("https://maven.neoforged.net/releases")
        }
        maven {
            name = "Sponge"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("dev.prism.settings") version "0.5.12"
}

rootProject.name = "Salts Animal Farm"

prism {
    version("26.1.2") {
        common()
        fabric()
        neoforge()
    }
}
