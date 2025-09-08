package com.imangazaliev.circlemenu.sample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.view.*
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

class FloatingMenuService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startInForeground()
        addOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service is sticky to keep overlay alive until explicitly stopped
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val channelId = "floating_menu"
        val channelName = "Floating Menu"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }

        val tapIntent = Intent(this, SimpleMenuActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_more)
            .setContentTitle("CircleMenu flotante activo")
            .setContentText("Toca para abrir la app")
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            startForeground(1001, notification, type)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun addOverlay() {
        if (overlayView != null) return

        val themedContext = ContextThemeWrapper(this, R.style.AppTheme)
        val inflater = LayoutInflater.from(themedContext)
        val view = inflater.inflate(R.layout.overlay_circle_menu, null) as FrameLayout

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = (20 * resources.displayMetrics.density).toInt()
        params.y = (200 * resources.displayMetrics.density).toInt()

        val circle = view.findViewById<com.imangazaliev.circlemenu.CircleMenu>(R.id.floatingMenu)

        // Arrastre moviendo toda la ventana (no los márgenes internos)
        view.setOnTouchListener(null)
        circle.setOnTouchListener(object : View.OnTouchListener {
            private var initialWinX = 0
            private var initialWinY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var dragging = false
            private val touchSlop = ViewConfiguration.get(this@FloatingMenuService).scaledTouchSlop

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dragging = false
                        initialWinX = params.x
                        initialWinY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        // return true to start tracking; we'll synthesize click on UP if not dragged
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (!dragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                            dragging = true
                        }
                        if (dragging) {
                            params.x = initialWinX + dx
                            params.y = initialWinY + dy
                            windowManager.updateViewLayout(view, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!dragging) {
                            v.performClick()
                        }
                        // Ensure window recomputes size if content changed
                        windowManager.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })

        // Mantén la ventana ligera; no forces updates durante la animación para preservar fluidez

        // Evita duplicados si por alguna razón quedó un overlay previo
        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (_: IllegalStateException) {
            removeOverlay()
            windowManager.addView(view, params)
            overlayView = view
        }
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        } finally {
            overlayView = null
        }
    }
}
