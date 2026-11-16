package com.arthou.ntranslator.client.translation

import com.arthou.ntranslator.Language
import java.util.concurrent.CompletableFuture

@Suppress("UNUSED_PARAMETER")
object ClientTranslationHelper {
    fun translateTextAsync(text: String, targetLanguage: Language) =
        CompletableFuture.completedFuture(TranslatedText(targetLanguage, text, complete = text.isBlank()))

    fun translateLinesAsync(lines: List<String>, targetLanguage: Language) =
        CompletableFuture.completedFuture(TranslatedLines(targetLanguage, lines, complete = lines.all { it.isBlank() }))

    fun cachedLanguage(text: String): Language? = null

    fun detectLanguage(text: String): Language = Language.ENGLISH
}

data class TranslatedText(val sourceLanguage: Language, val text: String, val complete: Boolean = true)

data class TranslatedLines(val sourceLanguage: Language, val lines: List<String>, val complete: Boolean = true)
