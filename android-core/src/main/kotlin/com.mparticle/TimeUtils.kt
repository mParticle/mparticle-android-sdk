package com.mparticle

import java.text.SimpleDateFormat
import java.util.Date

fun millisToLoggingDate(millis: Long): String {
    return SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Date(millis))
}