plugins {
    kotlin("jvm") version "1.9.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("org.bitcoinj:bitcoinj-core:0.15.10")
    implementation("org.json:json:20230227")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("io.ktor:ktor-server-core:1.6.4")
    implementation("io.ktor:ktor-server-netty:1.6.4")
    implementation("io.ktor:ktor-serialization:1.6.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}


application {
    mainClass.set("com.example.MainKt")
}
