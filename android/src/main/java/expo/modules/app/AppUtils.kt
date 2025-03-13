package expo.modules.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Base64
import java.io.ByteArrayOutputStream

internal object AppUtils {
    /**
     * 将Drawable转换为128x128的Base64编码PNG图片
     * @param quality 图片压缩质量(0-100)
     */
    fun drawableToBase64(
        drawable: Drawable,
        size: Int = 128,
        quality: Int = 85
    ): String {
        return try {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).apply {
                drawable.setBounds(0, 0, width, height)
                drawable.draw(this)
            }
            ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, quality, stream)
                Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
            }
        } catch (e: Exception) {
            ""
        }
    }
}