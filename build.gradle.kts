plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.kyssta"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    // Adventure API (bundled with Paper)
    compileOnly("net.kyori:adventure-api:4.18.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.18.0")

    // HikariCP for SQL
    implementation("com.zaxxer:HikariCP:6.2.1")

    // MySQL
    implementation("com.mysql:mysql-connector-j:9.2.0")

    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.5")

    // H2 embedded
    implementation("com.h2database:h2:2.3.232")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // GeoIP
    implementation("com.maxmind.geoip2:geoip2:4.2.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().resources.srcDirs) {
        include("plugin.yml")
        include("config.yml")
        include("messages.yml")
        include("templates.yml")
        include("menus/**")
        include("data/**")
    }
}

tasks.shadowJar {
    archiveFileName.set("CasualBans-${project.version}.jar")
    relocate("com.zaxxer.hikari", "com.kyssta.casualbans.libs.hikari")
    relocate("com.mysql", "com.kyssta.casualbans.libs.mysql")
    relocate("org.postgresql", "com.kyssta.casualbans.libs.postgresql")
    relocate("org.h2", "com.kyssta.casualbans.libs.h2")
    relocate("com.google.gson", "com.kyssta.casualbans.libs.gson")
    relocate("com.maxmind.geoip2", "com.kyssta.casualbans.libs.geoip2")
    minimize()
}
