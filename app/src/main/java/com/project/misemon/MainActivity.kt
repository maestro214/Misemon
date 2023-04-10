package com.project.misemon

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.project.misemon.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import java.util.jar.Manifest
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var cancellationTokenSource: CancellationTokenSource? = null

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater)}
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState) // 부모 클래스인 AppCompatActivity의 onCreate() 메소드를 호출합니다.
        setContentView(binding.root) // activity_main.xml 레이아웃을 이 액티비티에 연결합니다.

        requestLocationPermissions() // 위치 권한 요청을 위한 함수를 호출합니다.
        initVariables()

    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSource?.cancel()
        scope.cancel()
    }

    @SuppressWarnings("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // 권한 요청 결과를 처리하기 위한 함수를 오버라이드합니다.
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        ) // 부모 클래스의 onRequestPermissionsResult() 메소드를 호출합니다.

        // 위치 권한이 허용되었는지 확인합니다.
        val locationPermissionGranted =
            requestCode == REQUEST_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        // 권한이 거부된 경우, 액티비티를 종료합니다.
        if (!locationPermissionGranted) {
            finish()

        } else {
            // 권한이 승인된 경우, fetchData() 함수를 호출하여 데이터를 가져옵니다.
            //fetchData(주어진 API나 데이터베이스에서 데이터를 가져오는 것을 일컫는 보편적인 용어)

            cancellationTokenSource = CancellationTokenSource()

            fusedLocationProviderClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource!!.token
            ).addOnSuccessListener { location ->
                binding.textview.text = "${location.latitude},${location.longitude}"

            }


        }


    }

    private fun initVariables() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION
            ),
            REQUEST_ACCESS_LOCATION_PERMISSIONS
        )
    }

    companion object {
        private const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 100
    }
}



