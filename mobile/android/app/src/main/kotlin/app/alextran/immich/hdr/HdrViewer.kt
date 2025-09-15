package app.alextran.immich.hdr

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.RequiresApi
import io.flutter.plugin.platform.PlatformView
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Native Android HDR viewer that displays HDR images using Android's native HDR support.
 * This viewer is designed to work with Android 14+ (API 34+) devices that support Ultra HDR.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class HdrViewer(
    private val context: Context,
    private val viewId: Int,
    private val creationParams: Map<String?, Any?>?
) : PlatformView {

    private val imageView: ImageView
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    init {
        Log.d("HdrViewer", "init: $creationParams")
        imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            adjustViewBounds = true
        }
        
        // Load the image if provided
        val imagePath = creationParams?.get("imagePath") as? String
        if (imagePath != null) {
            loadHdrImage(imagePath)
        }
    }

    override fun getView(): View = imageView

    override fun dispose() {
        // Clean up resources if needed
    }

    /**
     * Loads an HDR image from the specified path.
     * Supports Ultra HDR (JPEG) and other HDR formats.
     */
    private fun loadHdrImage(imagePath: String) {
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                Log.e("HdrViewer", "Image file does not exist: $imagePath")
                return
            }

            // Try to load as Ultra HDR first (Android 14+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                loadUltraHdrImage(file)
            } else {
                // Fallback to regular bitmap loading
                loadRegularImage(file)
            }
        } catch (e: Exception) {
            Log.e("HdrViewer", "Error loading HDR image: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun loadUltraHdrImage(file: File) {
        try {
            Log.d("HdrViewer", "Attempting to load Ultra HDR image: ${file.absolutePath}")
            Log.d("HdrViewer", "File size: ${file.length()} bytes")
            
            // First, check if the display supports HDR
            val hdrCapable = isDisplayHdrCapable()
            Log.d("HdrViewer", "Display HDR capable: $hdrCapable")
            
            if (!hdrCapable) {
                Log.w("HdrViewer", "Display does not support HDR, falling back to regular image")
                loadRegularImage(file)
                return
            }

            // Try multiple approaches for HDR loading
            val bitmap = tryLoadHdrBitmap(file)
            
            if (bitmap != null) {
                Log.d("HdrViewer", "!!!!!!!!!!!!gainMap: ${bitmap.hasGainmap()}")

                // Set color mode of the activity to HDR - this is the key for Ultra HDR display
                setHdrColorMode()
                
                Log.d("HdrViewer", "Bitmap loaded successfully")
                Log.d("HdrViewer", "Bitmap config: ${bitmap.config}")
                Log.d("HdrViewer", "Bitmap color space: ${bitmap.colorSpace}")
                Log.d("HdrViewer", "Bitmap wide gamut: ${bitmap.colorSpace?.isWideGamut}")
                Log.d("HdrViewer", "Bitmap has alpha: ${bitmap.hasAlpha()}")
                Log.d("HdrViewer", "Bitmap is mutable: ${bitmap.isMutable}")
                
                // Simple approach following Android platform sample
                // The HDR effect comes from the window color mode, not complex ImageView configuration
                imageView.setImageBitmap(bitmap)
                
                // Log color space information
                Log.d("HdrViewer", "!!!!!!!!!!!!Bitmap color space: ${bitmap.colorSpace}")
                Log.d("HdrViewer", "!!!!!!!!!!!!Bitmap has gainmap: ${bitmap.hasGainmap()}")
                
                Log.d("HdrViewer", "!!!!!!!!!!!!ImageView configured for HDR display")
                
                Log.d("HdrViewer", "Successfully loaded Ultra HDR image")
            } else {
                Log.e("HdrViewer", "Failed to decode Ultra HDR image")
                loadRegularImage(file)
            }
        } catch (e: Exception) {
            Log.e("HdrViewer", "Error loading Ultra HDR image: ${e.message}", e)
            loadRegularImage(file)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun tryLoadHdrBitmap(file: File): Bitmap? {
        // Follow the Android platform sample approach - use simple decodeStream
        // The HDR effect comes from the window color mode, not bitmap configuration
        try {
            val inputStream = FileInputStream(file)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                Log.d("HdrViewer", "Successfully loaded Ultra HDR image")
                Log.d("HdrViewer", "Bitmap config: ${bitmap.config}")
                Log.d("HdrViewer", "Bitmap color space: ${bitmap.colorSpace}")
                Log.d("HdrViewer", "Bitmap has gainmap: ${bitmap.hasGainmap()}")
                return bitmap
            }
        } catch (e: Exception) {
            Log.w("HdrViewer", "Failed to load Ultra HDR image: ${e.message}")
        }
        
        return null
    }

    private fun loadRegularImage(file: File) {
        try {
            val inputStream = FileInputStream(file)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                Log.d("HdrViewer", "Successfully loaded regular image")
            } else {
                Log.e("HdrViewer", "Failed to decode image")
            }
        } catch (e: Exception) {
            Log.e("HdrViewer", "Error loading regular image: ${e.message}", e)
        }
    }

    /**
     * Updates the image path and reloads the image.
     * This method can be called from Flutter to change the displayed image.
     */
    fun updateImagePath(imagePath: String) {
        Log.d("HdrViewer", "Updating image path to: $imagePath")
        loadHdrImage(imagePath)
    }

    /**
     * Updates the scale type of the image view.
     */
    fun updateScaleType(scaleType: String) {
        Log.d("HdrViewer", "Updating scale type to: $scaleType")
        setScaleType(scaleType)
    }

    /**
     * Sets the scale type for the image view.
     */
    fun setScaleType(scaleType: String) {
        val type = when (scaleType.lowercase()) {
            "center" -> ImageView.ScaleType.CENTER
            "center_crop" -> ImageView.ScaleType.CENTER_CROP
            "center_inside" -> ImageView.ScaleType.CENTER_INSIDE
            "fit_center" -> ImageView.ScaleType.FIT_CENTER
            "fit_start" -> ImageView.ScaleType.FIT_START
            "fit_end" -> ImageView.ScaleType.FIT_END
            "fit_xy" -> ImageView.ScaleType.FIT_XY
            "matrix" -> ImageView.ScaleType.MATRIX
            else -> ImageView.ScaleType.CENTER_CROP
        }
        imageView.scaleType = type
    }

    /**
     * Sets the window color mode to HDR for proper Ultra HDR display.
     * This is the key method that enables the HDR effect.
     */
    private fun setHdrColorMode() {
        try {
            val activity = context as? android.app.Activity ?: 
                (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
            
            if (activity != null) {
                val currentColorMode = activity.window.colorMode
                Log.d("HdrViewer", "Current color mode: $currentColorMode")
                
                // Set the window color mode to HDR - this enables Ultra HDR gain map processing
                activity.window.colorMode = ActivityInfo.COLOR_MODE_HDR
                val newColorMode = activity.window.colorMode
                Log.d("HdrViewer", "New color mode: $newColorMode")
                
                // Enable hardware acceleration for HDR rendering
                activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
                
                Log.d("HdrViewer", "HDR color mode set successfully")
            } else {
                Log.w("HdrViewer", "Could not get activity context for HDR color mode")
            }
        } catch (e: Exception) {
            Log.w("HdrViewer", "Could not set HDR color mode: ${e.message}")
        }
    }

    /**
     * Checks if the display is capable of showing HDR content.
     */
    private fun isDisplayHdrCapable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display = windowManager.defaultDisplay
                val hdrCapabilities = display.hdrCapabilities
                if (hdrCapabilities != null) {
                    val supportedTypes = hdrCapabilities.supportedHdrTypes
                    Log.d("HdrViewer", "Supported HDR types: ${supportedTypes.contentToString()}")
                    return supportedTypes.isNotEmpty()
                }
                false
            } catch (e: Exception) {
                Log.w("HdrViewer", "Error checking HDR capabilities: ${e.message}")
                false
            }
        } else {
            false
        }
    }

    /**
     * Checks if the device supports HDR display.
     */
    companion object {
        fun isHdrSupported(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }

        /**
         * Checks if an image file is likely to be an HDR image based on its extension and content.
         */
        fun isHdrImage(imagePath: String): Boolean {
            val extension = File(imagePath).extension.lowercase()
            return extension in listOf("jpg", "jpeg", "heic", "heif", "avif", "webp") ||
                   imagePath.lowercase().contains("hdr") ||
                   imagePath.lowercase().contains("ultra")
        }
    }
}
