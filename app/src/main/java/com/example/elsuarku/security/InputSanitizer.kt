package com.example.elsuarku.security

/**
 * Centralized input validation & sanitization for ElSuarKu.
 *
 * Protects against:
 * - NoSQL injection (Firestore query injection via $operators)
 * - XSS (cross-site scripting via HTML/script tags)
 * - Path traversal attacks (../, ..\, ~/)
 * - Malicious data injection into Firestore documents
 *
 * All user inputs MUST pass through this sanitizer before
 * being sent to Firestore or displayed in UI.
 */
object InputSanitizer {

    // === DANGEROUS PATTERNS ===
    // Using Kotlin Regex (not java.util.regex.Pattern) for clean Kotlin integration

    // Firestore document path injection
    private val PATH_TRAVERSAL = Regex("\\.\\./|\\.\\.\\\\|~/")

    // NoSQL operator injection: $where, $regex, $gt, etc.
    private val NOSQL_OPERATORS = Regex(
        "\\\$where|\\\$regex|\\\$gt|\\\$lt|\\\$ne|\\\$eq|\\\$in|\\\$nin|" +
        "\\\$or|\\\$and|\\\$not|\\\$nor|\\\$exists|\\\$type|\\\$mod|\\\$text|" +
        "\\\$search|\\\$elemMatch|\\\$all|\\\$size|\\\$inc|\\\$set|\\\$unset|" +
        "\\\$push|\\\$pop|\\\$pull|\\\$addToSet|\\\$each|\\\$position|\\\$slice|\\\$sort",
        setOf(RegexOption.IGNORE_CASE)
    )

    // Script/HTML injection
    private val XSS_PATTERN = Regex("<script|<img|<iframe|<svg|onerror=|onload=|javascript:|data:text/html|&#x3c;|&#60;|%3Cscript", RegexOption.IGNORE_CASE)

    // Control characters (NULL, BEL, BS, etc.)
    private val CONTROL_CHARS = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]")

    // Firebase/Sensitive key prefixes
    private val FIREBASE_RESERVED = Regex("^__.*|^\\.\\..*|^firebase.*|^google.*", RegexOption.IGNORE_CASE)

    // === PUBLIC METHODS ===

    /**
     * Sanitize a user-provided text field (name, description, visi, misi, etc.).
     * Strips dangerous characters and patterns.
     */
    fun sanitizeText(input: String, maxLength: Int = 5000): String {
        var sanitized = input
            .trim()
            .replace(CONTROL_CHARS, "")
            .replace(XSS_PATTERN, "")
            .replace(PATH_TRAVERSAL, "")
            .replace(NOSQL_OPERATORS, "")
            .replace(FIREBASE_RESERVED, "")
        if (sanitized.length > maxLength) sanitized = sanitized.substring(0, maxLength)
        return sanitized
    }

    /**
     * Validate email address.
     */
    fun isValidEmail(email: String): Boolean {
        if (email.length > 254) return false
        return android.util.Patterns.EMAIL_ADDRESS.matcher(sanitizeText(email, 254)).matches()
    }

    /**
     * Validate password strength.
     * Requirements: min 6 chars, at least 1 letter, at least 1 digit
     */
    fun isStrongPassword(password: String): Boolean {
        return password.length >= 6 &&
                password.any { it.isLetter() } &&
                password.any { it.isDigit() }
    }

    /**
     * Validate an election/candidate name (no special chars).
     */
    fun isValidName(name: String): Boolean {
        val sanitized = sanitizeText(name, 200)
        return sanitized.isNotBlank() && sanitized.length >= 2
    }

    /**
     * Sanitize a Firestore document ID — only alphanumeric + dash + underscore.
     */
    fun sanitizeDocumentId(id: String): String {
        return id.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(1500)
    }

    /**
     * Sanitize a map of data before writing to Firestore.
     * Recursively sanitizes all string values.
     */
    fun sanitizeMap(data: Map<String, Any?>): Map<String, Any?> {
        return data.mapValues { (key, value) ->
            when (value) {
                is String -> sanitizeText(value)
                is Map<*, *> -> sanitizeMap(value as Map<String, Any?>)
                else -> value
            }
        }.filterKeys { key ->
            !FIREBASE_RESERVED.matches(key)
        }
    }

    /**
     * Check if input contains any injection attempts.
     * Returns list of detected threats.
     */
    fun detectThreats(input: String): List<String> {
        val threats = mutableListOf<String>()
        if (XSS_PATTERN.containsMatchIn(input)) threats.add("XSS")
        if (NOSQL_OPERATORS.containsMatchIn(input)) threats.add("NoSQL_Injection")
        if (PATH_TRAVERSAL.containsMatchIn(input)) threats.add("Path_Traversal")
        if (FIREBASE_RESERVED.containsMatchIn(input)) threats.add("Reserved_Key")
        if (CONTROL_CHARS.containsMatchIn(input)) threats.add("Control_Chars")
        return threats
    }

    /**
     * Validate integer within bounds (for nomor urut, duration, etc.).
     */
    fun validateInt(input: String, min: Int = 0, max: Int = Int.MAX_VALUE): Int? {
        val num = input.trim().toIntOrNull() ?: return null
        return if (num in min..max) num else null
    }

    /**
     * Sanitize election ID / candidate ID for use in navigation routes.
     */
    fun sanitizeRouteParam(param: String): String {
        return param.replace(Regex("[^a-zA-Z0-9_\\-@.]"), "").take(256)
    }
}
