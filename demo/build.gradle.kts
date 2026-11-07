plugins {
    application
    id("com.gradleup.shadow") version "8.3.6"
}

dependencies {
    implementation(project(":contract"))
    implementation(project(":client"))
}

application { mainClass = "demo.DemoMain" }

tasks.shadowJar {
    archiveBaseName = "demo"
    archiveVersion = ""
    archiveClassifier = ""
    manifest { attributes["Main-Class"] = "demo.DemoMain" }
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}