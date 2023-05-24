package com.project.misemon.data.models.airquality

import androidx.annotation.ColorRes
import com.google.gson.annotations.SerializedName
import com.project.misemon.R

enum class Grade(
    val label: String,
    val emoji: String,
    @ColorRes val colorResId: Int
) {

    @SerializedName("1")
    GOOD("좋음", "😆", R.color.blue),

    @SerializedName("2")
    NORMAL("보통", "🙂", R.color.green),

    @SerializedName("3")
    BAD("나쁨", "😞", R.color.red),

    @SerializedName("4")
    AWFUL("심각", "😫", R.color.miseblack),

    UNKNOWN("정보 없음", "🧐", R.color.gray);

    override fun toString(): String {
        return "$label $emoji"

    }

}