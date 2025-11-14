package com.mlbbspelltracker

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private val handler = Handler()

    private val spells = listOf(
        "Execute", "Retribution", "Inspire", "Sprint",
        "Revitalize", "Aegis", "Petrify", "Purify",
        "Flameshot", "Flicker", "Arrival", "Vengeance"
    )

    private val cooldowns = mapOf(
        "Execute" to 90, "Retribution" to 35, "Inspire" to 75,
        "Sprint" to 100, "Revitalize" to 75, "Aegis" to 75,
        "Petrify" to 75, "Purify" to 90, "Flameshot" to 50,
        "Flicker" to 120, "Arrival" to 75, "Vengeance" to 75
    )

    private val spellIcons = mapOf(
        "Execute" to R.drawable.execute,
        "Retribution" to R.drawable.retribution,
        "Inspire" to R.drawable.inspire,
        "Sprint" to R.drawable.sprint,
        "Revitalize" to R.drawable.revitalize,
        "Aegis" to R.drawable.aegis,
        "Petrify" to R.drawable.petrify,
        "Purify" to R.drawable.purify,
        "Flameshot" to R.drawable.flameshot,
        "Flicker" to R.drawable.flicker,
        "Arrival" to R.drawable.arrival,
        "Vengeance" to R.drawable.vengeance
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = android.view.Gravity.TOP or android.view.Gravity.END
        params.x = 50
        params.y = 200
        windowManager.addView(overlayView, params)

        // Draggable with snap-to-edge
        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val centerX = event.rawX
                        params.gravity = if (centerX < screenWidth / 2)
                            android.view.Gravity.TOP or android.view.Gravity.START
                        else
                            android.view.Gravity.TOP or android.view.Gravity.END
                        params.x = 0
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        val toggleButton = overlayView.findViewById<ImageButton>(R.id.minimizeButton)
        val lanesContainer = overlayView.findViewById<View>(R.id.lanesContainer)

        // ✅ Toggle between minimize (X) and maximize (circle)
        toggleButton.setOnClickListener {
            if (lanesContainer.visibility == View.VISIBLE) {
                lanesContainer.visibility = View.GONE
                toggleButton.setImageResource(android.R.drawable.radiobutton_off_background) // circle
                toggleButton.contentDescription = "Maximize Overlay"
            } else {
                lanesContainer.visibility = View.VISIBLE
                toggleButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel) // X
                toggleButton.contentDescription = "Minimize Overlay"
            }
        }

        // Attach roles
        setupRole(R.id.expSpinner, R.id.expButton, R.id.expCooldown)
        setupRole(R.id.midSpinner, R.id.midButton, R.id.midCooldown)
        setupRole(R.id.roamerSpinner, R.id.roamerButton, R.id.roamerCooldown)
        setupRole(R.id.junglerSpinner, R.id.junglerButton, R.id.junglerCooldown)
        setupRole(R.id.goldSpinner, R.id.goldButton, R.id.goldCooldown)
    }

    private fun setupRole(spinnerId: Int, buttonId: Int, cooldownId: Int) {
        val spinner = overlayView.findViewById<Spinner>(spinnerId)
        val button = overlayView.findViewById<ImageButton>(buttonId)
        val cooldownView = overlayView.findViewById<TextView>(cooldownId)

        val adapter = ArrayAdapter(this, R.layout.spinner_item, spells)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        var selectedSpell: String? = null

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSpell = spells[position]
                cooldownView.text = "Ready"
                cooldownView.setTextColor(Color.GREEN) // ✅ Ready is green
                spellIcons[selectedSpell]?.let { button.setImageResource(it) }
                button.alpha = 1.0f
                (view as? TextView)?.setTextColor(Color.parseColor("#2196F3")) // selected item text blue
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        button.setOnClickListener {
            selectedSpell?.let {
                startCooldown(it, cooldownView, button)
            }
        }
    }

    private fun startCooldown(spell: String, cooldownView: TextView, button: ImageButton) {
        val duration = cooldowns[spell] ?: return
        handler.post { button.alpha = 0.5f }

        Thread {
            for (i in duration downTo 1) {
                Thread.sleep(1000)
                handler.post {
                    cooldownView.text = "$i s"
                    cooldownView.setTextColor(Color.YELLOW)
                }
            }
            handler.post {
                cooldownView.text = "Ready"
                cooldownView.setTextColor(Color.GREEN) // ✅ Ready is green
                button.alpha = 1.0f
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(overlayView)
    }
}
