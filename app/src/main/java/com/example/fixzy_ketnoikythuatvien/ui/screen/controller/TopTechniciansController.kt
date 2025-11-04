package com.example.fixzy_ketnoikythuatvien.ui.screen.controller

import android.util.Log
import com.example.fixzy_ketnoikythuatvien.redux.action.Action
import com.example.fixzy_ketnoikythuatvien.redux.store.Store
import com.example.fixzy_ketnoikythuatvien.service.UserService
import com.example.fixzy_ketnoikythuatvien.service.model.TopTechnicianResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "TopTechniciansController"

object TopTechniciansController {
    private val userService = UserService()
    private var isFetching = false

    suspend fun fetchTechnicians(categoryId: Int? = null): TopTechnicianResponse {
        if (isFetching) {
            return TopTechnicianResponse(success = false, data = emptyList())
        }
        isFetching = true
        return try {
            val response = withContext(Dispatchers.IO) {
                userService.fetchTopTechnicians(categoryId?.toString())
            }
            isFetching = false
            userService.dispatch(Action.FetchTopTechnicians(categoryId?.toString())) { action ->
                Store.store.dispatch(action)
            }
            response
        } catch (e: Exception) {
            isFetching = false
            userService.dispatch(Action.TopTechniciansLoadFailed(e.message ?: "Lỗi không xác định")) { action ->
                Store.store.dispatch(action)
            }
            TopTechnicianResponse(success = false, data = emptyList())
        }
    }
}