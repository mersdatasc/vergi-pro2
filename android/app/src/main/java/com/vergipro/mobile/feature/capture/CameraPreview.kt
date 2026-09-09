package com.vergipro.mobile.feature.capture

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
    onError: (Throwable) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner, imageCapture) {
        val listener = Runnable {
            runCatching {
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            }.onFailure(onError)
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            if (cameraProviderFuture.isDone) runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }
}

fun captureExecutor(context: Context) = ContextCompat.getMainExecutor(context)

