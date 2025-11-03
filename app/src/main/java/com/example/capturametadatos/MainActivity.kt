package com.example.capturametadatos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var photoAdapter: PhotoAdapter
    private val photoList = mutableListOf<Uri>()

    // Lanzador para solicitar permisos de lectura de almacenamiento
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadPhotos()
            } else {
                Toast.makeText(this, "El permiso para leer imágenes es necesario", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()

        val fab: FloatingActionButton = findViewById(R.id.fab_take_photo)
        fab.setOnClickListener {
            // Inicia la segunda actividad para tomar la foto
            startActivity(Intent(this, TakePhotoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Cada vez que volvemos a esta actividad, recargamos las fotos
        // para ver las nuevas que se hayan tomado.
        checkAndLoadPhotos()
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_photos)
        photoAdapter = PhotoAdapter(photoList)
        recyclerView.layoutManager = GridLayoutManager(this, 3) // 3 columnas
        recyclerView.adapter = photoAdapter
    }

    private fun checkAndLoadPhotos() {
        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permissionToRequest
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadPhotos()
            }
            shouldShowRequestPermissionRationale(permissionToRequest) -> {
                Toast.makeText(this, "El permiso es necesario para mostrar la galería", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(permissionToRequest)
            }
            else -> {
                requestPermissionLauncher.launch(permissionToRequest)
            }
        }
    }

    private fun loadPhotos() {
        // Usamos un Coroutine en el hilo de IO para no bloquear la UI
        lifecycleScope.launch(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            val photoUris = mutableListOf<Uri>()

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    photoUris.add(contentUri)
                }
            }

            // Actualizamos la UI en el hilo principal
            withContext(Dispatchers.Main) {
                photoList.clear()
                photoList.addAll(photoUris)
                photoAdapter.notifyDataSetChanged()
            }
        }
    }
}
