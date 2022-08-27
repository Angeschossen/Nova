group = "xyz.xenondevs.nova"

val mojangMapped = System.getProperty("mojang-mapped") != null

plugins {
    java
    kotlin("jvm") version "1.7.10"
    id("xyz.xenondevs.specialsource-gradle-plugin") version "1.0.0"
    id("xyz.xenondevs.string-remapper-gradle-plugin") version "1.0.0"
    `maven-publish`
}

dependencies {
    implementation(project(":nova-api"))
    compileOnly(project(":nova-loader"))
    
    implementation(deps.bundles.kotlin)
    implementation(deps.bundles.ktor)
    implementation(deps.bundles.cbf)
    compileOnly(deps.bundles.maven.resolver)
    compileOnly(variantOf(deps.spigot.server) { classifier("remapped-mojang") })
    
    implementation("de.studiocode.invui:InvUI:0.8-SNAPSHOT") { for (i in 1..11) exclude(module = "IA-R$i") }
    implementation("de.studiocode.invui:ResourcePack:0.8-SNAPSHOT") { exclude(module = "InvUI") }
    implementation("de.studiocode.invui:IA-R11:0.8-SNAPSHOT:remapped-mojang")
    implementation("xyz.xenondevs:nms-utilities:0.1-SNAPSHOT:remapped-mojang")
    implementation("xyz.xenondevs:particle:1.8")
    implementation("xyz.xenondevs.bstats:bstats-bukkit:3.0.1")
    implementation("xyz.xenondevs.bytebase:ByteBase-Runtime:0.3-SNAPSHOT")
    implementation("xyz.xenondevs:minecraft-asset-downloader:1.0-SNAPSHOT")
    implementation("net.lingala.zip4j:zip4j:2.11.1")
    implementation("me.xdrop:fuzzywuzzy:1.4.0")
    implementation("software.amazon.awssdk:s3:2.17.259")
    
    // plugin dependencies
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.6")
    compileOnly("com.github.TechFortress:GriefPrevention:16.17.1") { isTransitive = false }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    compileOnly("com.plotsquared:PlotSquared-Core:6.9.4") { isTransitive = false }
    compileOnly("com.plotsquared:PlotSquared-Bukkit:6.9.4") { isTransitive = false }
    compileOnly("com.griefdefender:api:2.0.0-SNAPSHOT") { isTransitive = false }
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.1.0b") { isTransitive = false }
    compileOnly("com.github.TownyAdvanced:Towny:0.97.2.0") { isTransitive = false }
    compileOnly("com.github.Th0rgal:Oraxen:2ddf3c68b7") { isTransitive = false }
    compileOnly("io.lumine:MythicLib-dist:1.3") { isTransitive = false }
    compileOnly("net.Indyuce:MMOItems:6.7") { isTransitive = false }
    compileOnly("dev.espi:protectionstones:2.10.2") { isTransitive = false }
    compileOnly("org.maxgamer:QuickShop:5.1.0.7") { isTransitive = false }
    compileOnly("com.bekvon:Residence:5.0.1.6") { isTransitive = false }
    
    // test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.10")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/kotlin/"))
        }
    }
}

tasks {
    named<Jar>("jar") {
        dependsOn("remapStrings")
    }
    
    register("finalJar") {
        group = "build"
        dependsOn(if (mojangMapped) "jar" else "remapObfToSpigot")
    }
    
    register<Jar>("sources") {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        from("src/main/kotlin")
        archiveClassifier.set("sources")
    }
    
    register<Jar>("publishJar") {
        dependsOn("remapStrings")
        from(sourceSets.main.get().output)
        exclude("assets/*", "configs/*", "recipes/*", "*.bin", "*.yml")
    }
}

spigotRemap {
    spigotVersion.set(deps.versions.spigot.get().substringBefore('-'))
    sourceJarTask.set(tasks.jar)
    spigotJarClassifier.set("")
}

remapStrings {
    remapGoal.set(if (mojangMapped) "mojang" else "spigot")
    spigotVersion.set(deps.versions.spigot.get())
    classes.set(listOf(
        "xyz.xenondevs.nova.util.reflection.ReflectionRegistry",
        "xyz.xenondevs.nova.util.NMSUtils",
        "xyz.xenondevs.nova.transformer.patch.noteblock.NoteBlockPatch"
    ))
}

publishing {
    repositories {
        maven {
            credentials {
                name = "xenondevs"
                url = uri { "https://repo.xenondevs.xyz/releases/" }
                credentials(PasswordCredentials::class)
            }
        }
    }
    
    publications {
        // https://github.com/gradle/gradle/issues/11150
        var pomContent: String? = null
        create<MavenPublication>("internalNova") {
            from(components.getByName("kotlin"))
            pom.withXml { pomContent = asString().toString() }
        }
        
        create<MavenPublication>("nova") {
            artifacts { 
                artifact(tasks.getByName("publishJar"))
                artifact(tasks.getByName("sources"))
            }
            pom.withXml { 
                val builder = asString()
                builder.clear()
                builder.append(pomContent)
            }
        }
    }
}

tasks.getByName("generatePomFileForNovaPublication").dependsOn("generatePomFileForInternalNovaPublication")