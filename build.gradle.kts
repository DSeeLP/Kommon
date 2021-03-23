val defaultGroupName="de.dseelp.kommon"
val defaultVersion="0.0.1"

group = defaultGroupName
version = defaultVersion

plugins {
    base
    `maven-publish`
    kotlin("jvm") version "1.4.31" apply false
    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
    kotlin("plugin.serialization") version "1.4.30" apply false
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
    }
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}
