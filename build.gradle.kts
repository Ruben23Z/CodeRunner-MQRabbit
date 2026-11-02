subprojects {
    group = "isel.coderunner"
    version = "1.0"

    repositories { mavenCentral() }

    tasks.withType<JavaCompile>().configureEach {
        options.release = 21
        options.encoding = "UTF-8"
    }
}
