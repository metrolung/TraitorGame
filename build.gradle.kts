plugins {
    kotlin("jvm") version "2.4.0"
    id("com.gradleup.shadow") version "9.4.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.xenondevs.xyz/releases")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("xyz.xenondevs.invui:invui:2.3.1")
    implementation("xyz.xenondevs.invui:invui-kotlin:2.3.1")
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
}

kotlin {
    jvmToolchain(25)
}

tasks {
    runServer {
        downloadPlugins {
            modrinth("PacketEvents", "2.13.0+spigot")
        }
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

kotlin {
    jvmToolchain(25)
}