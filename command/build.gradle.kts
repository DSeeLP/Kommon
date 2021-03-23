plugins {
    `maven-publish`
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

val coroutinesVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
}

val implementationVersion = version

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Implementation-Version" to implementationVersion))
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        register("mavenKotlin", MavenPublication::class) {
            from(components["kotlin"])
            artifact(sourcesJar.get())
        }
    }
}
