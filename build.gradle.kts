plugins {
    kotlin("jvm") version "2.1.21"
    application
}

group = "br.com.bikeshop"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // driver JDBC do PostgreSQL
    implementation("org.postgresql:postgresql:42.7.7")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("br.com.bikeshop.MainKt")
}

tasks.named<JavaExec>("run") {
    // para o menu conseguir ler o teclado no "gradlew run"
    standardInput = System.`in`
}
