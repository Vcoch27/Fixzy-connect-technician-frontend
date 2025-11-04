package com.example.fixzy_ketnoikythuatvien.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.cloudinary.Cloudinary
import com.cloudinary.Transformation
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.fixzy_ketnoikythuatvien.data.model.UserData
import com.example.fixzy_ketnoikythuatvien.redux.action.Action
import com.example.fixzy_ketnoikythuatvien.redux.store.Store
import com.example.fixzy_ketnoikythuatvien.service.model.TopTechnician
import com.example.fixzy_ketnoikythuatvien.service.model.TopTechnicianResponse
import com.example.fixzy_ketnoikythuatvien.service.model.UpdateProfileRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resumeWithException

private const val TAG = "UserService"

class UserService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val apiService = ApiClient.apiService
    suspend fun fetchTopTechnicians(categoryId: String? = null): TopTechnicianResponse {
        val response = apiService.getTopTechnicians(categoryId)
        return response
    }

    suspend fun dispatch(action: Action, dispatch: (Action) -> Unit) {
        Log.d(TAG, "Xử lý action: $action")
        when (action) {
            is Action.FetchTopTechnicians -> {
                Log.d(TAG, "Xử lý FetchTopTechnicians với categoryId: ${action.categoryId}")
                try {
                    val response = fetchTopTechnicians(action.categoryId)
                    val technicians = response.data.map { dto ->
                        Log.d(
                            TAG,
                            "Ánh xạ kỹ thuật viên: id=${dto.technician_id}, name=${dto.full_name}"
                        )
                        TopTechnician(
                            id = dto.technician_id,
                            name = dto.full_name ?: "Unknown",
                            avatarUrl = dto.avatar_url ?: "",
                            serviceName = dto.service_name,
                            price = dto.service_price,
                            rating = dto.service_rating,
                            completedOrders = dto.service_orders_completed,
                            categoryName = dto.category_name,
                            categoryId = dto.category_id
                        )
                    }
                    Log.i(
                        TAG,
                        "Đã ánh xạ ${technicians.size} kỹ thuật viên: ${technicians.map { it.name }}"
                    )
                    dispatch(Action.TopTechniciansLoaded(technicians))
                    Log.i(TAG, "Đã gửi Action.TopTechniciansLoaded")
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi lấy kỹ thuật viên: ${e.message}", e)
                    dispatch(Action.TopTechniciansLoadFailed(e.message ?: "Lỗi không xác định"))
                    Log.i(TAG, "Đã gửi Action.TopTechniciansLoadFailed")
                }
            }

            else -> {
                Log.w(TAG, "Action không được xử lý: $action")
            }
        }
    }

    suspend fun updateProfile(
        fullName: String?,
        phone: String?,
        address: String?,
        avatarUrl: String? = "https://res.cloudinary.com/dlkrskgwq/image/upload/v1746646758/fixzy/avatars/apr8ahowuvez4hvjahp8.png",
        context: Context
    ) {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) {
            Log.d(
                TAG,
                "Request firebaseUid: $firebaseUid,  fullname: $fullName, phone: $phone, address: $address, avatarUrl: $avatarUrl"
            )
            val response = apiService.updateUserProfile(
                UpdateProfileRequest(
                    firebaseUid = firebaseUid,
                    fullName = fullName,
                    phone = phone,
                    address = address,
                    avatarUrl = avatarUrl
                )

            )

            Log.d(TAG, "Response from server: $response")

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to update profile: ${response.message()}")
                throw IOException("Failed to update profile: ${response.message()}")
            }else{


                val authService = AuthService(
                    context = context,
                    activity = null,
                    onSuccess = null,
                    onError =null
                )
                authService.getUserData()
            }
        } else {
            Log.e(TAG, "No Firebase UID found")
            throw IOException("No Firebase UID found")
        }

    }

    suspend fun uploadAvatar(context: Context, imageUri: Uri): String =
        suspendCancellableCoroutine { continuation ->
            MediaManager.get()
                .upload(imageUri)
                .option("folder", "fixzy/avatars")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {
                        Log.d("UploadAvatar", "Bắt đầu upload: $requestId")
                    }

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                        // progress, if needed
                    }

                    override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            continuation.resume(url) { cause, _, _ -> }
                        } else {
                            continuation.resumeWithException(IOException("No secure_url in result"))
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        continuation.resumeWithException(IOException("Upload error: ${error?.description}"))
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        continuation.resumeWithException(IOException("Upload rescheduled: ${error?.description}"))
                    }
                })
                .dispatch()
        }


}