package com.raza.noisedetection

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import com.raza.noisedetection.NoiseMeter.checkMicrophonePermission
import com.raza.noisedetection.NoiseMeter.getAmplitude
import com.raza.noisedetection.NoiseMeter.stop
import kotlinx.android.synthetic.main.activity_main.*
import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    /* wake lock initialization*/
    private var wakeLock: PowerManager.WakeLock? = null

    /* running state  */
    private var isRunning = false

    private val mHandler = Handler()

    // Create runnable thread to Monitor Voice
    private val monitorNoiseTask = object : Runnable {
        override fun run() {
            val amp = getAmplitude()
            if (amp > THRESHOLD) {
                message.append("\nNoise Detected with amplitude $amp")
            }
            mHandler.postDelayed(this, POLL_INTERVAL.toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)

        toggle.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkMicrophonePermission(this)) {
                requestPermissions(arrayOf(RECORD_AUDIO), PERMISSION_REQUEST_CODE)
                return@setOnClickListener
            }
            if (isRunning) {
                stopDetecting()
            } else {
                startDetecting()
            }
        }
    }

    private fun stopDetecting() {
        if (wakeLock?.isHeld!!) {
            wakeLock?.release()
        }
        stop()
        isRunning = false
        message.text = ""
        toggle.text = getString(R.string.start)
        mHandler.removeCallbacks(monitorNoiseTask)
    }

    private fun startDetecting() {
        NoiseMeter.start()
        if (!wakeLock?.isHeld!!) {
            wakeLock?.acquire(10*60*1000L /*10 minutes*/)
        }
        isRunning = true
        toggle.text = getString(R.string.stop)
        mHandler.postDelayed(monitorNoiseTask, POLL_INTERVAL.toLong())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDetecting()
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
