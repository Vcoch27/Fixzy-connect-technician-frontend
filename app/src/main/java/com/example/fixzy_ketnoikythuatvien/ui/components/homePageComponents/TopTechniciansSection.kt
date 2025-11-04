package com.example.fixzy_ketnoikythuatvien.ui.components.homePageComponents

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import coil.compose.rememberImagePainter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fixzy_ketnoikythuatvien.R
import com.example.fixzy_ketnoikythuatvien.redux.data_class.AppState
import com.example.fixzy_ketnoikythuatvien.redux.store.Store
import com.example.fixzy_ketnoikythuatvien.service.model.TopTechnician
import com.example.fixzy_ketnoikythuatvien.ui.screen.controller.CategoryController
import com.example.fixzy_ketnoikythuatvien.ui.screen.controller.TopTechniciansController
import com.example.fixzy_ketnoikythuatvien.ui.theme.AppTheme
import com.example.fixzy_ketnoikythuatvien.ui.theme.LocalAppTypography

private const val TAG = "TopTechniciansSection"

@Composable
fun TopTechniciansSection(modifier: Modifier = Modifier,navController: NavController) {
    LaunchedEffect(Unit) {
        CategoryController.fetchCategories()
        TopTechniciansController.fetchTechnicians()
    }
    var selectedCategoryId by remember { mutableStateOf(-1) }

    val state by Store.stateFlow.collectAsState()
//    Log.d(
//        TAG,
//        "Trạng thái hiện tại: selectedCategoryId=$selectedCategoryId, số kỹ thuật viên=${state.topTechnicians.size}, số danh mục=${state.categories.size}"
//    )

    LaunchedEffect(selectedCategoryId) {
        if (selectedCategoryId == -1) {
            TopTechniciansController.fetchTechnicians()
        } else {
            TopTechniciansController.fetchTechnicians(selectedCategoryId)
        }
    }

    val typography = LocalAppTypography.current

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .height(16.dp)
                        .width(4.dp)
                        .background(AppTheme.colors.mainColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Top technicians",
                    style = typography.titleSmall,
                    color = AppTheme.colors.onBackground
                )
            }

//            Button(
//                onClick = {},
//                shape = RoundedCornerShape(40),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color.White,
//                    contentColor = AppTheme.colors.mainColor
//                ),
//                elevation = ButtonDefaults.buttonElevation(0.dp), // Loại bỏ bóng
//                border = ButtonDefaults.outlinedButtonBorder // Viền mỏng quanh nút
//            ) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Text(
//                        text = "See All",
//                        fontSize = 12.sp
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Icon(
//                        painter = painterResource(R.drawable.baseline_arrow_forward_ios_24),
//                        contentDescription = "Arrow",
//                        tint = AppTheme.colors.onBackgroundVariant,
//                        modifier = Modifier.size(14.dp)
//                    )
//                }
//            }


        }

        FilterSection(
            state = state,
            selectedCategory = selectedCategoryId,
            onCategorySelected = { categoryId ->
                val newCategoryId = if (categoryId == "All") -1 else categoryId.toIntOrNull() ?: -1
                selectedCategoryId = newCategoryId
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TopTechniciansListSection(selectedCategory = selectedCategoryId, state = state, navController = navController)
    }
}

private const val TAG_FILTER_SECTION = "FilterSection"

@Composable
fun FilterSection(
    state: AppState,
    selectedCategory: Int,
    onCategorySelected: (String) -> Unit
) {
    Log.d(
        TAG_FILTER_SECTION,
        "Rendering FilterSection: isLoadingCategories=${state.isLoadingCategories}, categoriesCount=${state.categories.size}, selectedCategory=$selectedCategory"
    )

    when {
        state.isLoadingCategories -> {
            Log.i(TAG_FILTER_SECTION, "Displaying loading state for categories")
            Text(
                text = "Loading...",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        state.categoriesError != null -> {
            Log.e(TAG_FILTER_SECTION, "Category error: ${state.categoriesError}")
            Text(
                text = "Error: ${state.categoriesError}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        else -> {
            Log.d(
                TAG_FILTER_SECTION,
                "Rendering category chips: ${state.categories.map { it.name }}"
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip "All"
                item(key = "all") {
                    FilterChip(
                        category = "All",
                        isElected = selectedCategory == -1,
                        onClick = {
                            Log.i(TAG_FILTER_SECTION, "All chip clicked")
                            onCategorySelected("All")
                        }
                    )
                }

                // Category chips
                items(
                    items = state.categories,
                    key = { it.categoryId }
                ) { category ->
                    FilterChip(
                        category = category.name,
                        isElected = selectedCategory == category.categoryId,
                        onClick = {
                            Log.i(
                                TAG_FILTER_SECTION,
                                "Category chip clicked: ${category.name} (id=${category.categoryId})"
                            )
                            onCategorySelected(category.categoryId.toString())
                        }
                    )
                }
            }
        }
    }
}


private const val TAG_FILTER_CHIP = "FilterChip"

@Composable
fun FilterChip(category: String, isElected: Boolean, onClick: () -> Unit) {
    Log.d(TAG_FILTER_CHIP, "Rendering FilterChip: category=$category, isElected=$isElected")
    Button(
        onClick = {
            Log.i(TAG_FILTER_CHIP, "FilterChip clicked: category=$category")
            onClick()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isElected) AppTheme.colors.mainColor else Color.White,
            contentColor = if (isElected) Color.White else AppTheme.colors.mainColor
        ),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .height(36.dp)
            .padding(horizontal = 4.dp),
        border = BorderStroke(1.dp, AppTheme.colors.mainColor)
    ) {
        Text(text = category, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private const val TAG_TECHNICIANS_LIST = "TopTechniciansListSection"

@Composable
fun TopTechniciansListSection(selectedCategory: Int, state: AppState,navController: NavController) {
    Log.d(
        TAG_TECHNICIANS_LIST,
        "Rendering TopTechniciansListSection: selectedCategory=$selectedCategory, isLoading=${state.isLoading}, techniciansCount=${state.topTechnicians.size}"
    )

    when {
        state.isLoading -> {
            Log.i(TAG_TECHNICIANS_LIST, "Displaying loading state for technicians")
            Text(
                text = "Loading technicians...",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        state.error != null -> {
            Log.e(TAG_TECHNICIANS_LIST, "Technicians error: ${state.error}")
            Text(
                text = "Error: ${state.error}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        else -> {
            val filteredList = state.topTechnicians.filter {
                selectedCategory == -1 || it.categoryName == state.categories.find { category ->
                    category.categoryId == selectedCategory
                }?.name
            }
            Log.d(
                TAG_TECHNICIANS_LIST,
                "Filtered technicians: count=${filteredList.size}, selectedCategory=$selectedCategory"
            )

            if (filteredList.isEmpty()) {
                Log.i(TAG_TECHNICIANS_LIST, "No technicians available for selected category")
                Text(
                    text = "No technicians available",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    style = AppTheme.typography.bodySmall
                )
            } else {
                Log.d(TAG_TECHNICIANS_LIST, "Rendering ${filteredList.size} technician cards")
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredList.forEach { technician ->
                        TechnicianCard(technician, navController)
                    }
                }
            }
        }
    }
}
private const val TAG_TECHNICIAN_CARD = "TechnicianCard"

@Composable
fun TechnicianCard(technician: TopTechnician,navController: NavController) {
    var isFavorite by remember { mutableStateOf(false) }
    Log.d(
        TAG_TECHNICIAN_CARD,
        "Rendering TechnicianCard: technician=${technician.name}, service=${technician.serviceName}, isFavorite=$isFavorite"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                val providerId = technician.id
                navController.navigate("provider_screen/$providerId")
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar
            AsyncImage(
                model = technician.avatarUrl,
                contentDescription = "${technician.name}'s avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.coc),
                error = painterResource(id = R.drawable.coc),
                onLoading = {
                    Log.d(TAG_TECHNICIAN_CARD, "Loading avatar for ${technician.name}: ${technician.avatarUrl}")
                },
                onSuccess = {
                    Log.d(TAG_TECHNICIAN_CARD, "Avatar loaded successfully for ${technician.name}")
                },
                onError = {
                    Log.e(TAG_TECHNICIAN_CARD, "Failed to load avatar for ${technician.name}: ${technician.avatarUrl}")
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Thông tin kỹ thuật viên
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tên dịch vụ
                Text(
                    text = technician.name,
                    style = AppTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.onSurface
                )

                // Tên kỹ thuật viên
                Text(
                    text = technician.serviceName,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onBackgroundVariant
                )

                Text(
                    text = "${technician.price.toDouble().toInt().toString()} VND",
                    style = AppTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.mainColor
                )


                // Đánh giá và số đơn
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Hiển thị sao dựa trên rating
                    val rating = technician.rating.coerceIn(0.0, 5.0)
                    val fullStars = rating.toInt()
                    val hasHalfStar = rating % 1 >= 0.5
                    repeat(5) { index ->
                        Icon(
                            imageVector = when {
                                index < fullStars -> Icons.Default.Star
                                index == fullStars && hasHalfStar -> Icons.AutoMirrored.Filled.StarHalf
                                else -> Icons.Default.StarBorder
                            },
                            contentDescription = null,
                            tint = Color(0xFFF3B700),
                            modifier = Modifier.size(16.dp)
                        )
                    }

//                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = String.format("%.1f", technician.rating),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onBackgroundVariant
                    )

                    Text(
                        text = " | ${technician.completedOrders} Đơn",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onBackgroundVariant
                    )
                }
            }

            // Nút yêu thích
            IconButton(onClick = {
                isFavorite = !isFavorite
                Log.i(TAG_TECHNICIAN_CARD, "Favorite button clicked for ${technician.name}: isFavorite=$isFavorite")
            }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Xóa khỏi yêu thích" else "Thêm vào yêu thích",
                    tint = if (isFavorite) AppTheme.colors.secondarySurface else AppTheme.colors.onBackgroundVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}