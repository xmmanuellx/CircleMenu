package com.imangazaliev.circlemenu.sample

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.imangazaliev.circlemenu.sample.databinding.ActivitySimpleMenuBinding

class SimpleMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySimpleMenuBinding

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                maybeRequestPostNotificationsAndStart()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Auto-solicitar permiso de superposición y arrancar el menú flotante
        if (!Settings.canDrawOverlays(this)) {
            // Intenta abrir la pantalla específica de la app; en algunos OEMs abrirá la lista general
            val appIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                overlayPermissionLauncher.launch(appIntent)
            } catch (_: Exception) {
                // Fallback: pantalla general de superposición
                val generalIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                overlayPermissionLauncher.launch(generalIntent)
            }
        } else {
            maybeRequestPostNotificationsAndStart()
        }
    }

    private fun maybeRequestPostNotificationsAndStart() {
        if (Build.VERSION.SDK_INT >= 33) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
                // onRequestPermissionsResult -> volverá aquí. Iniciar después.
                return
            }
        }
        ContextCompat.startForegroundService(this, Intent(this, FloatingMenuService::class.java))
        // Cierra la Activity para evitar superponer su propio contenido con el overlay
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            // Intenta iniciar el servicio de todas formas (si se concedió, mostrará notificación)
            ContextCompat.startForegroundService(this, Intent(this, FloatingMenuService::class.java))
            finish()
        }
    }
}
