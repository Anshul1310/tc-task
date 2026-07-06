package com.anshul.campuscare.ui.screens

// ──────────────────────────────────────────────
// Create / Edit Item Screen
//
// A form for creating a new lost or found item,
// or editing an existing one. When an itemId is
// provided, the form loads the existing item data
// and switches to "edit mode".
//
// Form fields:
//   - Title (text)
//   - Description (multiline text)
//   - Category (dropdown)
//   - Location (text)
//   - Date (date picker)
//   - Status (LOST / FOUND toggle) — only for create
//   - Images (gallery picker)
// ──────────────────────────────────────────────

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.anshul.campuscare.data.model.Item
import com.anshul.campuscare.data.repository.ItemRepository
import com.anshul.campuscare.ui.theme.LostColor
import com.anshul.campuscare.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateItemScreen(
    editItemId: Int? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Whether we're editing an existing item or creating new
    val isEditMode: Boolean = (editItemId != null)

    // ── Form State ────────────────────────────
    var title: String by remember { mutableStateOf("") }
    var description: String by remember { mutableStateOf("") }
    var category: String by remember { mutableStateOf("") }
    var location: String by remember { mutableStateOf("") }
    var date: String by remember { mutableStateOf("") }
    var status: String by remember { mutableStateOf("LOST") }
    var selectedImageUris: List<Uri> by remember { mutableStateOf(emptyList()) }

    // ── UI State ──────────────────────────────
    var isSubmitting: Boolean by remember { mutableStateOf(false) }
    var isLoadingItem: Boolean by remember { mutableStateOf(isEditMode) }
    var errorMessage: String? by remember { mutableStateOf(null) }
    var showCategoryDropdown: Boolean by remember { mutableStateOf(false) }

    // ── Categories ────────────────────────────
    val categories: List<String> = listOf(
        "Electronics",
        "Wallet",
        "Keys",
        "ID Card",
        "Books",
        "Clothing",
        "Accessories",
        "Bag",
        "Water Bottle",
        "Umbrella",
        "Other"
    )

    // ── Image Picker ──────────────────────────
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris = selectedImageUris + uris
        }
    }

    // ── Load existing item for edit mode ──────
    if (isEditMode) {
        LaunchedEffect(editItemId) {
            isLoadingItem = true
            val result: Result<Item> = ItemRepository.getItemById(itemId = editItemId!!)
            if (result.isSuccess) {
                val existingItem: Item = result.getOrNull()!!
                title = existingItem.title
                description = existingItem.description
                category = existingItem.category
                location = existingItem.location
                date = existingItem.dateLostOrFound.take(n = 10)
                status = existingItem.status
            } else {
                errorMessage = "Failed to load item for editing"
            }
            isLoadingItem = false
        }
    }

    // ── Submit Form ───────────────────────────
    fun submitForm() {
        // Basic validation
        if (title.isBlank()) {
            errorMessage = "Title is required"
            return
        }
        if (description.isBlank()) {
            errorMessage = "Description is required"
            return
        }
        if (category.isBlank()) {
            errorMessage = "Please select a category"
            return
        }
        if (location.isBlank()) {
            errorMessage = "Location is required"
            return
        }
        if (date.isBlank()) {
            errorMessage = "Date is required"
            return
        }

        isSubmitting = true
        errorMessage = null

        coroutineScope.launch {
            if (isEditMode) {
                // Update existing item
                val result = ItemRepository.updateItem(
                    itemId = editItemId!!,
                    title = title,
                    description = description,
                    category = category,
                    location = location,
                    date = date,
                    imageUris = selectedImageUris,
                    context = context
                )
                if (result.isSuccess) {
                    onNavigateBack()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to update item"
                }
            } else {
                // Create new item
                val result = ItemRepository.createItem(
                    title = title,
                    description = description,
                    category = category,
                    location = location,
                    date = date,
                    status = status,
                    imageUris = selectedImageUris,
                    context = context
                )
                if (result.isSuccess) {
                    onNavigateBack()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to create item"
                }
            }
            isSubmitting = false
        }
    }

    // ── UI ────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Item" else "Report Item",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Text(text = "←", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (isLoadingItem) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding)
                    .verticalScroll(state = rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(height = 8.dp))

                // ── Status Toggle (Create only) ──
                if (!isEditMode) {
                    Text(
                        text = "What happened?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(height = 8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        FilterChip(
                            selected = (status == "LOST"),
                            onClick = { status = "LOST" },
                            label = { Text(text = "I Lost Something") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LostColor,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = (status == "FOUND"),
                            onClick = { status = "FOUND" },
                            label = { Text(text = "I Found Something") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(height = 16.dp))
                }

                // ── Title ─────────────────────
                OutlinedTextField(
                    value = title,
                    onValueChange = { newValue: String -> title = newValue },
                    label = { Text(text = "Title") },
                    placeholder = { Text(text = "e.g., Black Wallet") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(size = 12.dp)
                )

                Spacer(modifier = Modifier.height(height = 12.dp))

                // ── Description ───────────────
                OutlinedTextField(
                    value = description,
                    onValueChange = { newValue: String -> description = newValue },
                    label = { Text(text = "Description") },
                    placeholder = { Text(text = "Describe the item in detail...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 120.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(size = 12.dp)
                )

                Spacer(modifier = Modifier.height(height = 12.dp))

                // ── Category Dropdown ─────────
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text(text = "Category") },
                        placeholder = { Text(text = "Select a category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryDropdown = true },
                        readOnly = true,
                        enabled = false,
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    // Make the whole field clickable
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showCategoryDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        for (categoryOption: String in categories) {
                            DropdownMenuItem(
                                text = { Text(text = categoryOption) },
                                onClick = {
                                    category = categoryOption
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(height = 12.dp))

                // ── Location ──────────────────
                OutlinedTextField(
                    value = location,
                    onValueChange = { newValue: String -> location = newValue },
                    label = { Text(text = "Location") },
                    placeholder = { Text(text = "e.g., Library, Canteen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(size = 12.dp)
                )

                Spacer(modifier = Modifier.height(height = 12.dp))

                // ── Date ──────────────────────
                OutlinedTextField(
                    value = date,
                    onValueChange = { newValue: String -> date = newValue },
                    label = { Text(text = "Date (YYYY-MM-DD)") },
                    placeholder = { Text(text = "e.g., 2025-01-15") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(size = 12.dp)
                )

                Spacer(modifier = Modifier.height(height = 16.dp))

                // ── Image Picker ──────────────
                Text(
                    text = "Photos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(height = 8.dp))

                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(size = 12.dp)
                ) {
                    Text(text = "📷 Add Photos")
                }

                // Show selected images
                if (selectedImageUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(height = 8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        items(items = selectedImageUris) { uri: Uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .size(size = 80.dp)
                                    .clip(shape = RoundedCornerShape(size = 8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(height = 20.dp))

                // ── Error Message ─────────────
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = LostColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // ── Submit Button ─────────────
                Button(
                    onClick = { submitForm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 52.dp),
                    shape = RoundedCornerShape(size = 14.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(size = 24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(width = 8.dp))
                        Text(text = "Submitting...")
                    } else {
                        Text(
                            text = if (isEditMode) "Update Item" else "Submit Report",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(height = 32.dp))
            }
        }
    }
}
