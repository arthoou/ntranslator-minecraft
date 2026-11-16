package com.arthou.ntranslator

import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path

class PlatformProxyImpl : PlatformProxy {
    companion object {
        private val BETA_VERSION_REGEX = Regex("""(\d+)\.(\d+)(?:\.(\d+))?-beta""")
    }

    private val loader: FabricLoader
        get() = FabricLoader.getInstance()

    override val isDev: Boolean
        get() = loader.isDevelopmentEnvironment

    override val gameDir: Path
        get() = loader.gameDir

    override val configDir: Path
        get() = loader.configDir

    override val modVersion: String
        get() {
            val raw = loader.getModContainer(NTranslator.MOD_ID)
                .map { it.metadata.version.friendlyString }
                .orElse("unknown")
            return formatDisplayVersion(raw)
        }

    override fun isModLoaded(id: String): Boolean {
        return loader.isModLoaded(id)
    }

    override fun isClient(): Boolean {
        return loader.environmentType == EnvType.CLIENT
    }

    private fun formatDisplayVersion(raw: String): String {
        val match = BETA_VERSION_REGEX.matchEntire(raw) ?: return raw
        val major = match.groupValues[1]
        val minor = match.groupValues[2]
        val patch = match.groupValues.getOrElse(3) { "" }

        return buildString {
            append("beta ")
            append(major)
            append('.')
            append(minor)

            if (patch.isNotBlank() && patch != "0") {
                append('.')
                append(patch)
            }
        }
    }
}

