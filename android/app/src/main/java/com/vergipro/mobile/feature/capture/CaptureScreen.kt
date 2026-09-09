package com.vergipro.mobile.feature.capture

import android.net.Uri
import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.IntentSenderRequest
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.vergipro.mobile.core.data.DocumentItem
import com.vergipro.mobile.core.network.ApiFailure
import com.vergipro.mobile.core.security.SecureDocumentVault
import com.vergipro.mobile.core.designsystem.VPColor
import com.vergipro.mobile.core.designsystem.formattedTRY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.annotation.StringRes

enum class DocTypeOption(val code: String, @StringRes val titleRes: Int, @StringRes val descRes: Int) {
    EXPENSE("EXPENSE", com.vergipro.mobile.R.string.capture_type_expense, com.vergipro.mobile.R.string.capture_type_expense_detail),
    FUEL("FUEL", com.vergipro.mobile.R.string.capture_type_fuel, com.vergipro.mobile.R.string.capture_type_fuel_detail),
    RECEIPT("RECEIPT", com.vergipro.mobile.R.string.capture_type_receipt, com.vergipro.mobile.R.string.capture_type_receipt_detail),
    SALES("SALES", com.vergipro.mobile.R.string.capture_type_sales, com.vergipro.mobile.R.string.capture_type_sales_detail),
}

@Composable
fun CaptureScreen(
    organizationId: Int,
    onUploadDocument: (fileBytes: ByteArray, filename: String, docType: String, description: String, plate: String, isHarici: Boolean, onResult: (Result<DocumentItem>) -> Unit) -> Unit = { _, _, _, _, _, _, cb -> cb(Result.failure(ApiFailure.NetworkUnavailable)) },
    onUploadBatch: (files: List<Pair<ByteArray, String>>, docType: String, description: String, plate: String, isHarici: Boolean, onResult: (Result<List<DocumentItem>>) -> Unit) -> Unit = { _, _, _, _, _, cb -> cb(Result.failure(ApiFailure.NetworkUnavailable)) },
    onImmersiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val vault = remember { SecureDocumentVault(context.applicationContext) }
    val draftStore = remember { EncryptedCaptureDraftStore(context.applicationContext) }
    var selectedPages by remember(organizationId) { mutableStateOf<List<CapturedPage>>(emptyList()) }
    var selectedDocType by remember { mutableStateOf(DocTypeOption.EXPENSE) }
    var description by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var isHarici by remember { mutableStateOf(false) }

    var isUploading by remember { mutableStateOf(false) }
    var uploadStatusText by remember { mutableStateOf("") }
    var uploadSuccessDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var uploadSuccessDocs by remember { mutableStateOf<List<DocumentItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDraftProtectedAfterFailure by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showBrandedCamera by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    fun persistUris(uris: List<Uri>) {
        scope.launch {
            val previousPages = selectedPages
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    uris.mapIndexed { index, uri ->
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error(context.getString(com.vergipro.mobile.R.string.capture_read_failed))
                        val name = "belge_${System.currentTimeMillis()}_${index + 1}.jpg"
                        val document = vault.store(bytes, organizationId, name, "image/jpeg")
                        CapturedPage(document.id.toString(), bytes, document)
                    }
                }
            }
            val persisted = result.getOrNull()
            if (persisted != null) {
                selectedPages = previousPages + persisted
                withContext(Dispatchers.IO) {
                    draftStore.save(CaptureDraftManifest(organizationId, selectedPages.map { it.vaultDocument }))
                }
                errorMessage = null
            } else {
                selectedPages = previousPages
                errorMessage = context.getString(com.vergipro.mobile.R.string.capture_secure_draft_save_failed)
            }
        }
    }

    fun removePage(page: CapturedPage) {
        selectedPages = selectedPages.filterNot { it.id == page.id }
        val remaining = selectedPages
        scope.launch(Dispatchers.IO) {
            runCatching { vault.delete(page.vaultDocument) }
            runCatching { draftStore.save(CaptureDraftManifest(organizationId, remaining.map { it.vaultDocument })) }
        }
    }

    fun clearDraft() {
        val pagesToDelete = selectedPages
        selectedPages = emptyList()
        scope.launch(Dispatchers.IO) {
            pagesToDelete.forEach { runCatching { vault.delete(it.vaultDocument) } }
            runCatching { draftStore.save(CaptureDraftManifest(organizationId, emptyList())) }
        }
    }

    LaunchedEffect(organizationId) {
        val restored = withContext(Dispatchers.IO) {
            runCatching {
                draftStore.load(organizationId).documents.map { document ->
                    CapturedPage(document.id.toString(), vault.read(document), document, document.createdAt)
                }
            }
        }
        selectedPages = restored.getOrElse {
            errorMessage = context.getString(com.vergipro.mobile.R.string.capture_secure_draft_load_failed)
            emptyList()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (captured && uri != null) {
            persistUris(listOf(uri))
            errorMessage = null
            uploadSuccessDoc = null
            uploadSuccessDocs = emptyList()
        }
    }

    val documentScanner = remember {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
    }

    val documentScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val pages = GmsDocumentScanningResult
                .fromActivityResultIntent(activityResult.data)
                ?.pages
                ?.map { it.imageUri }
                .orEmpty()
            if (pages.isNotEmpty()) persistUris(pages)
        }
    }

    fun openCamera() {
        val output = File.createTempFile("vergipro_capture_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", output)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }


    fun openDocumentScanner() {
        val activity = context as? Activity
        if (activity == null) {
            openCamera()
            return
        }
        documentScanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                documentScannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                // Devices without a compatible Google Play services scanner still
                // retain a working camera capture path.
                openCamera()
            }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showBrandedCamera = true
            onImmersiveChanged(true)
        } else {
            onImmersiveChanged(false)
            errorMessage = context.getString(com.vergipro.mobile.R.string.capture_camera_permission)
        }
    }

    fun openBrandedCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showBrandedCamera = true
            onImmersiveChanged(true)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Multiple Image Picker
    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            persistUris(uris)
            errorMessage = null
            uploadSuccessDoc = null
            uploadSuccessDocs = emptyList()
        }
    }

    if (showBrandedCamera) {
        BackHandler {
            showBrandedCamera = false
            onImmersiveChanged(false)
        }
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
            CameraPreview(
                imageCapture = imageCapture,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    errorMessage = it.localizedMessage
                    showBrandedCamera = false
                    onImmersiveChanged(false)
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showBrandedCamera = false; onImmersiveChanged(false) }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    stringResource(com.vergipro.mobile.R.string.capture_camera_title),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(0.72f)
                    .align(Alignment.Center)
                    .border(1.5.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(24.dp)),
            )
            Text(
                stringResource(com.vergipro.mobile.R.string.capture_camera_hint),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 126.dp),
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(108.dp).align(Alignment.BottomCenter).navigationBarsPadding().background(Color.Black.copy(alpha = 0.44f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    onClick = {
                        val output = File.createTempFile("vergipro_scan_", ".jpg", context.cacheDir)
                        val options = ImageCapture.OutputFileOptions.Builder(output).build()
                        imageCapture.takePicture(options, captureExecutor(context), object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                persistUris(listOf(Uri.fromFile(output)))
                                showBrandedCamera = false
                                onImmersiveChanged(false)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                errorMessage = exception.localizedMessage
                            }
                        })
                    },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(76.dp),
                    border = androidx.compose.foundation.BorderStroke(5.dp, Color.White.copy(alpha = 0.35f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(shape = CircleShape, color = Color.Black, modifier = Modifier.size(58.dp)) {}
                    }
                }
            }
        }
        return
    }

    // Success Screen (Single or Multiple)
    if (uploadSuccessDoc != null || uploadSuccessDocs.isNotEmpty()) {
        val docs = if (uploadSuccessDoc != null) listOf(uploadSuccessDoc!!) else uploadSuccessDocs
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VPColor.Success.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VPColor.Success.copy(alpha = 0.28f)),
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF059669), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (docs.size > 1) stringResource(com.vergipro.mobile.R.string.capture_documents_processed, docs.size) else stringResource(com.vergipro.mobile.R.string.capture_document_processed),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = VPColor.Success
                        )
                        Text(stringResource(com.vergipro.mobile.R.string.capture_analysis_complete), style = MaterialTheme.typography.bodySmall, color = VPColor.Success)
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(com.vergipro.mobile.R.string.capture_processed_documents, docs.size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        docs.forEachIndexed { index, doc ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(doc.supplierName.ifBlank { stringResource(com.vergipro.mobile.R.string.capture_supplier) }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(doc.totalAmount.formattedTRY(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(com.vergipro.mobile.R.string.capture_invoice, doc.invoiceNo.ifBlank { "-" }), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(stringResource(com.vergipro.mobile.R.string.capture_vat, doc.totalKdv.formattedTRY()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (index < docs.size - 1) {
                                Spacer(Modifier.height(1.dp).fillMaxWidth().background(VPColor.CardBorder))
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        clearDraft()
                        uploadSuccessDoc = null
                        uploadSuccessDocs = emptyList()
                        description = ""
                        plate = ""
                        isHarici = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(com.vergipro.mobile.R.string.capture_upload_another), fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // Keep the first-use experience aligned with iOS: scanning choices are the
    // only primary actions until at least one protected draft page exists.
    if (selectedPages.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(com.vergipro.mobile.R.string.capture_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(60.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VPColor.CardBorder),
                modifier = Modifier.size(120.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(com.vergipro.mobile.R.string.capture_choose_file),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(com.vergipro.mobile.R.string.capture_choose_file_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            errorMessage?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Button(
                onClick = ::openBrandedCamera,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(com.vergipro.mobile.R.string.capture_camera), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { multipleImagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(com.vergipro.mobile.R.string.capture_choose_multiple), fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(com.vergipro.mobile.R.string.capture_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(com.vergipro.mobile.R.string.capture_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Image Selection Area
        item {
            if (selectedPages.size == 1) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(com.vergipro.mobile.R.string.capture_selected_preview), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = { multipleImagePickerLauncher.launch("image/*") }) {
                                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = stringResource(com.vergipro.mobile.R.string.capture_add_more))
                                }
                                IconButton(onClick = { clearDraft() }) {
                                    Icon(Icons.Outlined.Close, contentDescription = stringResource(com.vergipro.mobile.R.string.capture_remove))
                                }
                            }
                        }
                        AsyncImage(
                            model = selectedPages.first().previewBytes,
                            contentDescription = stringResource(com.vergipro.mobile.R.string.capture_document_preview),
                            modifier = Modifier.fillMaxWidth().height(220.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(com.vergipro.mobile.R.string.capture_selected_documents, selectedPages.size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { clearDraft() }) {
                                Text(stringResource(com.vergipro.mobile.R.string.capture_clear_all), color = Color(0xFFDC2626), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            itemsIndexed(selectedPages, key = { _, page -> page.id }) { index, page ->
                                Box(modifier = Modifier.size(100.dp)) {
                                    AsyncImage(
                                        model = page.previewBytes,
                                        contentDescription = stringResource(com.vergipro.mobile.R.string.capture_receipt_number, index + 1),
                                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).border(1.dp, VPColor.CardBorder, RoundedCornerShape(10.dp))
                                    )
                                    IconButton(
                                        onClick = { removePage(page) },
                                        modifier = Modifier.align(Alignment.TopEnd).size(26.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
                                    ) {
                                        Icon(Icons.Outlined.Close, contentDescription = stringResource(com.vergipro.mobile.R.string.delete), tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { multipleImagePickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(100.dp)
                                ) {
                                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = stringResource(com.vergipro.mobile.R.string.add))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Document Type Selection
        item {
            Text(stringResource(com.vergipro.mobile.R.string.capture_document_type), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DocTypeOption.entries.forEach { option ->
                    Card(
                        onClick = { selectedDocType = option },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedDocType == option) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth().border(
                            width = if (selectedDocType == option) 2.dp else 1.dp,
                            color = if (selectedDocType == option) MaterialTheme.colorScheme.primary else VPColor.CardBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = when (option) {
                                    DocTypeOption.EXPENSE -> Icons.Outlined.TrendingDown
                                    DocTypeOption.FUEL -> Icons.Outlined.LocalGasStation
                                    DocTypeOption.RECEIPT -> Icons.Outlined.Receipt
                                    DocTypeOption.SALES -> Icons.Outlined.TrendingUp
                                },
                                contentDescription = null,
                                tint = if (selectedDocType == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(option.titleRes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(option.descRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Additional Fields
        if (selectedDocType == DocTypeOption.FUEL) {
            item {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text(stringResource(com.vergipro.mobile.R.string.capture_vehicle_plate)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(com.vergipro.mobile.R.string.capture_description)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
        }

        if (selectedDocType == DocTypeOption.RECEIPT) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isHarici, onCheckedChange = { isHarici = it })
                    Text(stringResource(com.vergipro.mobile.R.string.capture_external_expense), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isDraftProtectedAfterFailure) VPColor.Warning.copy(alpha = 0.10f) else VPColor.Danger.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(errorMessage!!, color = if (isDraftProtectedAfterFailure) VPColor.Warning else VPColor.Danger, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        if (isDraftProtectedAfterFailure) {
                            Text(stringResource(com.vergipro.mobile.R.string.capture_retry_protected_detail), color = VPColor.Warning, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    if (selectedPages.isEmpty()) {
                        errorMessage = context.getString(com.vergipro.mobile.R.string.capture_select_at_least_one)
                        return@Button
                    }
                    isUploading = true
                    uploadStatusText = if (selectedPages.size > 1) {
                        context.getString(com.vergipro.mobile.R.string.capture_uploading_multiple, selectedPages.size)
                    } else {
                        context.getString(com.vergipro.mobile.R.string.capture_uploading_single)
                    }
                    errorMessage = null
                    isDraftProtectedAfterFailure = false

                    scope.launch {
                        try {
                            if (selectedPages.size == 1) {
                                val page = selectedPages.first()
                                val fileBytes = withContext(Dispatchers.IO) { vault.read(page.vaultDocument) }
                                val filename = page.vaultDocument.originalFileName

                                onUploadDocument(fileBytes, filename, selectedDocType.code, description, plate, isHarici) { result ->
                                    isUploading = false
                                    result.onSuccess { document ->
                                        uploadSuccessDoc = document
                                        clearDraft()
                                    }.onFailure { failure ->
                                        isDraftProtectedAfterFailure = true
                                        errorMessage = context.getString(if (failure is ApiFailure.NetworkUnavailable) com.vergipro.mobile.R.string.capture_offline_retry else com.vergipro.mobile.R.string.capture_processing_failed)
                                    }
                                }
                            } else {
                                val batchPayload = withContext(Dispatchers.IO) {
                                    selectedPages.map { page ->
                                        Pair(vault.read(page.vaultDocument), page.vaultDocument.originalFileName)
                                    }
                                }

                                onUploadBatch(batchPayload, selectedDocType.code, description, plate, isHarici) { result ->
                                    isUploading = false
                                    result.onSuccess { results ->
                                        if (results.isNotEmpty()) {
                                            uploadSuccessDocs = results
                                            clearDraft()
                                        } else {
                                            isDraftProtectedAfterFailure = true
                                            errorMessage = context.getString(com.vergipro.mobile.R.string.capture_batch_failed)
                                        }
                                    }.onFailure { failure ->
                                        isDraftProtectedAfterFailure = true
                                        errorMessage = context.getString(if (failure is ApiFailure.NetworkUnavailable) com.vergipro.mobile.R.string.capture_offline_retry else com.vergipro.mobile.R.string.capture_batch_failed)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            isUploading = false
                            errorMessage = e.localizedMessage ?: context.getString(com.vergipro.mobile.R.string.capture_read_failed)
                        }
                    }
                },
                enabled = !isUploading && selectedPages.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(uploadStatusText, fontSize = 13.sp)
                } else {
                    Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedPages.size > 1) stringResource(com.vergipro.mobile.R.string.capture_upload_multiple, selectedPages.size) else stringResource(com.vergipro.mobile.R.string.capture_upload_single),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
