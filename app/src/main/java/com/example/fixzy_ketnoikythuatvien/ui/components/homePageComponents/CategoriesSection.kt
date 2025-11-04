package com.example.fixzy_ketnoikythuatvien.ui.components.homePageComponents

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fixzy_ketnoikythuatvien.R
import com.example.fixzy_ketnoikythuatvien.data.model.CategoryData
import com.example.fixzy_ketnoikythuatvien.redux.store.Store
import com.example.fixzy_ketnoikythuatvien.service.CategoryService
import com.example.fixzy_ketnoikythuatvien.ui.screen.controller.CategoryController
import com.example.fixzy_ketnoikythuatvien.ui.theme.AppTheme
import com.example.fixzy_ketnoikythuatvien.ui.theme.AppTypography
import com.example.fixzy_ketnoikythuatvien.ui.theme.LocalAppTypography

@Composable
fun CategoriesSection(modifier: Modifier = Modifier,navController: NavController) {
    val TAG = "CategoriesSection"
    val typography = LocalAppTypography.current
    val state by Store.stateFlow.collectAsState()

    LaunchedEffect(Unit) {
        CategoryController.fetchCategories()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .height(16.dp)
                        .width(4.dp)
                        .background(AppTheme.colors.mainColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Categories",
                    style = typography.titleSmall,
                    color = AppTheme.colors.onBackground
                )
            }
            Button(
                onClick = {},
                shape = RoundedCornerShape(40),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = AppTheme.colors.mainColor
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    navController.navigate("all_categories")
                }) {
                    Text(text = "See All", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.baseline_arrow_forward_ios_24),
                        contentDescription = "Arrow",
                        tint = AppTheme.colors.onBackgroundVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoadingCategories -> {
                Log.d(TAG, "Categories are loading...")
                Text(
                    text = "Loading...",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            state.categoriesError != null -> {
                Log.e(TAG, "Error loading categories: ${state.categoriesError}")
                Text(
                    text = "Error: ${state.categoriesError}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            else -> {
                Log.d(TAG, "Loaded categories: ${state.categories.map { it.name }}")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    items(state.categories.take(4)) { category ->
                        CategoryItem(category, typography, onClick = {})
                    }
                }
            }
        }
    }
}
@Composable
fun CategoryItem(category: CategoryData, typography: AppTypography, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable {
                Log.d("CategoryItem", "Clicked on ${category.name}")
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(category.backgroundColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = category.iconUrl,
                contentDescription = category.name,
                modifier = Modifier.size(32.dp),
                placeholder = painterResource(R.drawable.placeholder),
                error = painterResource(R.drawable.placeholder)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = category.name, fontSize = 12.sp, style = typography.bodySmall)
    }
}
