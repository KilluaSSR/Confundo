package killua.dev.confundo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_CACHE_SIZE = 64
    }

    private val cache = LruCache<String, ImageBitmap>(MAX_CACHE_SIZE)

    suspend fun getIcon(packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(48)
                val imageBitmap = bitmap.asImageBitmap()
                cache.put(packageName, imageBitmap)
                imageBitmap
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun Drawable.toBitmap(sizeDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        if (this is AdaptiveIconDrawable) {
            val bitmap = createBitmap(sizePx, sizePx)
            val canvas = Canvas(bitmap)
            setBounds(0, 0, sizePx, sizePx)
            draw(canvas)
            return bitmap
        }
        if (this is BitmapDrawable) {
            val bmp = bitmap
            if (bmp != null) return bmp.scale(sizePx, sizePx)
        }
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, sizePx, sizePx)
        draw(canvas)
        return bitmap
    }
}
