package com.example.doineedto.data

object ReasonValidator {
    private val knownKeywords = setOf(
        "reply", "message", "text", "call", "email", "camera", "photo", "music", "podcast",
        "directions", "maps", "map", "navigation", "calendar", "reminder", "alarm", "bank",
        "payment", "pay", "notes", "note", "todo", "task", "weather", "train", "bus", "uber",
        "taxi", "translate", "browser", "search", "look", "check", "booking", "ticket",
        "document", "code", "work", "school", "study", "read", "article", "news", "spotify",
        "instagram", "insta", "tiktok", "twitter", "x", "reddit", "youtube", "whatsapp",
        "telegram", "discord", "slack", "messenger", "snapchat", "snap", "facebook",
        "linkedin", "gmail", "chrome", "safari", "netflix", "prime", "shopping", "amazon",
        "ebay", "shein", "zara", "doomscroll", "scroll", "feed", "reels", "shorts",
        "bored", "habit", "impulse", "curious", "curiosity", "waste", "timepass",
        "lurking", "meme", "memes", "game", "gaming", "chat", "dm", "post", "story"
    )

    private val distractionKeywords = setOf(
        "instagram", "insta", "tiktok", "twitter", "x", "reddit", "youtube", "snapchat",
        "facebook", "scroll", "doomscroll", "feed", "reels", "shorts", "bored", "habit",
        "impulse", "dopamine", "meme", "memes", "lurking", "game", "gaming"
    )

    fun isReasonValid(reason: String, curatedReasons: Set<String>): Boolean {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) return false
        if (curatedReasons.contains(trimmed)) return true

        val normalized = trimmed.lowercase()
        val words = normalized
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 }

        if (words.isEmpty()) return false
        if (looksLikeGibberish(normalized, words)) return false
        if (words.any(::matchesKnownKeyword)) return true

        val longWords = words.count { it.length >= 4 }
        return words.size >= 2 && longWords >= 1
    }

    fun normalizeReason(reason: String): String {
        return reason.lowercase()
            .trim()
            .replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ")
    }

    fun isDistractionReason(reason: String): Boolean {
        val words = normalizeReason(reason)
            .split(" ")
            .filter { it.length >= 2 }

        return words.any { word ->
            val normalizedForms = setOf(
                word,
                word.removeSuffix("s"),
                word.removeSuffix("es"),
                word.removeSuffix("ed"),
                word.removeSuffix("ing"),
            ).filter { it.length >= 2 }

            normalizedForms.any { candidate ->
                candidate in distractionKeywords || distractionKeywords.any { keyword ->
                    candidate.startsWith(keyword) || keyword.startsWith(candidate)
                }
            }
        }
    }

    private fun matchesKnownKeyword(word: String): Boolean {
        if (word in knownKeywords) return true

        val normalizedForms = setOf(
            word,
            word.removeSuffix("s"),
            word.removeSuffix("es"),
            word.removeSuffix("ed"),
            word.removeSuffix("ing"),
        ).filter { it.length >= 2 }

        return normalizedForms.any { candidate ->
            candidate in knownKeywords || knownKeywords.any { keyword ->
                candidate.startsWith(keyword) || keyword.startsWith(candidate)
            }
        }
    }

    private fun looksLikeGibberish(normalized: String, words: List<String>): Boolean {
        if (normalized.length < 4) return true
        if (Regex("^(.)\\1{3,}$").matches(normalized.replace(" ", ""))) return true

        val lettersOnly = normalized.filter { it.isLetter() }
        if (lettersOnly.length >= 5) {
            val vowelCount = lettersOnly.count { it in "aeiou" }
            if (vowelCount == 0) return true
        }

        val suspiciousWords = words.count { word ->
            word.length >= 5 && word.count { it in "aeiou" } == 0
        }
        return suspiciousWords == words.size && words.isNotEmpty()
    }
}
