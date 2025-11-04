package com.example.fixzy_ketnoikythuatvien.service

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.fixzy_ketnoikythuatvien.data.model.CategoryData
import com.example.fixzy_ketnoikythuatvien.redux.action.Action
import com.example.fixzy_ketnoikythuatvien.redux.store.Store
import com.example.fixzy_ketnoikythuatvien.service.model.CategoryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CategoryService {
    private val TAG = "CATEGORY_SERVICE"
    private val apiService = ApiClient.apiService
    private val store = Store.Companion.store

    fun fetchCategories() {
        store.dispatch(Action.FetchCategoriesRequest)

        apiService.getCategories().enqueue(object : Callback<CategoryResponse> {
            override fun onResponse(call: Call<CategoryResponse>, response: Response<CategoryResponse>) {
                if (response.isSuccessful) {
                    val categoryResponse = response.body()
                    if (categoryResponse?.success == true) {
                        val colors = listOf(
                            Color(0xFFFFE8AD),
                            Color(0xFFFC8EA7),
                            Color(0xFF969CEC),
                            Color(0xFFA1D6E2)
                        )
                        val categories = categoryResponse.data.mapIndexed { index, apiCategory ->
                            CategoryData(
                                categoryId = apiCategory.category_id,
                                name = apiCategory.name,
                                iconUrl = apiCategory.icon_url,
                                backgroundColor = colors[index % colors.size]
                            )
                        }

                        store.dispatch(Action.FetchCategoriesSuccess(categories))
                    } else {
                        store.dispatch(Action.FetchCategoriesFailure("API returned success: false"))
                    }
                } else {
                    store.dispatch(Action.FetchCategoriesFailure("HTTP error: ${response.message()}"))
                }
            }

            override fun onFailure(call: Call<CategoryResponse>, t: Throwable) {
                store.dispatch(Action.FetchCategoriesFailure("Network error: ${t.message ?: "Unknown error"}"))
            }
        })
    }
}
