plugins {
    application
    id("com.gradleup.shadow") version "8.3.6"
}

dependencies {
    implementation(project(":contract"))
    implementation("redis.clients:jedis:7.5.3")
    implementation("com.github.docker-java:docker-java-core:3.7.1")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.36")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.7.1")

}

application { mainClass = "runserver.RunServerMain" }

tasks.shadowJar {
    archiveBaseName = "runserver"
    archiveVersion = ""
    archiveClassifier = ""
    manifest { attributes["Main-Class"] = "runserver.RunServerMain" }
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.named<JavaExec>("run") { standardInput = System.`in` }
