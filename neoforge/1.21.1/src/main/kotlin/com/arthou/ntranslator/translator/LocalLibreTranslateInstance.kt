package com.arthou.ntranslator.translator

import com.sun.management.OperatingSystemMXBean
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.common.LifecycleEvent
import net.minecraft.Util
import net.minecraft.network.chat.Component
import net.minecraft.util.HttpUtil
import net.minecraft.util.Mth
import com.arthou.ntranslator.NTranslator
import com.arthou.ntranslator.client.NTranslatorClient
import java.io.File
import java.lang.management.ManagementFactory
import java.util.*
import java.util.function.Consumer

class LocalLibreTranslateInstance private constructor(val process: Process, val port: Int) : LibreTranslateInstance("http://127.0.0.1:$port", 150) {
    init {
        info("Started local LibreTranslate instance on port $port.")

        LifecycleEvent.SERVER_STOPPING.register {
            process.destroy()
        }

        if (NTranslator.instance.proxy.isClient())
            registerEventsClient()
    }

    private fun registerEventsClient() {
        ClientLifecycleEvent.CLIENT_STOPPING.register {
            process.destroy()
        }
    }

    companion object {
        private const val EXECUTABLE_PROPERTY = "nexel.libreTranslateExecutable"
        private const val EXECUTABLE_ENV = "NEXEL_LIBRETRANSLATE_EXECUTABLE"
        private var lastPid = -1L
        var hasStarted = false
        var currentInstance: LocalLibreTranslateInstance? = null

        val libreTranslateDir = File(NTranslator.instance.proxy.gameDir.toFile(), ".nexel")

        fun canRunLibreTranslate(): Boolean {
            val operatingSystemBean = ManagementFactory.getOperatingSystemMXBean()
            val availableSystemMemory = if (operatingSystemBean is OperatingSystemMXBean) {
                operatingSystemBean.freeMemorySize
            } else {
                Runtime.getRuntime().freeMemory()
            }

            val availableForTranslatorMb = (availableSystemMemory.coerceAtLeast(0L) / 1048576L)
            return (Runtime.getRuntime().availableProcessors() >= 2 || TranslatorManager.supportsCuda) &&
                    availableForTranslatorMb >= 768
        }

        // TODO: make translatable
        private fun warn(text: String) {
            try {
                if (NTranslator.instance.proxy.isClient()) {
                    NTranslatorClient.displayMessage(Component.literal(text), true)
                } else {
                    NTranslator.logger.warn(text)
                }
            } catch (_: Throwable) {
                System.err.println("[NEXEL/WARN] $text")
            }
        }

        private fun info(text: String) {
            try {
                if (NTranslator.instance.proxy.isClient()) {
                    NTranslatorClient.displayMessage(Component.literal(text), false)
                } else {
                    NTranslator.logger.info(text)
                }
            } catch (_: Throwable) {
                System.out.println("[NEXEL/INFO] $text")
            }
        }

        fun killOpenInstances() {
            if (lastPid == -1L) {
                hasStarted = false
                currentInstance = null
                return
            }

            ProcessHandle.of(lastPid)
                .ifPresent {
                    info("Detected LibreTranslate instance ${lastPid}, killing.")
                    it.destroyForcibly()

                    lastPid = -1L
                }

            hasStarted = false
            currentInstance = null
        }

        private fun clearDeadDirectories() {
            val files = libreTranslateDir.listFiles()

            if (files != null) {
                for (file in files) {
                    if (!file.isDirectory)
                        continue

                    if (file.name.startsWith("_MEI")) {
                        if (!file.deleteRecursively()) {
                            warn("Failed to delete unused LibreTranslate directories, this may mean a dead LibreTranslate instance is running on your computer!")
                            warn("Please try to terminate any \"libretranslate.exe\" processes that you see running, then restart your game.")
                        }
                    }
                }
            }
        }

        private fun configuredExecutable(): File? {
            val configured = System.getProperty(EXECUTABLE_PROPERTY)
                ?: System.getenv(EXECUTABLE_ENV)
                ?: NTranslator.config.server.libreTranslateExecutable.takeIf { it.isNotBlank() }
                ?: return null

            val file = File(configured)
            if (file.exists()) {
                return file
            }

            warn("Configured LibreTranslate executable was not found: ${file.absolutePath}")
            return null
        }

        private fun findExecutableInPath(platform: Util.OS): File? {
            val path = System.getenv("PATH") ?: return null
            val executable = if (platform == Util.OS.WINDOWS) "libretranslate.exe" else "libretranslate"

            return path.split(File.pathSeparator)
                .asSequence()
                .map { File(it, executable) }
                .firstOrNull { it.exists() && it.isFile }
        }

        private fun processLogFile(): File {
            if (!libreTranslateDir.exists()) {
                libreTranslateDir.mkdirs()
            }

            return File(libreTranslateDir, "libretranslate.log")
        }

        private fun commandForSource(source: File, platform: Util.OS): List<String> {
            if (platform != Util.OS.WINDOWS && source.isFile) {
                val firstLine = runCatching { source.bufferedReader().use { it.readLine() } }.getOrNull()
                if (firstLine?.startsWith("#!") == true && File("/bin/sh").exists()) {
                    return listOf("/bin/sh", source.absolutePath)
                }
            }

            return listOf(source.absolutePath)
        }

        fun launchLibreTranslate(source: File, consumer: Consumer<LibreTranslateInstance>) {
            val port = if (HttpUtil.isPortAvailable(5000)) 5000 else HttpUtil.getAvailablePort()

            if (lastPid != -1L) {
                killOpenInstances()
            }

            clearDeadDirectories()

            if (!source.canExecute()) {
                if (!source.setExecutable(true)) {
                    NTranslator.logger.error("Unable to start local LibreTranslate instance! You may have to manually set the execute permission on the file yourself!")
                    NTranslator.logger.error("File path: ${source.absolutePath}")
                    return
                }
            }

            val platform = Util.getPlatform()
            val processBuilder = ProcessBuilder(commandForSource(source, platform) + listOf(
                "--host", "127.0.0.1",
                "--port",
                "$port",
                "--threads",
                "${Mth.clamp(NTranslator.config.server.libreTranslateThreads, 1, Runtime.getRuntime().availableProcessors())}",
                "--disable-web-ui",
                "--disable-files-translation"
            ))

            processBuilder.directory(libreTranslateDir)

            val environment = processBuilder.environment()
            environment["PYTHONIOENCODING"] = "utf-8"
            environment["PYTHONLEGACYWINDOWSSTDIO"] = "utf-8"

            val logFile = processLogFile()
            if (logFile.length() > 1024L * 1024L) {
                logFile.writeText("")
            }

            processBuilder
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))

            val process = processBuilder.start()
            lastPid = process.pid()

            val timer = Timer()

            val launchedPid = process.pid()
            process.onExit()
                .whenCompleteAsync { exitedProcess, e ->
                    e?.printStackTrace()

                    if (!hasStarted) {
                        timer.cancel()
                        val exitCode = exitedProcess.exitValue()
                        warn("LibreTranslate appears to have exited with code $exitCode, not proceeding with local translator instance.")
                        warn("LibreTranslate process log: ${logFile.absolutePath}")

                    }

                    if (lastPid == launchedPid) {
                        hasStarted = false
                        currentInstance = null
                        lastPid = -1L
                    }
                }

            var attempts = 0
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    try {
                        val instance = LocalLibreTranslateInstance(process, port)

                        currentInstance = instance
                        consumer.accept(instance)

                        timer.cancel()
                        hasStarted = true
                    } catch (_: Exception) {
                    }
                }
            }, 2500L, 2500L)
        }

        fun findLocalExecutable(): File? {
            val platform = Util.getPlatform()
            if (platform !in listOf(Util.OS.WINDOWS, Util.OS.OSX, Util.OS.LINUX)) return null
            return configuredExecutable() ?: findExecutableInPath(platform)
        }
    }
}
