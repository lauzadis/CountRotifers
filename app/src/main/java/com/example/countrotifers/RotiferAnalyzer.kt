package com.example.countrotifers

import android.graphics.*
import android.media.Image
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils.bitmapToMat
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

class RotiferAnalyzer(private val listener: (rotifers: Int) -> Unit) : ImageAnalysis.Analyzer {
    fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining();
        val uSize = uBuffer.remaining();
        val vSize = vBuffer.remaining();

        val nv21 = ByteArray(ySize + uSize + vSize)
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null);
        val out = ByteArrayOutputStream();
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out);

        val imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val bitmap = image.image?.toBitmap()
        image.close()

        val mat = Mat()
        bitmapToMat(bitmap, mat)

        Log.d(TAG, mat.toString())

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(9.0, 9.0), 2.0, 2.0)

        val circles = Mat()
        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1.0,
            (gray.rows()/16).toDouble(), 100.0, 30.0, 1, 30 )

        Log.d(TAG, "Circles: $circles")
        Log.d(TAG, "Circles height: ${circles.size().height}")
        Log.d(TAG, "Circles width: ${circles.size().width}")

        listener(circles.size().width.toInt())
    }

    companion object {
        const val TAG = "RotiferAnalyzer"
    }
}