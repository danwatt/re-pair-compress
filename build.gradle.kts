plugins {
    kotlin("jvm") version "1.9.22"
    id("org.springframework.boot") version "3.2.2" apply false
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.danwatt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    implementation("org.apache.commons:commons-compress:1.21")
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}