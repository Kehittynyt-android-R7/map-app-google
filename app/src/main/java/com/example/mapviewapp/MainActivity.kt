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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.maps.android.compose.CameraPositionState

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
    val cameraPositionState = rememberCameraPositionState()
    val markerState = remember { mutableStateOf(MarkerState()) }
    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 1000 // päivittää sijainnin joka sekuntti
                fastestInterval = 500 // päivitysten nopein aikaväli
            }
            fetchLocation(cameraPositionState, context, markerState)
            requestLocationUpdates(context, markerState, cameraPositionState, locationRequest)
        } else {
            // oletussijainti
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
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = markerState.value,
            title = "Your Location",
            snippet = "You are here"
        )
    }
}
// sijainnin päivitys
fun requestLocationUpdates(context: Context, markerState: MutableState<MarkerState>, cameraPositionState: CameraPositionState, locationRequest: LocationRequest) {
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
// hakee sijainnin
fun fetchLocation(
    cameraPositionState: CameraPositionState,
    context: Context,
    markerState: MutableState<MarkerState>
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
                } else {
                    Log.e("fetchLocation", "Location is unavailable")
                }
            }
    } catch (e: SecurityException) {
        Log.e("fetchLocation", "Security exception: ${e.message}")
    }
}
@Preview(showBackground = true)
@Composable
fun MapPreview(){
    val context = LocalContext.current
    MyMap(context)
}