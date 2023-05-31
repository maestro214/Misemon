package com.project.misemon.data.models.airquality

import androidx.annotation.ColorRes
import com.google.gson.annotations.SerializedName
import com.project.misemon.R

enum class Grade(
    val label: String,
//    val emoji: String,
    @ColorRes val colorResId: Int,
    @ColorRes val boxcolorResId: Int
) {

    @SerializedName("1")//"😆",
    GOOD("좋음",  R.color.blue,R.color.boxblue),

    @SerializedName("2")//"🙂",
    NORMAL("보통",  R.color.green,R.color.boxgreen),

    @SerializedName("3")//"😞",
    BAD("나쁨",  R.color.red,R.color.boxred),

    @SerializedName("4")//"😫",
    AWFUL("심각",  R.color.miseblack,R.color.boxmiseblack),

    UNKNOWN("정보 없음",  R.color.gray,R.color.miseblack);//"🧐",

    override fun toString(): String {
        return "$label "

    }

}