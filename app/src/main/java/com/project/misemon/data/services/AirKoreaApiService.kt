package com.project.misemon.data.services

import com.google.android.gms.common.api.Response
import com.project.misemon.BuildConfig
import com.project.misemon.data.models.airquality.AirQualityResponse
import com.project.misemon.data.services.models.monitoringstation.MonitoringStationsResponse
import retrofit2.http.GET
import retrofit2.http.Query


interface AirKoreaApiService {

    @GET("B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList" +
            "?serviceKey=${BuildConfig.AIR_KOREA_SERVICE_KEY}" +
            "&returnType=json")
    suspend fun getNearbyMonitoringStation(
        @Query("tmX") tmX: Double,
        @Query("tmY") tmY: Double
    ): retrofit2.Response<MonitoringStationsResponse>

    @GET("B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
            "?serviceKey=${BuildConfig.AIR_KOREA_SERVICE_KEY}" +
            "&returnType=json" +
            "&dataTerm=DAILY" +
            "&ver=1.3")
    suspend fun getRealtimeAirQualties(
        @Query("stationName") stationName: String
    ): retrofit2.Response<AirQualityResponse>
}