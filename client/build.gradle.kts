plugins {
    application
    id("com.gradleup.shadow") version "8.3.6"
}

dependencies {
    implementation(project(":contract"))

}

application { mainClass = "client.ClientMain" }

tasks.shadowJar {
    archiveBaseName = "client"
    archiveVersion = ""
    archiveClassifier = ""
    manifest { attributes["Main-Class"] = "client.ClientMain" }
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.named<JavaExec>("run") { standardInput = System.`in` }
