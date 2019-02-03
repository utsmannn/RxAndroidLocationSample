package com.utsman.rxandroidlocationsample

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val TAG = "APP_MAP"
    private val REQUEST_CHECK_SETTINGS = 0

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var googleMap: GoogleMap

    private lateinit var disposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapFragment = map_view as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    @SuppressLint("MissingPermission")
    private fun getPermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                        getDeviceLocation()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                            permission: PermissionRequest?,
                            token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        Toast.makeText(this@MainActivity, "Location not enable", Toast.LENGTH_SHORT).show()
                    }

                })
                .check()
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        val locationProvider = ReactiveLocationProvider(this)


        val requestLocation = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setInterval(5000)

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(requestLocation)
                .setAlwaysShow(true)
                .build()

        locationProvider.checkLocationSettings(builder)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    val status = it.status
                    if (status.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                        } catch (intentSender: IntentSender.SendIntentException) {
                            Log.e(TAG, "Error open setting", intentSender)
                        }
                    }
                }
                .subscribe()

        locationProvider
                .getUpdatedLocation(requestLocation)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observerLocation())


    }

    @SuppressLint("MissingPermission")
    private fun observerLocation() : Observer<Location> {
        return object : Observer<Location> {
            override fun onComplete() {
                Log.i(TAG, "complete request")
            }

            override fun onSubscribe(d: Disposable) {
                disposable = d
            }

            override fun onNext(t: Location) {
                val mLat = t.latitude
                val mlng = t.longitude
                googleMap.isMyLocationEnabled = true
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mLat, mlng), 12f))
                toast("location at $mLat, $mlng")
            }

            override fun onError(e: Throwable) {
                Log.e(TAG, "error", e)
                toast("error : ${e.localizedMessage}")
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
    }

    override fun onStart() {
        super.onStart()
        getPermission()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onStop() {
        super.onStop()
        disposable.dispose()
    }
}
