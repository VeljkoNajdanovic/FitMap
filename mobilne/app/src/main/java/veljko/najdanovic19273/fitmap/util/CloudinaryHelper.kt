package veljko.najdanovic19273.fitmap.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object CloudinaryHelper {

    private const val TAG = "CloudinaryHelper"
    private var isInitialized = false

    // Inicijalizacija Cloudinary-a
    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                val config = mapOf(
                    "cloud_name" to Constants.CLOUDINARY_CLOUD_NAME
                )
                MediaManager.init(context, config)
                isInitialized = true
                Log.d(TAG, "Cloudinary uspešno inicijalizovan")
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri inicijalizaciji Cloudinary-a", e)
            }
        }
    }

    // Upload slike na Cloudinary sa unsigned preset-om
    suspend fun uploadImage(
        imageUri: Uri,
        folder: String = "fitmap"
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            if (!isInitialized) {
                continuation.resume(Result.failure(Exception("Cloudinary nije inicijalizovan")))
                return@suspendCancellableCoroutine
            }

            Log.d(TAG, "Započinjem upload slike: $imageUri")

            MediaManager.get()
                .upload(imageUri)
                .unsigned(Constants.CLOUDINARY_UPLOAD_PRESET)
                .option("folder", folder)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Upload započet, requestId: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes.toDouble() / totalBytes.toDouble() * 100).toInt()
                        Log.d(TAG, "Upload progres: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            Log.d(TAG, "Upload uspešan! URL: $url")
                            continuation.resume(Result.success(url))
                        } else {
                            Log.e(TAG, "URL nije pronađen u rezultatu")
                            continuation.resume(Result.failure(Exception("URL nije pronađen")))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Upload greška: ${error.description}")
                        continuation.resume(Result.failure(Exception(error.description)))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "Upload reschedule: ${error.description}")
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            Log.e(TAG, "Greška pri upload-u", e)
            continuation.resume(Result.failure(e))
        }
    }
}
