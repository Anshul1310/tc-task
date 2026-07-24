package com.anshul.campuscare.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.anshul.campuscare.data.repository.DiscussionRepository
import com.anshul.campuscare.data.repository.DuplicateDiscussionException
import kotlinx.coroutines.launch
import java.util.Locale

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
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Duplicate handling state
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateException by remember { mutableStateOf<DuplicateDiscussionException?>(null) }

    // Fetch Address from Node.js Server (which uses OpenCage API)
    fun fetchAddressFromBackend(lat: Double, lng: Double) {
        latitude = lat
        longitude = lng
        isFetchingLocation = true

        coroutineScope.launch {
            val address = discussionRepository.reverseGeocode(lat, lng)
            isFetchingLocation = false

            if (!address.isNullOrBlank()) {
                buildingName = address
                Toast.makeText(context, "Location address updated!", Toast.LENGTH_SHORT).show()
            } else {
                buildingName = "Lat: $lat, Lng: $lng"
                Toast.makeText(context, "Coordinates attached!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetch Device GPS Location
    fun fetchDeviceLocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            Toast.makeText(context, "Location Service unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            var location: Location? = null
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (location != null) {
                fetchAddressFromBackend(location.latitude, location.longitude)
            } else {
                Toast.makeText(context, "Could not fetch current GPS coordinates", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Location permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceLocation()
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchDeviceLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Image Picker (One-by-one addition up to 5)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (selectedImageUris.size < 5) {
                selectedImageUris = selectedImageUris + uri
            } else {
                Toast.makeText(context, "Maximum 5 images allowed", Toast.LENGTH_SHORT).show()
            }
        }
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
                latitude = latitude,
                longitude = longitude,
                buildingName = buildingName.takeIf { it.isNotBlank() },
                createAnyway = createAnyway,
                imageUris = selectedImageUris
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
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                placeholder = { Text("e.g. Broken water pipeline near Library") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                placeholder = { Text("Provide details about the issue or topic...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            // Location / Building Name Input & GPS Button
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = buildingName,
                    onValueChange = { buildingName = it },
                    label = { Text("Location / Address") },
                    placeholder = { Text("e.g. Central Library, Block A") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { requestLocation() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFetchingLocation
                ) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetching address from server...")
                    } else {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📍 Use My Current Location")
                    }
                }
            }

            // Attach Images Button (Max 5)
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedImageUris.size < 5,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selectedImageUris.isEmpty()) "Attach Image (0/5)"
                    else "Attach Image (${selectedImageUris.size}/5)"
                )
            }

            // Image Thumbnails Row with Remove Buttons
            if (selectedImageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedImageUris) { uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Remove Button (X)
                            IconButton(
                                onClick = {
                                    selectedImageUris = selectedImageUris - uri
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Submit Button
            Button(
                onClick = { submitDiscussion(createAnyway = false) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Post Discussion", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
