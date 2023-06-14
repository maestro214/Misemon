package com.project.misemon

import android.Manifest
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.util.FusedLocationSource
import com.project.misemon.data.Repository
import com.project.misemon.data.models.airquality.Grade
import com.project.misemon.data.models.airquality.MeasuredValue
import com.project.misemon.data.services.models.monitoringstation.MonitoringStation
import com.project.misemon.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.daum.mf.map.api.MapView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
//    private lateinit var locationSource: FusedLocationSource
    private var cancellationTokenSource: CancellationTokenSource? = null

//    private lateinit var naverMap: NaverMap


    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val scope = MainScope()

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timestamp = sdf.format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState) // 부모 클래스인 AppCompatActivity의 onCreate() 메소드를 호출합니다.
        setContentView(binding.root) // activity_main.xml 레이아웃을 이 액티비티에 연결합니다.

        NaverMapSdk.getInstance(this).setClient(
            NaverMapSdk.NaverCloudPlatformClient("z2f7jttck5")
        )


        if (!hasLocationPermission()) {
            setContentView(R.layout.permission_screen)
            bindViews()
            val requestButton = findViewById<Button>(R.id.startButton)
            requestButton.setOnClickListener {

                bindViews()
                requestLocationPermissions()
                initVariables()



                Log.d("퍼미션","화면 넘긴다")

                // 위치 권한 요청을 위한 함수를 호출합니다.
            }
        } else {

            bindViews() // View binding 및 변수 초기화
            initVariables()
            requestLocationPermissions() // 위치 권한 요청을 위한 함수를 호출합니다.
            // 위치 권한이 이미 허용된 경우 다음 동작 수행
        }



//        locationSource =
//            FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
//        Log.d("네이버맵",locationSource.toString())




    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSource?.cancel() // 코루틴 작업을 취소하기 위해 CancellationTokenSource의 cancel() 메서드 호출
        scope.cancel() //코루틴 스코프 해제
    }

    // 위치 권한 요청 결과 처리
    @SuppressWarnings("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d("퍼미션","화면 넘어옴")

        // 권한 요청 결과를 처리하기 위한 함수를 오버라이드
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        ) // 부모 클래스의 onRequestPermissionsResult() 메소드를 호출

        // 위치 권한이 허용되었는지 확인
        val locationPermissionGranted =
            requestCode == REQUEST_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        val backgroundlocationPermissionGranted =
            requestCode == REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!backgroundlocationPermissionGranted) {
                Log.d("퍼미션","거부당함, 화면 다시뜨게한다")

                requestBackgroundLocationPermissions()

            } else {
                binding.permissionLayout.visibility = View.GONE
                fetchAirQualityData()
//                naverMap.locationTrackingMode = LocationTrackingMode.None
                Log.d("퍼미션","통과, 화면 사라지게한다")

            }

        } else {
            // 권한이 거부된 경우, 액티비티를 종료합니다.
            if (!locationPermissionGranted) {
                finish()
//                naverMap.locationTrackingMode = LocationTrackingMode.None

            } else {
                binding.permissionLayout.visibility = View.GONE
                fetchAirQualityData()

            }
        }


    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // View binding 설정
    private fun bindViews() {
        binding.refresh.setOnRefreshListener {
            fetchAirQualityData()
            val intent = Intent(ACTION_REFRESH_DATA)
            sendBroadcast(intent)
        }
        //화면을 아래로 스크롤후 위로 스크롤할때에 화면 최상단에서만 refresh되도록 처리
        binding.layoutScroll.viewTreeObserver.addOnScrollChangedListener {
            binding.refresh.isEnabled = (binding.layoutScroll.scrollY == 0)
        }
    }


    private fun initVariables() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    // 위치 권한 요청
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

    // Android 10 이상의 배경 위치 권한 요청
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

    // 미세먼지 데이터 가져오기
    @SuppressLint("MissingPermission")
    private fun fetchAirQualityData() {

        cancellationTokenSource = CancellationTokenSource()

        fusedLocationProviderClient
            .getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource!!.token
            ).addOnSuccessListener { location ->
                scope.launch {  // 코루틴시작
//                    binding.errorDescriptionTextView.visibility = View.GONE // 에러 메시지 숨김
                    try {
                        Log.d("퍼미션","가까운 관측소 가져오기")
                        // 가까운 관측소 가져오기
                        val monitoringStation =
                            Repository.getNearbyMonitoringStation(
                                location.latitude,
                                location.longitude
                            )

                        // 최신 미세먼지 데이터 가져오기
                        val measuredValue =
                            Repository.getLatestAirQualityData(monitoringStation!!.stationName!!)

                        displayAirQualityData(monitoringStation, measuredValue!!)

                    } catch (exception: Exception) {
                        val toast = Toast.makeText(
                            applicationContext,
                            "연결 상태를 확인할 수 없습니다.",
                            Toast.LENGTH_LONG
                        )
                        toast.show()

                        // 기존 토스트 메시지를 즉시 취소하고 새로운 메시지를 표시
                        Handler(Looper.getMainLooper()).postDelayed({
                            toast.cancel()
                            Toast.makeText(
                                applicationContext,
                                "네트워크 연결 상태를 확인해 주세요.",
                                Toast.LENGTH_LONG
                            ).show()
                        }, 2000) // 2000 밀리초(2초) 후에 실행


//                        binding.errorDescriptionTextView.visibility = View.VISIBLE
                        binding.contentsLayout.alpha = 1F
                        NoneInternetdisplayAirQualityData()

                    } finally {
                        binding.loadingImage.visibility = View.GONE
                        binding.refresh.isRefreshing = false

                        Log.d("퍼미션","파이널리")


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

        val permissionLayout = findViewById<FrameLayout>(R.id.permissionLayout)
        permissionLayout.visibility = View.GONE

        Log.d("퍼미션","디스플레이")

        binding.measuringStationNameTextView.text = monitoringStation.stationName
        binding.timestamp.text = timestamp
        binding.measuringStationAddressTextView.text = "측정 위치 : ${monitoringStation.addr}"





        (measuredValue.khaiGrade ?: Grade.UNKNOWN).let { grade ->

            binding.topbox.setCardBackgroundColor(ContextCompat.getColor(this, grade.boxcolorResId))
            binding.bottombox.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    grade.boxcolorResId
                )
            )




            binding.root.setBackgroundResource(grade.colorResId)
            binding.totalGradeLabelTextView.text = grade.label
            Log.d("색상정보", grade.colorResId.toString())

            val animation = AnimationUtils.loadAnimation(this, R.anim.fall_down)
            animation.interpolator = AnticipateOvershootInterpolator()



            when (grade.label) {
                "좋음" -> {
                    binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_good)
                    binding.totalGradleEmojiTextView.startAnimation(animation)
                }
                "보통" -> {
                    binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_normal)
                    binding.totalGradleEmojiTextView.startAnimation(animation)
                }
                "나쁨" -> {
                    binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_bad)
                    binding.totalGradleEmojiTextView.startAnimation(animation)
                }
                "심각" -> {
                    binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_awful)
                    binding.totalGradleEmojiTextView.startAnimation(animation)
                }
                "정보 없음" -> {
                    binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_awful)
                    binding.totalGradleEmojiTextView.startAnimation(animation)
                }

            }

        }




        with(measuredValue) {

            binding.fineDustText.text = "미세먼지"
            binding.ultraFineDustText.text = "초미세먼지"
            binding.fineDustGradeTextView.text = (pm10Grade ?: Grade.UNKNOWN).toString()
            binding.ultraFineDustGradeTextView.text = (pm25Grade ?: Grade.UNKNOWN).toString()
            binding.fineDustInformationTextView.text = "$pm10Value ㎍/㎥"
            binding.ultraFineDustInformationTextView.text = "$pm25Value ㎍/㎥"

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


    @SuppressLint("SetTextI18n")
    fun NoneInternetdisplayAirQualityData() {
        binding.contentsLayout.animate()
            .alpha(1F)
            .start()

        binding.measuringStationNameTextView.text = "인터넷 사용불가"
        binding.timestamp.text = timestamp
        binding.measuringStationAddressTextView.text = "위치 정보를 찾을 수 없음"



        binding.topbox.setCardBackgroundColor(ContextCompat.getColor(this, R.color.boxmiseblack))
        binding.bottombox.setCardBackgroundColor(ContextCompat.getColor(this, R.color.boxmiseblack))


        binding.root.setBackgroundResource(R.color.miseblack)
        binding.totalGradeLabelTextView.text = "네트워크 연결을\n확인 해주세요"


        binding.totalGradeLabelTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)

        val animation = AnimationUtils.loadAnimation(this, R.anim.fall_down)
        animation.interpolator = AnticipateOvershootInterpolator()


        binding.totalGradleEmojiTextView.setImageResource(R.drawable.misemon_awful)
        binding.totalGradleEmojiTextView.startAnimation(animation)


        binding.fineDustText.text = "미세먼지"
        binding.ultraFineDustText.text = "초미세먼지"
        binding.fineDustGradeTextView.text = "정보없음"
        binding.ultraFineDustGradeTextView.text = "정보없음"
        binding.fineDustInformationTextView.text = "0"
        binding.ultraFineDustInformationTextView.text = "0"

        with(binding.so2Item) {
            labelTextView.text = "아황산가스"
            gradeTextView.text = "정보없음"
            valueTextView.text = "0"
        }

        with(binding.coItem) {
            labelTextView.text = "일산화탄소"
            gradeTextView.text = "정보없음"
            valueTextView.text = "0"
        }

        with(binding.o3Item) {
            labelTextView.text = "오존"
            gradeTextView.text = "정보없음"
            valueTextView.text = "0"
        }

        with(binding.no2Item) {
            labelTextView.text = "이산화질소"
            gradeTextView.text = "정보없음"
            valueTextView.text = "0"
        }
    }


//    override fun onMapReady(p0: NaverMap) {
//        this.naverMap = naverMap
//        naverMap.locationSource = locationSource
//        Log.d("온맵레디","온맵레디")
//
//        Log.d("네이버맵",naverMap.locationSource.toString())
//
//    }


    companion object {
        private const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 100
        private const val REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS = 100
        const val ACTION_REFRESH_DATA = "com.project.misemon.appwidget.ACTION_REFRESH_DATA"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000


    }


}

private fun MapView.setMapViewEventListener(mainActivity: MainActivity) {

}





