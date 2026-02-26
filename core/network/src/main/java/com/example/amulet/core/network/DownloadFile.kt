package com.example.amulet.core.network

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

/**
 * Интерфейс для скачивания файлов.
 * Реализация находится в core/network, data слой использует этот интерфейс.
 */
interface FileDownloader {
    suspend fun download(url: String): AppResult<ByteArray>
}

/**
 * Реализация FileDownloader через OkHttpClient.
 */
class OkHttpFileDownloader(
    private val client: okhttp3.OkHttpClient
) : FileDownloader {

    override suspend fun download(url: String): AppResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        Ok(body.bytes())
                    } else {
                        Err(AppError.Network)
                    }
                } else {
                    Err(AppError.Network)
                }
            }
        } catch (e: IOException) {
            Err(AppError.Network)
        } catch (e: Exception) {
            Err(AppError.Unknown)
        }
    }
}
