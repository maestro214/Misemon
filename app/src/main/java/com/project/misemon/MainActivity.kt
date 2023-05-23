package com.project.misemon

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.project.misemon.data.Repository
import com.project.misemon.data.models.airquality.Grade
import com.project.misemon.data.models.airquality.MeasuredValue
import com.project.misemon.data.services.models.monitoringstation.MonitoringStation
import com.project.misemon.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var cancellationTokenSource: CancellationTokenSource? = null

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val scope = MainScope()

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timestamp = sdf.format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState) // 부모 클래스인 AppCompatActivity의 onCreate() 메소드를 호출합니다.
        setContentView(binding.root) // activity_main.xml 레이아웃을 이 액티비티에 연결합니다.

        bindViews()
        initVariables()
        requestLocationPermissions() // 위치 권한 요청을 위한 함수를 호출합니다.


    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSource?.cancel()
        scope.cancel() //코루틴 스코프 해제
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

        val backgroundlocationPermissionGranted =
            requestCode == REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!backgroundlocationPermissionGranted) {
                requestBackgroundLocationPermissions()


            } else {

                fetchAirQualityData()

            }

        } else {
            // 권한이 거부된 경우, 액티비티를 종료합니다.
            if (!locationPermissionGranted) {
                finish()

            } else {
                // 권한이 승인된 경우, fetchData() 함수를 호출하여 데이터를 가져옵니다.
                //fetchData(주어진 API나 데이터베이스에서 데이터를 가져오는 것을 일컫는 보편적인 용어)

                fetchAirQualityData()

            }
        }
    }


    private fun bindViews() {
        binding.refresh.setOnRefreshListener {
            fetchAirQualityData()
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


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                ACCESS_BACKGROUND_LOCATION,
            ),
            REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS
        )
    }


    @SuppressLint("MissingPermission")
    private fun fetchAirQualityData() {
        cancellationTokenSource = CancellationTokenSource()

        fusedLocationProviderClient
            .getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource!!.token
            ).addOnSuccessListener { location ->
                scope.launch {  // 코루틴시작
                    binding.errorDescriptionTextView.visibility = View.GONE
                    try {

                        val monitoringStation =
                            Repository.getNearbyMonitoringStation(
                                location.latitude,
                                location.longitude
                            )
                        val measuredValue =
                            Repository.getLatestAirQualityData(monitoringStation!!.stationName!!)

                        displayAirQualityData(monitoringStation, measuredValue!!)

                    } catch (exception: Exception) {
                        binding.errorDescriptionTextView.visibility = View.VISIBLE
                        binding.contentsLayout.alpha = 0F

                    } finally {
                        binding.progressBar.visibility = View.GONE
                        binding.refresh.isRefreshing = false


                    }


                }

            }

        //깃 유저네임 변경
        //깃 이메일 변경
    }

    @SuppressLint("SetTextI18n")
    fun displayAirQualityData(monitoringStation: MonitoringStation, measuredValue: MeasuredValue) {
        binding.contentsLayout.animate()
            .alpha(1F)
            .start()

        binding.measuringStationNameTextView.text = monitoringStation.stationName
        binding.timestamp.text = timestamp
        binding.measuringStationAddressTextView.text = "측정 위치 : ${monitoringStation.addr}"

        Log.d("정보",measuredValue.toString())


        (measuredValue.khaiGrade ?: Grade.UNKNOWN).let { grade ->
            binding.root.setBackgroundResource(grade.colorResId)
            binding.totalGradeLabelTextView.text = grade.label



            when (grade.emoji) {
                "\uD83D\uDE06" -> binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_good)
                "\uD83D\uDE42" -> binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_normal)
                "\uD83D\uDE1E" -> binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_bad)
                "\uD83D\uDE2B" -> binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_awful)
                "\uD83E\uDDD0" ->  binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_awful)

            }

        }

        with(measuredValue) {

            binding.fineDustText.text = "미세먼지"
            binding.ultraFineDustText.text = "초미세먼지"
            binding.fineDustInformationTextView.text =
                " ${(pm10Grade ?: Grade.UNKNOWN).toString()} \n $pm10Value ㎍/㎥"
            binding.ultraFineDustInformationTextView.text =
                " ${(pm25Grade ?: Grade.UNKNOWN).toString()} \n $pm25Value ㎍/㎥"

            with(binding.so2Item) {
                labelTextView.text = "아황산가스"
                gradeTextView.text = (so2Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$so2Value ppm"
            }

            with(binding.coItem) {
                labelTextView.text = "일산화탄소"
                gradeTextView.text = (coGrade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$coValue ppm"
            }

            with(binding.o3Item) {
                labelTextView.text = "오존"
                gradeTextView.text = (o3Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$o3Value ppm"
            }

            with(binding.no2Item) {
                labelTextView.text = "이산화질소"
                gradeTextView.text = (no2Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$no2Value ppm"
            }
        }
    }


    companion object {
        private const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 100
        private const val REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS = 100

    }
}



