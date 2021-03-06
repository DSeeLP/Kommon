plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

val coroutinesVersion: String by project
val nettyVersion: String by project
val serializationVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("io.netty:netty-all:$nettyVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    //api(project(":event"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    //testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    //testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.4.31")
    //testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
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

tasks {
    build {
        dependsOn(shadowJar)
    }
}
