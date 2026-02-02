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

//        maven {
//            //允许使用不安全的maven，gradle7要求必须使用https，内网maven评估不使用https
//            isAllowInsecureProtocol = true
//            //need use oppo maven,due to google repo is not available in services
//            url = uri(providers.gradleProperty("prop_oppoMavenUrlRelease").get())
//
//            credentials {
//                username = providers.gradleProperty("sonatypeUsername").get()
//                password = providers.gradleProperty("sonatypePassword").get()
//            }
//        }
//        maven {
//            isAllowInsecureProtocol = true
//            url = uri("http://maven.scm.adc.com:8081/nexus/content/groups/snapshots/")
//            credentials {
//                username = "swdp"
//                password = "swdp"
//            }
//        }
//        maven {
//            isAllowInsecureProtocol = true
//            url = uri("http://maven.scm.adc.com:8081/nexus/content/repositories/releases/")
//            credentials {
//                username = "swdp"
//                password = "swdp"
//            }
//        }


    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {

        google()
        mavenCentral()


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
        
        // Mapbox Maven repository
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN").orNull 
                    ?: "YOUR_MAPBOX_SECRET_TOKEN"
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }

    }
}

rootProject.name = "ComopseDemoHub"
include(":app")
include(":fitdemo")
include(":rundemo")
