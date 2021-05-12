val defaultGroupName = "io.github.dseelp"
val defaultVersion = "0.2"

group = defaultGroupName
version = defaultVersion

plugins {
    base
    java
    `maven-publish`
    signing
    kotlin("jvm") version "1.5.0" apply false
    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
    kotlin("plugin.serialization") version "1.5.0" apply false
}

allprojects {


    group = defaultGroupName

    version = defaultVersion

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }


}

subprojects {

    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "signing")

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }


    val excludedModules = arrayOf("console", "logging")


    val isDeployingToCentral = System.getProperty("DEPLOY_CENTRAL") == "yes"

    if (isDeployingToCentral) println("Deploying to central...")
    else println("DEBUG: Not deploying to central")

    publishing {
        if (excludedModules.contains(this@subprojects.name)) return@publishing
        repositories {
            mavenLocal()
            if (isDeployingToCentral) mavenCentral {
                credentials {
                    username = System.getProperty("MAVEN_USERNAME")
                    password = System.getProperty("MAVEN_PASSWORD")
                }
            }
        }
        publications {
            register(this@subprojects.name + "J", MavenPublication::class) {
                from(components["java"])
            }
            register(this@subprojects.name + "K", MavenPublication::class) {
                from(components["kotlin"])
                artifact(sourcesJar.get())
            }
        }
    }

    signing {
        if (!isDeployingToCentral) return@signing
        useInMemoryPgpKeys(
            System.getProperty("SIGNING_ID"),
            System.getProperty("SIGNING_KEY"),
            System.getProperty("SIGNING_PASSWORD")
        )
        publishing.publications.onEach {
            sign(it)
        }
    }
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}