plugins {
    id("java")
}

group = "com.github.tacomonkey11"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ta-api-core"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}