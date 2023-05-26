package com.project.misemon.appwidget

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.project.misemon.MainActivity
import com.project.misemon.R
import com.project.misemon.data.Repository
import com.project.misemon.data.models.airquality.Grade
import kotlinx.coroutines.launch

class SimpleAirQualityWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val mainActivityIntent = Intent(context, MainActivity::class.java)
        mainActivityIntent.action = MainActivity.ACTION_REFRESH_DATA
        context?.startActivity(mainActivityIntent)

        val widgetProvider = ComponentName(context!!, SimpleAirQualityWidgetProvider::class.java)
        val updateViews = RemoteViews(context.packageName, R.layout.widget_simple)
        appWidgetManager?.updateAppWidget(widgetProvider, updateViews)

Log.d("온업데이트 정보","실행댐")
        ContextCompat.startForegroundService(
            context!!,
            Intent(context,UpdateWidgetService::class.java)
        )
    }

    class UpdateWidgetService : LifecycleService() {

        override fun onCreate() {
            super.onCreate()
Log.d("온크리트정보","실행댐")
            createChannelIfNeeded()
            startForeground(
                NOTIFICATION_ID,
                createNotification()

            )
        }

        @SuppressLint("RemoteViewLayout")
        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


            // 위젯 레이아웃을 가져옵니다.


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val updateViews = RemoteViews(packageName,R.layout.widget_simple).apply{
                    setTextViewText(
                        R.id.resultTextView,
                        "권한 없음"
                    )

                    setViewVisibility(R.id.labelTextView, View.GONE)
                    setViewVisibility(R.id.gradeLabelTextView, View.GONE)
                }

                updateWidget(updateViews)
                stopSelf()

                return super.onStartCommand(intent, flags, startId)
            }



            LocationServices.getFusedLocationProviderClient(this).lastLocation
                .addOnSuccessListener { location ->
                    lifecycleScope.launch {
                        try {
                            val nearbyMonitoringStation =
                                Repository.getNearbyMonitoringStation(location.latitude, location.longitude)
                            val measuredValue = Repository.getLatestAirQualityData(nearbyMonitoringStation!!.stationName!!)
                            val updateViews = RemoteViews(packageName, R.layout.widget_simple).apply {
                                setViewVisibility(R.id.labelTextView, View.VISIBLE)
                                setViewVisibility(R.id.gradeLabelTextView, View.VISIBLE)


                                (measuredValue?.khaiGrade ?: Grade.UNKNOWN).let {grade ->
                                    Log.d("위젯색상정보",grade.colorResId.toString())
                                    setTextViewText(
                                        R.id.resultTextView,
                                        grade.emoji
                                    )
                                    setTextViewText(
                                        R.id.gradeLabelTextView,
                                        grade.label
                                    )
                                    setInt(R.id.widgetbackground,"setBackgroundResource",grade.colorResId)

                                }
                            }

                            updateWidget(updateViews)
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                        } finally {
                            stopSelf()
                        }
                    }
                }

            return super.onStartCommand(intent, flags, startId)
        }

        override fun onDestroy() {
            super.onDestroy()
            stopForeground(true)
        }

        fun createChannelIfNeeded() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                    ?.createNotificationChannel(
                        NotificationChannel(
                            WIDGET_REFRESH_CHANNEL_ID,
                            "위젯 갱신 채널",
                            NotificationManager.IMPORTANCE_LOW
                        )
                    )


            }
        }

        private fun createNotification() : Notification =
            NotificationCompat.Builder(this)
                .setChannelId(WIDGET_REFRESH_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_refresh_24)
                .build()


        private fun updateWidget(updateViews: RemoteViews){
            val widgetProvider = ComponentName(this, SimpleAirQualityWidgetProvider::class.java)
            AppWidgetManager.getInstance(this).updateAppWidget(widgetProvider, updateViews)
        }

    }



    companion object {
        private const val NOTIFICATION_ID = 101
        private const val WIDGET_REFRESH_CHANNEL_ID = "WIDGET_REFRESH_CHANNEL_ID"

    }
}