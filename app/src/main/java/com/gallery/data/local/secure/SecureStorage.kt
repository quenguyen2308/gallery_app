package com.gallery.data.local.secure

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val secureDir: File by lazy {
        File(context.filesDir, "secure_folder").apply { mkdirs() }
    }

    /** Encrypts [input] into app-private storage and returns the absolute path. */
    suspend fun encryptToFile(input: InputStream): String = withContext(Dispatchers.IO) {
        val file = File(secureDir, "${UUID.randomUUID()}.enc")
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()
        encryptedFile.openFileOutput().use { out ->
            input.copyTo(out)
        }
        file.absolutePath
    }

    fun openDecryptedStream(path: String): InputStream {
        val file = File(path)
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()
        return encryptedFile.openFileInput()
    }

    fun delete(path: String) {
        File(path).delete()
    }
}
