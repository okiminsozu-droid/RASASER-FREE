package com.rasaser.free.icerikureticisi

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * RFC 7636 + RFC 8252 native authorization utilities. No secret is shipped.
 * An actual grant requires a developer-registered public OAuth client ID,
 * provider consent, verified redirect, remote identity verification, and
 * approved scopes. This class NEVER marks a pasted profile as connected.
 */
data class NativePkceRequest(
    val clientId: String,
    val provider: String,
    val redirectUri: String,
    val state: String,
    val codeVerifier: String,
    val codeChallenge: String,
    val authorizationUrl: String
)

object OAuthPkceCore {
    private val random = SecureRandom()
    private const val PKCE_VERIFIER_BYTES = 48
    private const val OAUTH_STATE_BYTES = 32

    private fun urlSafe(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun challengeFor(verifier: String): String =
        urlSafe(MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(StandardCharsets.US_ASCII)))

    fun newVerifier(): String =
        ByteArray(PKCE_VERIFIER_BYTES).also(random::nextBytes).let(::urlSafe)

    fun newState(): String =
        ByteArray(OAUTH_STATE_BYTES).also(random::nextBytes).let(::urlSafe)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    /**
     * Official X native-public-client OAuth 2.0 route.
     * The developer MUST register redirectUri exactly in X Console.
     * offline.access requests a refresh token; it can still be revoked.
     */
    fun xAuthorization(
        registeredClientId: String,
        registeredRedirectUri: String
    ): NativePkceRequest {
        require(registeredClientId.length in 8..256 &&
            registeredClientId.all { it.isLetterOrDigit() || it in "_-" }) {
            "X geliştirici uygulamasının kayıtlı OAuth Client ID değeri gerekli."
        }
        val uri = URI(registeredRedirectUri)
        require(uri.scheme in setOf("https", "com.rasaser.free.icerikureticisi") &&
            uri.host != null && uri.userInfo == null && uri.rawFragment == null) {
            "X geliştirici panelinde kaydedilmiş güvenli dönüş adresi gerekli."
        }
        val verifier = newVerifier()
        val state = newState()
        val scope = "tweet.read tweet.write users.read offline.access"
        val url = "https://x.com/i/oauth2/authorize" +
            "?response_type=code" +
            "&client_id=" + encode(registeredClientId) +
            "&redirect_uri=" + encode(registeredRedirectUri) +
            "&scope=" + encode(scope) +
            "&state=" + encode(state) +
            "&code_challenge=" + encode(challengeFor(verifier)) +
            "&code_challenge_method=S256"
        return NativePkceRequest(
            clientId = registeredClientId,
            provider = "X",
            redirectUri = registeredRedirectUri,
            state = state,
            codeVerifier = verifier,
            codeChallenge = challengeFor(verifier),
            authorizationUrl = url
        )
    }

    /** Must validate exact state and provider callback before token exchange. */
    fun stateMatches(expected: String, returned: String): Boolean =
        expected.isNotBlank() && expected.length >= 32 &&
            MessageDigest.isEqual(
                expected.toByteArray(StandardCharsets.UTF_8),
                returned.toByteArray(StandardCharsets.UTF_8)
            )
}
