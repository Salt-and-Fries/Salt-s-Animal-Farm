plugins {
    id("dev.prism")
}

group = "org.betterLostItems"
version = providers.gradleProperty("mod_version").get()

allprojects {
    repositories {
        maven {
            name = "Terraformers"
            url = uri("https://maven.terraformersmc.com/")
        }
    }
}

prism {
    metadata {
        modId = "salts_animal_farm"
        name = "Salt's Animal Farm"
        description = "Idea was created by Reddit user u/Axoladdy. Adds richer farm animal behavior with comfort tasks, weather reactions, fear responses, weight-based loot, and debug tools."
        license = "MIT"
    }

    version("26.1.2") {
        common {
            dependencies {
                compileOnly("com.google.code.gson:gson:2.10.1")
            }
        }

        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.149.1+26.1.2")
            datagen()

            dependencies {
                modCompileOnly("com.terraformersmc:modmenu:18.0.0-beta.1")
                if (project.hasProperty("includeModMenu")) {
                    modRuntimeOnly("com.terraformersmc:modmenu:18.0.0-beta.1")
                }
            }
        }

        neoforge {
            loaderVersion = "26.1.2.61-beta"
            loaderVersionRange = "[4,)"
        }
    }
}
