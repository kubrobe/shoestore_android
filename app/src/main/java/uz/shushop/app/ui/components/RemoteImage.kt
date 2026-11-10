package uz.shushop.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a shoe photo (a link the shop owner pasted in the admin page). Small on purpose: download, shrink, keep the
 * most recent ones in memory. If the photo cannot be loaded the app simply draws the shoe instead.
 */
object ImageLoader {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val failed = HashSet<String>()

    suspend fun load(url: String, maxSide: Int = 900): Bitmap? {
        cache.get(url)?.let { return it }
        if (url in failed || !(url.startsWith("https://") || url.startsWith("http://"))) return null
        val bitmap = withContext(Dispatchers.IO) { download(url, maxSide) }
        if (bitmap != null) cache.put(url, bitmap) else failed.add(url)
        return bitmap
    }

    private fun download(url: String, maxSide: Int): Bitmap? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            if (conn.responseCode !in 200..299) return null
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.size > 8_000_000) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * The picture of a shoe: the owner's photo when there is one and it loads, otherwise the drawn shoe in [colorHex].
 */
@Composable
fun ShoePicture(artStyle: String, imageUrl: String?, colorHex: String?, seed: Long, modifier: Modifier = Modifier) {
    val photo by produceState<Bitmap?>(initialValue = null, imageUrl) {
        value = if (imageUrl.isNullOrBlank()) null else ImageLoader.load(imageUrl)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val bitmap = photo
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.matchParentSize())
        } else {
            ShoeArt(artStyle, colorHex, seed, Modifier.matchParentSize())
        }
    }
}
