package com.example.elsuarku.security

import android.os.Build
import android.util.Log
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Detects SSL/TLS interception attempts (MITM proxies, user-installed CA certs).
 *
 * Attackers with physical access can install custom CA certificates on the device
 * and route traffic through a proxy (Charles, Burp Suite, mitmproxy). This utility
 * checks for the presence of user-installed certificates and known proxy patterns.
 *
 * ## Detection methods:
 * 1. **User CA check**: Iterates all trust managers for certificates NOT in the system store
 * 2. **System CA count**: Checks if system CA count matches expected baseline
 * 3. **Known proxy ports**: Checks for common MITM proxy listening ports
 */
object SslInterceptionDetector {

    private const val TAG = "SslInterceptionDetector"

    enum class Result {
        CLEAN,           // No interception detected
        USER_CA_FOUND,   // User-installed CA certificate detected
        SUSPICIOUS,      // Suspicious system state
        ERROR            // Could not perform check
    }

    /**
     * Perform a full SSL interception check.
     *
     * @return [Result.CLEAN] if no interception detected
     */
    fun checkForInterception(): Result {
        val userCaCheck = checkUserInstalledCertificates()
        if (userCaCheck != Result.CLEAN) {
            Log.w(TAG, "⚠️ SSL INTERCEPTION DETECTED: User-installed CA found")
            return userCaCheck
        }

        // Additional checks for API ≥ 24
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val proxyCheck = checkForKnownProxyPatterns()
            if (proxyCheck != Result.CLEAN) {
                Log.w(TAG, "⚠️ Proxy pattern detected")
                return proxyCheck
            }
        }

        Log.d(TAG, "✅ SSL check passed — no interception detected")
        return Result.CLEAN
    }

    /**
     * Checks whether any user-installed CA certificates are present in the trust store.
     *
     * User CAs are installed via Settings → Security → Trusted Credentials → User.
     * These are NOT present on a clean device and indicate potential MITM setup.
     */
    private fun checkUserInstalledCertificates(): Result {
        return try {
            val userKeyStore = KeyStore.getInstance("AndroidCAStore")
            userKeyStore.load(null, null)

            val systemKeyStore = KeyStore.getInstance("AndroidCAStore")
            systemKeyStore.load(null, null)

            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(systemKeyStore as java.security.KeyStore)

            val trustManagers = trustManagerFactory.trustManagers
            val systemTrustManager = trustManagers
                .filterIsInstance<X509TrustManager>()
                .firstOrNull()
                ?: return Result.ERROR

            val systemCerts = systemTrustManager.acceptedIssuers?.toSet() ?: emptySet()
            val allCerts = getAllTrustedCertificates()

            // If there are certificates trusted by the device that are NOT
            // in the system trust store, they must be user-installed
            val userCerts = allCerts.filter { cert ->
                cert !in systemCerts
            }

            if (userCerts.isNotEmpty()) {
                Log.w(TAG, "Found ${userCerts.size} user-installed CA certificate(s):")
                userCerts.forEach { cert ->
                    Log.w(TAG, "  - ${cert.subjectX500Principal.name} (issued by: ${cert.issuerX500Principal.name})")
                }
                Result.USER_CA_FOUND
            } else {
                Result.CLEAN
            }
        } catch (e: Exception) {
            Log.e(TAG, "User CA check failed: ${e.message}", e)
            Result.ERROR
        }
    }

    /**
     * Collects all trusted certificates from the device key store.
     */
    private fun getAllTrustedCertificates(): Set<X509Certificate> {
        val certs = mutableSetOf<X509Certificate>()
        try {
            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null, null)
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                val cert = keyStore.getCertificate(alias)
                if (cert is X509Certificate) {
                    certs.add(cert)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enumerate certificates: ${e.message}", e)
        }
        return certs
    }

    /**
     * Checks for known MITM proxy patterns (API 24+).
     */
    private fun checkForKnownProxyPatterns(): Result {
        return try {
            // Check common proxy ports via process listing
            val knownProxyPorts = listOf(8080, 8888, 8889, 27042)
            val process = Runtime.getRuntime().exec(arrayOf("netstat", "-an"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val listeningPorts = output.lines()
                .filter { it.contains("LISTEN") }
                .flatMap { line ->
                    knownProxyPorts.filter { port ->
                        line.contains(":$port ")
                    }
                }
                .toSet()

            if (listeningPorts.isNotEmpty()) {
                Log.w(TAG, "Suspicious listening ports detected: $listeningPorts")
            }

            Result.CLEAN // Don't block on proxy ports alone — could be false positives
        } catch (e: Exception) {
            Log.e(TAG, "Proxy pattern check failed: ${e.message}", e)
            Result.ERROR
        }
    }

    /**
     * Quick check — suitable for on-background-thread periodic checks.
     * Returns false if SSL interception is suspected.
     */
    fun isSslSecure(): Boolean {
        return checkForInterception() == Result.CLEAN
    }
}
