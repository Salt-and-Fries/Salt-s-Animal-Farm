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

subprojects {
    if (path == ":1.20.1:forge") {
        tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
            doLast {
                val packMetadata = destinationDirectory.file("pack.mcmeta").get().asFile
                packMetadata.writeText(
                    """
                    {
                      "pack": {
                        "description": "Salt's Animal Farm resources",
                        "pack_format": 15
                      }
                    }
                    """.trimIndent()
                )
            }
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

    version("1.21.11") {
        common {
            dependencies {
                compileOnly("com.google.code.gson:gson:2.10.1")
            }
        }

        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.141.4+1.21.11")
            datagen()
        }

        neoforge {
            loaderVersion = "21.11.42"
            loaderVersionRange = "[4,)"
        }
    }

    version("1.21.1") {
        common {
            dependencies {
                compileOnly("com.google.code.gson:gson:2.10.1")
            }
        }

        fabric {
            loaderVersion = "0.16.10"
            fabricApi("0.116.1+1.21.1")
            datagen()
        }

        neoforge {
            loaderVersion = "21.1.222"
            loaderVersionRange = "[4,)"
        }
    }

    version("1.20.1") {
        common {
            dependencies {
                compileOnly("com.google.code.gson:gson:2.10.1")
            }
        }

        fabric {
            loaderVersion = "0.16.10"
            fabricApi("0.92.6+1.20.1")
            datagen()
        }

        forge {
            loaderVersion = "47.4.0"
            loaderVersionRange = "[47,)"
        }
    }
}
