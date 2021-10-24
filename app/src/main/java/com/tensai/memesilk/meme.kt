package com.tensai.memesilk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs


class meme : AppCompatActivity(), SensorEventListener {

    // Views
    private lateinit var image: ImageView

    // Shake Sensor
    private lateinit var sensorManager: SensorManager
    private var SHAKE_THRESHOLD: Int = 800
    private var lastUpdate: Long = 0
    private var lastX: Float = 0.0f
    private var lastY: Float = 0.0f
    private var lastZ: Float = 0.0f

    // Api Calls
    private val client = OkHttpClient()
    private var apiURL: String = "https://meme-api.herokuapp.com/gimme/animememe"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.meme)
        val actionBar = supportActionBar
        actionBar!!.hide()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!.hide(
                android.view.WindowInsets.Type.statusBars()
            )
        } else{
            val decorView = window.decorView
            val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
            decorView.systemUiVisibility = uiOptions
        }

        apiCall("https://meme-api.herokuapp.com/gimme/animememe")
        image = findViewById(R.id.imageView)
        Picasso.with(this).load(apiURL).fetch()

        setUpSensorStuffs()
    }

    private fun setUpSensorStuffs() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.also {
            sensorManager.registerListener(this, it,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        Handler(Looper.myLooper()!!).postDelayed({
            if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                val curTime: Long = System.currentTimeMillis()
                if (curTime - lastUpdate > 100) {
                    val diffTime = curTime - lastUpdate
                    lastUpdate = curTime

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val speed: Float = abs(x+y+z - lastX - lastY - lastZ) / diffTime * 10000

                    if (speed > SHAKE_THRESHOLD && speed < SHAKE_THRESHOLD*3 ) {
                        vibratePhone()
                        changeImage()
                    }
                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
        }, 5000)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { return }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    private fun changeImage() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this.display
                    else windowManager.defaultDisplay

        val size = Point()
        display?.getSize(size)

        val width: Int = size.x
        val height: Int = size.y

        apiCall("https://meme-api.herokuapp.com/gimme/animememe")
        Picasso.with(this).load(apiURL).skipMemoryCache().
            resize(width*3/4, height*3/4).centerInside().into(image)
    }

    private fun apiCall(url: String) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Toast.makeText(applicationContext, "$e", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val data = response.body()?.string()?.split("\"")
                apiURL = data?.get(15).toString()
            }
        })
    }

    private fun vibratePhone() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager: VibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
            } else getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun imageDownload(ctx: Context?) {
        Picasso.with(ctx)
            .load("http://blog.concretesolutions.com.br/wp-content/uploads/2015/04/Android1.png")
            .into(getTarget("http://blog.concretesolutions.com.br/wp-content/uploads/2015/04/Android1.png"))
    }


    private fun getTarget(url: String): Target? {
        return object : Target() {
            fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom?) {
                Thread {
                    val file =
                        File(Intent.ACTION_OPEN_DOCUMENT + "/" + url)
                    try {
                        file.createNewFile()
                        val stream = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        stream.flush()
                        stream.close()
                    } catch (e: IOException) {
                        Log.e("IOException", "${e.localizedMessage} nice")
                    }
                }.start()
            }

            fun onBitmapFailed(errorDrawable: Drawable?) {}
            fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        }
    }
}

