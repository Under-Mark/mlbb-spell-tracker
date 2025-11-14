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

    // Full MLBB Battle Spells
    private val spells = listOf(
        "Execute", "Retribution", "Inspire", "Sprint",
        "Revitalize", "Aegis", "Petrify", "Purify",
        "Flameshot", "Flicker", "Arrival", "Vengeance"
    )

    // Cooldown durations (seconds)
    private val cooldowns = mapOf(
        "Execute" to 90, "Retribution" to 35, "Inspire" to 75,
        "Sprint" to 100, "Revitalize" to 75, "Aegis" to 75,
        "Petrify" to 75, "Purify" to 90, "Flameshot" to 50,
        "Flicker" to 120, "Arrival" to 75, "Vengeance" to 75
    )

    // Spell icons (drawable resources)
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

        // ✅ Start anchored to the right side
        params.gravity = android.view.Gravity.TOP or android.view.Gravity.END
        params.x = 50
        params.y = 200

        windowManager.addView(overlayView, params)

        // ✅ Make overlay draggable with snap-to-edge
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

                        // Snap to nearest edge
                        if (centerX < screenWidth / 2) {
                            // Snap left
                            params.gravity = android.view.Gravity.TOP or android.view.Gravity.START
                            params.x = 0
                        } else {
                            // Snap right
                            params.gravity = android.view.Gravity.TOP or android.view.Gravity.END
                            params.x = 0
                        }
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        // ✅ Minimize button logic
        val minimizeButton = overlayView.findViewById<ImageButton>(R.id.minimizeButton)
        val lanesContainer = overlayView.findViewById<View>(R.id.lanesContainer)

        minimizeButton.setOnClickListener {
            lanesContainer.visibility =
                if (lanesContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Attach roles
        setupRole(R.id.expSpinner, R.id.expButton, R.id.expCooldown, "EXP Lane")
        setupRole(R.id.midSpinner, R.id.midButton, R.id.midCooldown, "Mid Lane")
        setupRole(R.id.roamerSpinner, R.id.roamerButton, R.id.roamerCooldown, "Roamer")
        setupRole(R.id.junglerSpinner, R.id.junglerButton, R.id.junglerCooldown, "Jungler")
        setupRole(R.id.goldSpinner, R.id.goldButton, R.id.goldCooldown, "Gold Lane")
    }

    private fun setupRole(spinnerId: Int, buttonId: Int, cooldownId: Int, role: String) {
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
                cooldownView.setTextColor(Color.GREEN)
                spellIcons[selectedSpell]?.let { button.setImageResource(it) }
                button.alpha = 1.0f
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        button.setOnClickListener {
            selectedSpell?.let {
                startCooldown(role, it, cooldownView, button)
            }
        }
    }

    private fun startCooldown(role: String, spell: String, cooldownView: TextView, button: ImageButton) {
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
                cooldownView.setTextColor(Color.GREEN)
                button.alpha = 1.0f
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(overlayView)
    }
}
