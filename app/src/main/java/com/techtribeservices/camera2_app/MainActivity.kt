package com.techtribeservices.camera2_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.media.MediaMetadataRetriever.BitmapParams
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.techtribeservices.camera2_app.ml.AutoModel1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    val paint = Paint()
    lateinit var imageView: ImageView
    lateinit var button: Button
    lateinit var bitmap: Bitmap
    lateinit var model: AutoModel1
    lateinit var labels: List<String>
    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(300,300,ResizeOp.ResizeMethod.BILINEAR))
        .build()

    var colors = listOf<Int>(
        Color.BLUE,Color.GREEN, Color.RED, Color.CYAN, Color.GRAY,
        Color.BLACK, Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)

        labels = FileUtil.loadLabels(this, "labels.txt")
        model = AutoModel1.newInstance(this)
        imageView = findViewById(R.id.imageV)
        button = findViewById(R.id.btn)

        button.setOnClickListener {
            startActivityForResult(intent, 101)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 101) {
            var uri = data?.data
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,uri)

            get_prediction()
        }
    }

    fun get_prediction() {

// Creates inputs for reference.
        var image = TensorImage.fromBitmap(bitmap)
        image = imageProcessor.process(image)

// Runs model inference and gets result.
        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

        var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)

        val h = mutable.height
        val w = mutable.width
        paint.textSize = h/35f
        paint.strokeWidth = h/85f
        var x = 0
        scores.forEachIndexed{index, fl ->
            x = index
            x *= 4

            if(fl > 0.5) {
                paint.setColor(colors.get(index))
                paint.style = Paint.Style.STROKE
                canvas.drawRect(RectF(
                    locations.get(x+1)*w,
                    locations.get(x)*h,
                    locations.get(x+3)*w,
                    locations.get(x+2)*h),
                    paint)
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    labels.get(classes.get(index).toInt()) + " " + fl.toString() ,
                    locations.get(x+1)*w,
                    locations.get(x)*h,
                    paint)
            }
        }
        imageView.setImageBitmap(mutable)
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
}