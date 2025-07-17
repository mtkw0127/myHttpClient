package io.github.mtkw0127.klient.cache

import java.net.URI

interface Cache {
    /**
     * Save the byte array to the cache with the given URI.
     *
     * @param uri The URI to save the byte array under.
     * @param byteArray The byte array to save.
     */
    suspend fun saveBody(
        uri: URI,
        byteArray: ByteArray
    )

    /**
     * Retrieve the byte array from the cache using the given URI.
     *
     * @param uri The URI to retrieve the byte array from.
     * @return The byte array if found, or null if not found.
     */
    suspend fun getBody(uri: URI): ByteArray?

    /**
     * Check if the cache contains an entry for the given URI.
     *
     * @param uri The URI to check.
     * @return True if the cache contains an entry for the URI, false otherwise.
     */
    suspend fun clear(uri: URI)

    /**
     * Clear all entries in the cache.
     */
    suspend fun clearAll()
}