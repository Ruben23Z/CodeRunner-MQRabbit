subprojects {
    group = "isel.coderunner"
    version = "1.0"

    repositories { mavenCentral() }

    tasks.withType<JavaCompile>().configureEach {
        options.release = 21
        options.encoding = "UTF-8"
    }

    plugins.withType<JavaPlugin> {
        dependencies {
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.4")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }
    }
}