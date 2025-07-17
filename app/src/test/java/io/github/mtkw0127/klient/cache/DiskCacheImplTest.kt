package io.github.mtkw0127.klient.cache

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URI

class DiskCacheImplTest {
    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun `Given uri when saved body then get body from cache`() = runTest {
        // prepare
        val uri = URI("http://example.com/test")
        val byteArray = "This is a test body".toByteArray()
        val cacheDir = tempFolder.newFolder("cache")
        val instance = DiskCacheImpl(cacheDir)

        // exec
        instance.saveBody(uri, byteArray)

        // verify
        val bodyByteArray = instance.getBody(uri)
        assertArrayEquals(byteArray, bodyByteArray)
    }

    @Test
    fun `Given disk cache existed when Clear it then getBody is null`() = runTest {
        // prepare
        val uri = URI("http://example.com/test")
        val byteArray = "This is a test body".toByteArray()
        val cacheDir = tempFolder.newFolder("cache")
        val instance = DiskCacheImpl(cacheDir)
        instance.saveBody(uri, byteArray)

        // exec
        instance.clear(uri)

        // verify
        val bodyByteArray = instance.getBody(uri)
        assertNull(bodyByteArray)
    }
}