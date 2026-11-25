plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
}

val versions = providers.gradleProperty("net.labymod.minecraft-versions").get().split(";")

group = "de.ulgu.fkreis"
version = providers.environmentVariable("VERSION").getOrElse("2.0.0")

labyMod {
    defaultPackageName = "de.ulgu.fkreis"

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
                getByName("client") {
                    devLogin = true
                }
            }
        }
    }

    addonInfo {
        namespace = "fkreis"
        displayName = "Freundeskreis"
        author = "Priester_Horst"
        description = "ULGU Software: profile cards, a sticker book, a notebook shelf with notes for every friend, friendship coins, a birthday calendar with HUD widget, a shareable photo album, Vanish (hide any player just for yourself) and a private diary with a leather cover you can dye."
        // Das Addon nutzt nur die LabyMod-API (kein versionsspezifischer Code),
        // deshalb laeuft es auf allen LabyMod-Versionen von 1.21 bis 26.3.
        minecraftVersion = "1.21<*"
        version = rootProject.version.toString()
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")

    group = rootProject.group
    version = rootProject.version

    extensions.findByType(JavaPluginExtension::class.java)?.apply {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
