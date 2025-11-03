package com.example.capturametadatos

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TakePhotoActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var imageUri: Uri? = null
    private var currentLocation: Location? = null

    // 1. Lanzador para solicitar múltiples permisos
    private val allPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isCameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val isFineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val isCoarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (isCameraGranted && isFineLocationGranted && isCoarseLocationGranted) {
                // Si tenemos permisos, iniciamos el proceso
                startPhotoCaptureProcess()
            } else {
                Toast.makeText(this, "Se requieren permisos de cámara y localización", Toast.LENGTH_LONG).show()
                finish() // Cierra la actividad si no hay permisos
            }
        }

    // 2. Lanzador para la cámara
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                // La foto se guardó en 'imageUri'.
                // Ahora escribimos los metadatos.
                imageUri?.let { uri ->
                    writeExifData(uri, currentLocation)
                }
            } else {
                // El usuario canceló. Borramos la entrada vacía que creamos.
                imageUri?.let { contentResolver.delete(it, null, null) }
                Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show()
            }
            // Sea éxito o no, terminamos la actividad
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No necesitamos un layout complejo, esta actividad es solo un orquestador
        setContentView(R.layout.activity_take_photo)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Iniciar el proceso pidiendo permisos
        val permissionsToRequest = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        allPermissionsLauncher.launch(permissionsToRequest)
    }

    private fun startPhotoCaptureProcess() {
        // Primero, obtenemos la ubicación. Esto es asíncrono.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Usamos getCurrentLocation para una sola actualización de alta precisión
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location: Location? ->
                    // Got the location. Puede ser null si el GPS está desactivado.
                    currentLocation = location
                    if (location == null) {
                        Toast.makeText(this, "No se pudo obtener GPS. Se guardará sin geo-localización.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Ubicación GPS obtenida", Toast.LENGTH_SHORT).show()
                    }
                    // Una vez que tenemos la ubicación (o null), lanzamos la cámara.
                    launchCamera()
                }
                .addOnFailureListener { e ->
                    Log.e("TakePhotoActivity", "Error al obtener ubicación", e)
                    currentLocation = null
                    Toast.makeText(this, "Error al obtener GPS. Se guardará sin geo-localización.", Toast.LENGTH_LONG).show()
                    launchCamera()
                }
        }
    }

    // --- SECCIÓN MODIFICADA ---
    private fun launchCamera() {
        // Creamos la entrada en MediaStore ANTES de lanzar la cámara
        val imageFileName = "IMG_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Guardamos en la carpeta "Pictures/GeoPhotoApp"
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/GeoPhotoApp")
            }
        }

        // Creamos una variable local 'val'
        val uriToLaunch = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        // Verificamos la variable local
        if (uriToLaunch == null) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Guardamos la URI en la variable de la clase para usarla en el callback
        this.imageUri = uriToLaunch

        // Lanzamos la cámara, pasándole la variable local (val) que es segura
        takePictureLauncher.launch(uriToLaunch)
    }
    // --- FIN DE SECCIÓN MODIFICADA ---

    private fun writeExifData(uri: Uri, location: Location?) {
        try {
            // Abrimos un FileDescriptor para la URI en modo "rw" (lectura/escritura)
            contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)

                // 1. Escribir Fecha y Hora
                val dateTime = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(Date())
                exif.setAttribute(ExifInterface.TAG_DATETIME, dateTime)
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateTime)
                exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateTime)

                // 2. Escribir datos GPS (si los tenemos)
                if (location != null) {
                    exif.setGpsInfo(location)
                    Log.d("TakePhotoActivity", "Escribiendo datos GPS: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.d("TakePhotoActivity", "No hay datos GPS para escribir.")
                }

                // Guardar los atributos
                exif.saveAttributes()
            }
            Toast.makeText(this, "Foto guardada con metadatos", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("TakePhotoActivity", "Error al escribir EXIF", e)
            Toast.makeText(this, "Error al guardar metadatos", Toast.LENGTH_SHORT).show()
        }
    }
}

