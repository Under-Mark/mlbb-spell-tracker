package com.mlbbspelltracker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<Button>(R.id.startOverlayButton)
        val stopButton = findViewById<Button>(R.id.stopOverlayButton)

        startButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, 1234)
                } else {
                    val serviceIntent = Intent(this, OverlayService::class.java)
                    startService(serviceIntent)
                }
            }
        }

        stopButton.setOnClickListener {
            val serviceIntent = Intent(this, OverlayService::class.java)
            stopService(serviceIntent)
        }
    }
}
