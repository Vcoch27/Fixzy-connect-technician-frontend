// service/AuthService.kt
@file:Suppress("DEPRECATION")

package com.example.fixzy_ketnoikythuatvien.service

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fixzy_ketnoikythuatvien.data.model.UserData
import com.example.fixzy_ketnoikythuatvien.data.model.UserDataResponse
import com.example.fixzy_ketnoikythuatvien.redux.action.Action
import com.example.fixzy_ketnoikythuatvien.redux.store.Store
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import com.example.fixzy_ketnoikythuatvien.BuildConfig
import com.example.fixzy_ketnoikythuatvien.R
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.fixzy_ketnoikythuatvien.service.model.GoogleUserDataRequest

@Suppress("DEPRECATION")
class AuthService(
    private val context: Context,
    private val activity: Activity?,
    private val onSuccess: ((UserData) -> Unit)?,
    private val onError: ((String) -> Unit)?,
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val store = Store.Companion.store
    private val TAG = "AUTH_SERVICE"

    private val apiService = ApiClient.apiService
    private var isFetchingUserData = false

    private val signInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
        Log.d("AuthDebug", "GoogleSignInClient created with WEB_CLIENT_ID: ${BuildConfig.WEB_CLIENT_ID}")
        Log.d("AuthDebug", "Google Sign-In Options: $gso")
        GoogleSignIn.getClient(context, gso)

    }

    fun launchGoogleSignIn(launcher: ActivityResultLauncher<Intent>) {
        signInClient.signOut().addOnCompleteListener {
            val signInIntent = signInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    fun handleGoogleSignInResult(
        result: ActivityResult,
        onSuccess: (UserData) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d("AuthDebug", "Handling Google Sign-In result: resultCode=${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                Log.d("AuthDebug", "Google account: ${account?.email}, idToken: ${idToken?.take(10)}...")
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken, onSuccess, onError)
                } else {
                    Log.e("AuthDebug", "ID Token is null. Ensure WEB_CLIENT_ID is correct.")
                    onError("ID Token null")
                }
            } catch (e: ApiException) {
                Log.e("AuthDebug", "Google Sign-In failed: ${e.statusCode} - ${e.localizedMessage}")
                onError("Google Sign-In thất bại: ${e.localizedMessage}")
            }
        } else {
            Log.w("AuthDebug", "Google Sign-In canceled or failed, resultCode=${result.resultCode}")
            onError("Đăng nhập Google đã bị hủy")
        }
    }

    private fun firebaseAuthWithGoogle(
        idToken: String,
        onSuccess: (UserData) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d("AuthDebug", "Authenticating with Firebase using ID Token...")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Log.d("AuthDebug", "Firebase credential: $credential")
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val user = auth.currentUser
                Log.d("AuthDebug", "Firebase sign-in success. Current user: ${user?.email}")
                if (user != null) {
                    val userData = UserData(
                        name = user.displayName ?: "",
                        email = user.email ?: "",
                        firebase_uid = user.uid,
                        phone = user.phoneNumber ?: "",
                        address = null,
                        avatarUrl = user.photoUrl?.toString(),
                        role = "user"
                    )
                    Log.d("AuthDebug", "Syncing user data with backend: $userData")
                    store.dispatch(Action.SyncGoogleUserLoading)

                    // Lưu vào Firestore ngay lập tức
                    firestore.collection("users")
                        .document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d("AuthDebug", "User data saved successfully to Firestore")
                        }
                        .addOnFailureListener { e ->
                            Log.e("AuthDebug", "Failed to save user data to Firestore: ${e.message}")
                        }

                    // Lấy token và đồng bộ với backend
                    user.getIdToken(false).addOnSuccessListener { tokenResult ->
                        val firebaseToken = tokenResult.token
                        Log.d("AuthDebug", "Firebase token retrieved: ${firebaseToken?.take(10)}...")
                        
                        if (firebaseToken == null) {
                            Log.e("AuthDebug", "Firebase token is null")
                            store.dispatch(Action.SyncGoogleUserFailure("Không thể lấy token xác thực"))
                            onError("Không thể lấy token xác thực")
                            return@addOnSuccessListener
                        }

                        apiService.syncGoogleUser(
                            "Bearer $firebaseToken",
                            GoogleUserDataRequest(
                                id = user.uid,
                                email = user.email ?: "",
                                name = user.displayName,
                                phone = user.phoneNumber,
                                address = "",
                                avatarUrl = user.photoUrl?.toString() ?: ""
                            )
                        ).enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                if (response.isSuccessful) {
                                    Log.d("AuthDebug", "User data synced successfully with backend")
                                    store.dispatch(Action.SyncGoogleUserSuccess("Đồng bộ người dùng thành công"))
                                    onSuccess(userData)
                                    getUserData()
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    Log.e("AuthDebug", "Backend sync failed: ${response.code()} - $errorBody")
                                    store.dispatch(Action.SyncGoogleUserFailure("Đồng bộ dữ liệu thất bại: ${errorBody ?: response.message()}"))
                                    onError("Đồng bộ dữ liệu thất bại: ${errorBody ?: response.message()}")
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Log.e("AuthDebug", "Backend sync failed: ${t.message}")
                                store.dispatch(Action.SyncGoogleUserFailure("Lỗi kết nối: ${t.message}"))
                                onError("Lỗi kết nối: ${t.message}")
                            }
                        })
                    }.addOnFailureListener { e ->
                        Log.e("AuthDebug", "Failed to get Firebase token: ${e.message}")
                        store.dispatch(Action.SyncGoogleUserFailure("Không thể lấy token xác thực: ${e.message}"))
                        onError("Không thể lấy token xác thực: ${e.message}")
                    }
                } else {
                    Log.e("AuthDebug", "User is null after Firebase sign-in")
                    store.dispatch(Action.SyncGoogleUserFailure("Người dùng null sau khi xác thực"))
                    onError("Người dùng null sau khi xác thực")
                }
            }
            .addOnFailureListener { e ->
                Log.e("AuthDebug", "Firebase authentication failed: ${e.message}")
                store.dispatch(Action.SyncGoogleUserFailure("Xác thực Firebase thất bại: ${e.message}"))
                onError("Xác thực Firebase thất bại: ${e.message}")
            }
    }

    fun getUserData() {
        Log.d(TAG, "-------------getUserData called-----------")
        if (isFetchingUserData) {
            Log.d(TAG, "Skipping duplicate getUserData call")
            return
        }
        isFetchingUserData = true

        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid.isNullOrEmpty()) {
            Log.e(TAG, "No Firebase UID found")
            store.dispatch(Action.FetchUserDataFailure("Không tìm thấy Firebase UID"))
            isFetchingUserData = false
            return
        }

        Log.e(TAG, "Fetching user data for UID: $firebaseUid")
        store.dispatch(Action.FetchUserDataStart(firebaseUid))
        Log.d(TAG, "Dispatch FetchUserDataStart completed")

        apiService.getUserData(firebaseUid).enqueue(object : Callback<UserDataResponse> {
            override fun onResponse(
                call: Call<UserDataResponse>,
                response: Response<UserDataResponse>,
            ) {
                isFetchingUserData = false
                Log.d(TAG, "GetUserData response received with code: ${response.code()}")

                try {
                    val userDataResponse = response.body()
                    Log.d(TAG, "Response body: $userDataResponse")

                    if (response.isSuccessful) {
                        if (userDataResponse?.success == true && userDataResponse.user != null) {
                            Log.d(TAG, "Checking role: ${userDataResponse.user.role}")
                            val validRoles = setOf("user", "technician")
                            val role = userDataResponse.user.role
                            if (role == null || role !in validRoles) {
                                Log.e(TAG, "Invalid or null role: $role")
                                store.dispatch(Action.FetchUserDataFailure("Vai trò không hợp lệ hoặc rỗng"))
                                return
                            }
                            val userData = userDataResponse.toUserData()
                            Log.d(TAG, "Dispatching FetchUserDataSuccess with UserData: $userData")
                            store.dispatch(Action.FetchUserDataSuccess(userData))
                            Log.d(TAG, "Dispatch FetchUserDataSuccess completed")
                        } else {
                            Log.w(TAG, "Response success=false or user null")
                            store.dispatch(Action.FetchUserDataFailure("Dữ liệu người dùng trống"))
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error body: $errorBody")
                        val errorMessage = try {
                            val errorJson = JSONObject(errorBody ?: "{}")
                            errorJson.getString("message") ?: "Lỗi: ${response.code()}"
                        } catch (e: Exception) {
                            "Lỗi khi lấy dữ liệu: ${response.code()}"
                        }
                        store.dispatch(Action.FetchUserDataFailure(errorMessage))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Crash in onResponse: ${e.message}", e)
                    store.dispatch(Action.FetchUserDataFailure("Lỗi xử lý dữ liệu: ${e.message ?: "Lỗi không xác định"}"))
                }
            }

            override fun onFailure(call: Call<UserDataResponse>, t: Throwable) {
                isFetchingUserData = false
                Log.e(TAG, "GetUserData failed: ${t.message}", t)
                store.dispatch(Action.FetchUserDataFailure("Lỗi kết nối: ${t.message ?: "Không thể kết nối"}"))
            }
        })
    }

    fun signUp(
        email: String,
        password: String,
        name: String,
        phone: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        Log.i(
            TAG,
            "Starting signUp with email: $email, name: $name, phone: $phone"
        ) // Log thông tin đầu vào
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Firebase sign-up successful") // Log khi Firebase Auth thành công
                    val user = auth.currentUser
                    if (user != null) {
                        Log.i(TAG, "Current user UID: ${user.uid}") // Log UID của người dùng
                        val userData = UserData(
                            name = name,
                            email = email,
                            firebase_uid = user.uid,
                            phone = phone,
                            address = null,
                            avatarUrl = null,
                            role = "user"
                        )
                        Log.i(
                            TAG,
                            "Saving userData to Firestore: $userData"
                        ) // Log trước khi lưu vào Firestore
                        firestore.collection("users")
                            .document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.i(TAG, "User data saved to Firestore: $userData") // Đã có
                                syncUserWithBackend(userData, onSuccess, onError)
                                getUserData()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error saving user data: ${e.message}") // Đã có
                                onError("Failed to save user data")
                            }
                    } else {
                        Log.e(TAG, "Current user is null after sign-up") // Log nếu user là null
                        onError("User creation failed")
                    }
                } else {
                    Log.e(TAG, "Sign up failed: ${task.exception?.message}") // Đã có
                    onError(task.exception?.message ?: "Sign up failed")
                }
            }
    }

    private fun syncUserWithBackend(
        userData: UserData,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        Log.i(TAG, "Starting syncUserWithBackend with userData: $userData")
        val gson = Gson()
        val jsonBody = gson.toJson(userData)
        Log.i(TAG, "JSON body being sent: $jsonBody") // Log JSON thực tế
        apiService.syncUser(userData).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.i(TAG, "API Response Code: ${response.code()}")
                if (response.isSuccessful) {
                    Log.i(TAG, "User synced with backend: ${response.body()?.string()}")
                    store.dispatch(Action.setUser(userData))
                    onSuccess()
                } else {
                    Log.e(TAG, "Backend sync failed: ${response.code()}")
                    Log.e(TAG, "Backend error message: ${response.message()}")
                    Log.e(TAG, "Backend error body: ${response.errorBody()?.string()}")
                    onError("Failed to sync with backend: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Backend sync error: ${t.message}")
                Log.e(TAG, "Backend sync throwable: ${t.stackTraceToString()}")
                onError("Failed to sync with backend: ${t.message}")
            }
        })
    }

    fun login(
        email: String,
        password: String,
        onSuccess: (UserData) -> Unit,
        onError: (String) -> Unit,
    ) {
        Log.i(TAG, "Starting login with email: $email")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Firebase login successful")
                    val user = auth.currentUser
                    if (user != null) {
                        Log.i(TAG, "Current user UID: ${user.uid}")
                        // Lấy ID Token từ Firebase
                        user.getIdToken(false).addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val idToken = tokenTask.result?.token
                                Log.i(TAG, "ID Token retrieved: $idToken")
                                if (idToken != null) {
                                    // Gửi ID Token đến backend để xác minh
                                    authenticateWithBackend(idToken, onSuccess, onError)
                                    getUserData()
                                } else {
                                    Log.e(TAG, "ID Token is null")
                                    onError("Failed to retrieve ID Token")
                                }
                            } else {
                                Log.e(
                                    TAG,
                                    "Failed to retrieve ID Token: ${tokenTask.exception?.message}"
                                )
                                onError("Failed to retrieve ID Token: ${tokenTask.exception?.message}")
                            }
                        }
                    } else {
                        Log.e(TAG, "Current user is null after login")
                        onError("Login failed: User not found")
                    }
                } else {
                    Log.e(TAG, "Login failed: ${task.exception?.message}")
                    onError(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun authenticateWithBackend(
        idToken: String,
        onSuccess: (UserData) -> Unit,
        onError: (String) -> Unit,
    ) {
        Log.i(TAG, "Authenticating with backend using ID Token")
        apiService.authenticate("Bearer $idToken").enqueue(object : Callback<UserData> {
            override fun onResponse(call: Call<UserData>, response: Response<UserData>) {
                Log.i(TAG, "Backend auth response code: ${response.code()}")
                if (response.isSuccessful) {
                    val userData = response.body()
                    if (userData != null) {
                        Log.i(TAG, "User data from backend: $userData")
                        syncUserWithBackend(
                            userData,
                            onSuccess = {
                                onSuccess(userData)
                            },
                            onError = onError
                        )
                        Log.i(TAG, "ID Token retrieved: $idToken")

                    } else {
                        Log.e(TAG, "User data from backend is null")
                        onError("Failed to retrieve user data from backend")
                    }
                } else {
                    Log.e(TAG, "Backend auth failed: ${response.code()}")
                    Log.e(TAG, "Backend auth error: ${response.errorBody()?.string()}")
                    onError("Backend authentication failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UserData>, t: Throwable) {
                Log.e(TAG, "Backend auth error: ${t.message}")
                Log.e(TAG, "Backend auth throwable: ${t.stackTraceToString()}")
                onError("Failed to authenticate with backend: ${t.message}")
            }
        })
    }


    fun signOut(onComplete: (() -> Unit)? = null) {
        auth.signOut()
        signInClient.signOut().addOnCompleteListener {
            Log.d("AuthDebug", "GoogleSignInClient signed out")
            onComplete?.invoke()
        }
    }

    init {
        Log.d("AuthDebug", "Checking Google Services configuration...")
        Log.d("AuthDebug", "Package name: ${context.packageName}")
    }
}