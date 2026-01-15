pluginManagement {
    repositories {
        maven {
            //允许使用不安全的maven，gradle7要求必须使用https，内网maven评估不使用https
            isAllowInsecureProtocol = true
            //need use oppo maven,due to google repo is not available in services
            url = uri(providers.gradleProperty("prop_oppoMavenUrlRelease").get())

            credentials {
                username = providers.gradleProperty("sonatypeUsername").get()
                password = providers.gradleProperty("sonatypePassword").get()
            }
        }
        maven {
            isAllowInsecureProtocol = true
            url = uri("http://maven.scm.adc.com:8081/nexus/content/groups/snapshots/")
            credentials {
                username = "swdp"
                password = "swdp"
            }
        }
        maven {
            isAllowInsecureProtocol = true
            url = uri("http://maven.scm.adc.com:8081/nexus/content/repositories/releases/")
            credentials {
                username = "swdp"
                password = "swdp"
            }
        }

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
        maven {
            //允许使用不安全的maven，gradle7要求必须使用https，内网maven评估不使用https
            isAllowInsecureProtocol = true
            //need use oppo maven,due to google repo is not available in services
            url = uri(providers.gradleProperty("prop_oppoMavenUrlRelease").get())

            credentials {
                username = providers.gradleProperty("sonatypeUsername").get()
                password = providers.gradleProperty("sonatypePassword").get()
            }
        }
        maven {
            isAllowInsecureProtocol = true
            url = uri("http://maven.scm.adc.com:8081/nexus/content/groups/snapshots/")
            credentials {
                username = "swdp"
                password = "swdp"
            }
        }
        maven {
            isAllowInsecureProtocol = true
            url = uri("http://maven.scm.adc.com:8081/nexus/content/repositories/releases/")
            credentials {
                username = "swdp"
                password = "swdp"
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "ComopseDemoHub"
include(":app")
include(":fitdemo")
