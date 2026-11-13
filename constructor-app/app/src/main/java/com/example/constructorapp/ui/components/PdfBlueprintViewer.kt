package com.example.constructorapp.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.constructorapp.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun ModernPdfBlueprintViewerDialog(
    title: String,
    urlOrPath: String,
    category: String = "Blueprint",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val isPdf = urlOrPath.contains(".pdf", ignoreCase = true) || urlOrPath.contains("pdf", ignoreCase = true)
    val isImage = urlOrPath.contains(".jpg", ignoreCase = true) ||
            urlOrPath.contains(".jpeg", ignoreCase = true) ||
            urlOrPath.contains(".png", ignoreCase = true) ||
            urlOrPath.contains(".webp", ignoreCase = true) ||
            urlOrPath.contains("image", ignoreCase = true)

    // Gesture Transformation State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    fun renderPage(renderer: PdfRenderer, index: Int) {
        if (index !in 0 until renderer.pageCount) return
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val page = renderer.openPage(index)
                // High-resolution bitmap scaling (2.5x density for ultra-sharp blueprint vector details)
                val density = 2.5f
                val bmpWidth = (page.width * density).roundToInt()
                val bmpHeight = (page.height * density).roundToInt()
                val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                withContext(Dispatchers.Main) {
                    pageBitmap = bitmap
                    currentPageIndex = index
                    // Reset zoom on page turn
                    scale = 1f
                    offset = Offset.Zero
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error rendering page: ${e.localizedMessage}"
                }
            }
        }
    }

    LaunchedEffect(urlOrPath) {
        isLoading = true
        errorMessage = null

        if (!isPdf && isImage) {
            // Direct image rendering, no PdfRenderer needed
            isLoading = false
            return@LaunchedEffect
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val file: File = if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
                    // Download to cache file
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val req = Request.Builder().url(urlOrPath).build()
                    val resp = client.newCall(req).execute()
                    if (!resp.isSuccessful) {
                        throw Exception("HTTP ${resp.code}: ${resp.message}")
                    }

                    val cacheFile = File(context.cacheDir, "temp_blueprint_${System.currentTimeMillis()}.pdf")
                    val fos = FileOutputStream(cacheFile)
                    resp.body?.byteStream()?.use { input ->
                        input.copyTo(fos)
                    }
                    fos.close()
                    cacheFile
                } else if (urlOrPath.startsWith("content://")) {
                    val uri = Uri.parse(urlOrPath)
                    val cacheFile = File(context.cacheDir, "temp_content_${System.currentTimeMillis()}.pdf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val fos = FileOutputStream(cacheFile)
                        input.copyTo(fos)
                        fos.close()
                    } ?: throw Exception("Cannot open content URI")
                    cacheFile
                } else {
                    File(urlOrPath)
                }

                if (!file.exists() || file.length() == 0L) {
                    throw Exception("Document file is empty or unreachable")
                }

                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)

                withContext(Dispatchers.Main) {
                    pfd = descriptor
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                    isLoading = false
                    renderPage(renderer, 0)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = e.message ?: "Failed to open PDF document"
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6090D16)) // Translucent deep background for immersive viewer
        ) {
            // Ambient Drifting Blobs for subtle visual depth
            AmbientBlobBackground(modifier = Modifier.fillMaxSize())

            // ─── Document Canvas (Pinch to Zoom, Pan, Rotate) ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                    rotationAngle = 0f
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.7f, 6.0f)
                            offset = Offset(
                                x = offset.x + pan.x,
                                y = offset.y + pan.y
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = BrandPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Loading Blueprint...",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    errorMessage != null -> {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureAsPdf,
                                    contentDescription = null,
                                    tint = RoseDanger,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Unable to Render In-App",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = errorMessage ?: "The file could not be parsed as a PDF.",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FluidGlossButton(
                                        text = "Open Externally",
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse(urlOrPath)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No app found to open document", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        variant = FluidGlossButtonVariant.Primary,
                                        size = FluidGlossButtonSize.Small
                                    )
                                    FluidGlossButton(
                                        text = "Close",
                                        onClick = onDismiss,
                                        variant = FluidGlossButtonVariant.Secondary,
                                        size = FluidGlossButtonSize.Small
                                    )
                                }
                            }
                        }
                    }

                    pageBitmap != null -> {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxSize(0.92f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = rotationAngle
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                            contentScale = ContentScale.Fit
                        )
                    }

                    isImage -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(urlOrPath)
                                .crossfade(true)
                                .build(),
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxSize(0.92f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = rotationAngle
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // ─── Floating Top Bar (Title, Category, Close) ────────────────────────
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandPrimary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = category.uppercase(),
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (pageCount > 1) {
                                Text(
                                    text = "Page ${currentPageIndex + 1} of $pageCount",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            fontFamily = OutfitFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(urlOrPath)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open externally", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open in external app", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ─── Floating Bottom Control Dock (Zoom, Rotate, Navigation) ─────────
            GlassCard(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 16.dp, vertical = 28.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Zoom Out
                    IconButton(
                        onClick = { scale = (scale - 0.35f).coerceAtLeast(0.7f) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom Out", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }

                    // Zoom Percent Badge
                    Text(
                        text = "${(scale * 100).roundToInt()}%",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.widthIn(min = 40.dp),
                        textAlign = TextAlign.Center
                    )

                    // Zoom In
                    IconButton(
                        onClick = { scale = (scale + 0.35f).coerceAtMost(6.0f) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom In", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(SlateDivider))

                    // Rotate 90 Degrees Clockwise
                    IconButton(
                        onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Outlined.RotateRight, contentDescription = "Rotate 90°", tint = TextPrimary, modifier = Modifier.size(19.dp))
                    }

                    // Reset View / Center
                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                            rotationAngle = 0f
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset Zoom", tint = TextPrimary, modifier = Modifier.size(19.dp))
                    }

                    // Page Navigation (if multi-page PDF)
                    if (pageCount > 1 && pdfRenderer != null) {
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(SlateDivider))

                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    renderPage(pdfRenderer!!, currentPageIndex - 1)
                                }
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Previous Page",
                                tint = if (currentPageIndex > 0) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentPageIndex < pageCount - 1) {
                                    renderPage(pdfRenderer!!, currentPageIndex + 1)
                                }
                            },
                            enabled = currentPageIndex < pageCount - 1,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "Next Page",
                                tint = if (currentPageIndex < pageCount - 1) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
