package com.example.mapviewapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.mapviewapp.ui.theme.MapviewAppTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.maps.android.compose.CameraPositionState
import com.example.mapviewapp.ui.theme.MapviewAppTheme
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapviewAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyMap(context = this)
                }
            }
        }
    }
}

@Composable
fun MyMap(context: Context) {
    val singapore = LatLng(1.35, 103.87)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState()
    val markerState = remember { mutableStateOf(MarkerState()) }
    val (latitude, setLatitude) = remember { mutableStateOf(0.0) }
    val (longitude, setLongitude) = remember { mutableStateOf(0.0) }
    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 1000 //päivittää sijainnin joka sekuntti
                fastestInterval = 500 //päivitysten nopein väli
            }
            fetchLocation(cameraPositionState, context, markerState, setLatitude, setLongitude)
            requestLocationUpdates(context, markerState, cameraPositionState, locationRequest, setLatitude, setLongitude)
        } else {
            // Default location
            markerState.value.position = singapore
            cameraPositionState.position = CameraPosition.fromLatLngZoom(singapore, 10f)
        }
    }
    // kysyy sijantilupaa käyttäjältä
    LaunchedEffect(Unit) {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = markerState.value,
                title = "Your Location",
                snippet = "Latitude: ${markerState.value.position.latitude}, " + "Longitude: ${markerState.value.position.longitude}"
            )
        }

        // Buttoni, joka tallentaa coordinaatit sijainnistasi
        SaveButton(
            onClick = {
                // antaa nykyisestä sijainnistasi latituden ja longituden
                saveCoordinatesToFile(latitude, longitude)
                // näyttää popupin, kun coordinaatit on tallennettu
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Coordinates saved")
                }
            },
            modifier = Modifier.padding(16.dp)
        )
        ClearButton(
            onClick = {
                clearCoordinatesFile()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Coordinates file cleared")
                }
            },
            modifier = Modifier.padding(13.dp)
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )

    }
}
//buttonin luonti
@Composable
fun SaveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = "Save Coordinates")
    }
}
@Composable
fun ClearButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = "Clear Coordinates File")
    }
}


// funktio, jolla tallennetaan coordinaatit teksitiedostona.
fun saveCoordinatesToFile(latitude: Double, longitude: Double) {
    val fileName = "coordinates.txt"
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadsDir, fileName)
    try {
        FileOutputStream(file, true).bufferedWriter().use { writer ->
            val coordinatesString = "My coordinates: " +
                    "Latitude: $latitude, Longitude: $longitude, " +
                    "Time: ${getCurrentTime()}\n" // Add a line break or separator
            writer.write(coordinatesString)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
fun clearCoordinatesFile() {
    val fileName = "coordinates.txt"
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadsDir, fileName)
    try {
        FileOutputStream(file).use { outputStream ->
            outputStream.write(byteArrayOf()) // Write an empty byte array to clear the file content
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
fun getCurrentTime(): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val currentTime = Date()
    return dateFormat.format(currentTime)
}

fun fetchLocation(
    cameraPositionState: CameraPositionState,
    context: Context,
    markerState: MutableState<MarkerState>,
    setLatitude: (Double) -> Unit,
    setLongitude: (Double) -> Unit
) {
    val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                    markerState.value.position = latLng
                    setLatitude(location.latitude)
                    setLongitude(location.longitude)
                } else {
                    Log.e("fetchLocation", "Location is unavailable")
                }
            }
    } catch (e: SecurityException) {
        Log.e("fetchLocation", "Security exception: ${e.message}")
    }
}

fun requestLocationUpdates(
    context: Context,
    markerState: MutableState<MarkerState>,
    cameraPositionState: CameraPositionState,
    locationRequest: LocationRequest,
    setLatitude: (Double) -> Unit,
    setLongitude: (Double) -> Unit
) {
    val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                markerState.value.position = latLng
                setLatitude(location.latitude)
                setLongitude(location.longitude)
            }
        }
    }
    try {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    } catch (e: SecurityException) {
        Log.e("requestLocationUpdates", "Security exception: ${e.message}")
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreview(){
    val context = LocalContext.current
    MyMap(context)
}
