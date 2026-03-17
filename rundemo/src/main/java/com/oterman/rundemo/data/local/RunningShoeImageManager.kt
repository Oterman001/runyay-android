package com.oterman.rundemo.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class RunningShoeImageManager(private val context: Context) {

    private val shoeImageDir: File
        get() = File(context.filesDir, "RunningShoes").also { it.mkdirs() }

    fun saveImage(shoeId: String, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val file = File(shoeImageDir, "${shoeId}.jpg")
            compressAndSave(bitmap, file)
            bitmap.recycle()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun loadImage(shoeId: String): Bitmap? {
        val file = File(shoeImageDir, "${shoeId}.jpg")
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun deleteImage(shoeId: String): Boolean {
        val file = File(shoeImageDir, "${shoeId}.jpg")
        return file.delete()
    }

    fun getImagePath(shoeId: String): String? {
        val file = File(shoeImageDir, "${shoeId}.jpg")
        return if (file.exists()) file.absolutePath else null
    }

    private fun compressAndSave(bitmap: Bitmap, file: File) {
        // Scale down if too large
        val maxDimension = 1024
        val scaled = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = minOf(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height
            )
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else bitmap

        // Compress to under 300KB
        var quality = 65
        var output: FileOutputStream
        do {
            output = FileOutputStream(file)
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.flush()
            output.close()
            quality -= 10
        } while (file.length() > 300 * 1024 && quality > 10)

        if (scaled !== bitmap) scaled.recycle()
    }
}
