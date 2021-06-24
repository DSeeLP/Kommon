val defaultGroupName = "io.github.dseelp.kommon"
val projectVersion: String by project

group = defaultGroupName
version = projectVersion

plugins {
    base
    java
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.4.32" apply false
    kotlin("jvm") version "1.5.20" apply false
    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
    kotlin("plugin.serialization") version "1.5.20" apply false
}

val isDeployingToCentral = System.getenv().containsKey("DEPLOY_CENTRAL")

if (isDeployingToCentral) println("Deploying to central...")
val excludedModules = arrayOf("console", "logging")

allprojects {
    group = defaultGroupName
    version = projectVersion

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }

    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")

    val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(dokkaHtml.outputDirectory)
    }

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    publishing {
        if (excludedModules.contains(this@allprojects.name)) return@publishing
        repositories {
            if (isDeployingToCentral) maven(url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            } else mavenLocal()
        }
        publications {
            register(this@allprojects.name, MavenPublication::class) {
                from(components["kotlin"])
                artifact(javadocJar.get())
                artifact(sourcesJar.get())

                pom {
                    url.set("https://github.com/DSeeLP/Kommon")
                    val prefix = "pom.${this@allprojects.name}"
                    val pomName = project.property("$prefix.name")
                    val pomDescription = project.property("$prefix.description")
                    name.set(pomName as String)
                    description.set(pomDescription as String)
                    developers {
                        developer {
                            name.set("DSeeLP")
                            organization.set("com.github.dseelp")
                            organizationUrl.set("https://www.github.com/DSeeLP")
                        }
                    }
                    licenses {
                        license {
                            name.set("MIT LICENSE")
                            url.set("https://www.opensource.org/licenses/mit-license.php")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/DSeeLP/Kommon.git")
                        developerConnection.set("scm:git:git://github.com/DSeeLP/Kommon.git")
                        url.set("https://github.com/DSeeLP/Kommon/")
                    }
                }
            }
        }
    }

    signing {
        if (!isDeployingToCentral) return@signing
        useInMemoryPgpKeys(
            //System.getenv("SIGNING_ID"),
            System.getenv("SIGNING_KEY"),
            System.getenv("SIGNING_PASSWORD")
        )
        publishing.publications.onEach {
            sign(it)
        }
    }


}
