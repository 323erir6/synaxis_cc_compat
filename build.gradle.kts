plugins {
    java
    id("net.neoforged.moddev") version "2.0.80"
}

version = property("mod_version")!!
group = "dev.synaxiscccompat"

base {
    archivesName.set("synaxis-cc-compat")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
}

neoForge {
    version = property("neo_version") as String
    mods {
        create("synaxis_cc_compat") {
            sourceSet(sourceSets.main.get())
        }
    }
}

val instanceMods = file(
    providers.gradleProperty("instance_mods_dir").orElse(
        providers.environmentVariable("APPDATA").map {
            "$it/ElyPrismLauncher/instances/1.0.0/minecraft/mods"
        }
    ).get()
)
val synaxisVersion = property("synaxis_version") as String
val ccVersion = property("cc_version") as String

dependencies {
    compileOnly(files(instanceMods.resolve("synaxis-${synaxisVersion}.jar")))
    compileOnly(files(instanceMods.resolve("cc-tweaked-1.21.1-forge-${ccVersion}.jar")))
    compileOnly(files(instanceMods.resolve("create-1.21.1-6.0.10.jar")))
    compileOnly(files("libs/META-INF/jarjar/ponder-neoforge-1.0.82+mc1.21.1.jar"))
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("synaxis_version", synaxisVersion)
    inputs.property("cc_version", ccVersion)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            "version" to project.version,
            "synaxis_version" to synaxisVersion,
            "cc_version" to ccVersion
        )
    }
}
