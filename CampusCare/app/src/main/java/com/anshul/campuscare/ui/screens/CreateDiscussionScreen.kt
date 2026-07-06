package com.anshul.campuscare.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anshul.campuscare.data.repository.DiscussionRepository
import com.anshul.campuscare.data.repository.DuplicateDiscussionException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDiscussionScreen(
    discussionRepository: DiscussionRepository,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var buildingName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    
    // Duplicate handling state
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateException by remember { mutableStateOf<DuplicateDiscussionException?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    fun submitDiscussion(createAnyway: Boolean) {
        if (title.isBlank() || description.isBlank()) {
            Toast.makeText(context, "Title and description are required", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        coroutineScope.launch {
            val result = discussionRepository.createDiscussion(
                title = title,
                description = description,
                latitude = null,
                longitude = null,
                buildingName = buildingName.takeIf { it.isNotBlank() },
                createAnyway = createAnyway,
                imageUris = selectedImageUri?.let { listOf(it) } ?: emptyList()
            )

            isSubmitting = false

            result.onSuccess { discussion ->
                Toast.makeText(context, "Discussion posted!", Toast.LENGTH_SHORT).show()
                onNavigateToDetail(discussion.id)
            }.onFailure { error ->
                if (error is DuplicateDiscussionException) {
                    duplicateException = error
                    showDuplicateDialog = true
                } else {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Discussion") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            OutlinedTextField(
                value = buildingName,
                onValueChange = { buildingName = it },
                label = { Text("Building Name (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedImageUri == null) "Attach Image" else "Image Selected")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { submitDiscussion(createAnyway = false) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Post Discussion")
                }
            }
        }
    }

    if (showDuplicateDialog && duplicateException != null) {
        val dupInfo = duplicateException!!.duplicateResponse
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Similar Discussion Found") },
            text = {
                Column {
                    Text(dupInfo.message)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Match: ${(dupInfo.similarity * 100).toInt()}%",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Title: ${dupInfo.matchedDiscussion.title}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDuplicateDialog = false
                    onNavigateToDetail(dupInfo.matchedDiscussion.id)
                }) {
                    Text("Join Discussion")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDuplicateDialog = false
                    submitDiscussion(createAnyway = true)
                }) {
                    Text("Create Anyway")
                }
            }
        )
    }
}
