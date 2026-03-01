package com.oterman.rundemo.util

object AppleWatchDeviceUtils {
    fun getModelName(identifier: String?): String {
        return when (identifier) {
            "Watch2,6", "Watch2,7" -> "Apple Watch Series 1"
            "Watch2,3", "Watch2,4" -> "Apple Watch Series 2"
            "Watch3,1", "Watch3,2", "Watch3,3", "Watch3,4" -> "Apple Watch Series 3"
            "Watch4,1", "Watch4,2", "Watch4,3", "Watch4,4" -> "Apple Watch Series 4"
            "Watch5,1", "Watch5,2", "Watch5,3", "Watch5,4" -> "Apple Watch Series 5"
            "Watch5,9", "Watch5,10", "Watch5,11", "Watch5,12" -> "Apple Watch SE"
            "Watch6,1", "Watch6,2", "Watch6,3", "Watch6,4" -> "Apple Watch Series 6"
            "Watch6,6", "Watch6,7", "Watch6,8", "Watch6,9" -> "Apple Watch Series 7"
            "Watch6,10", "Watch6,11", "Watch6,12", "Watch6,13" -> "Apple Watch SE 2"
            "Watch6,14", "Watch6,15", "Watch6,16", "Watch6,17" -> "Apple Watch Series 8"
            "Watch6,18" -> "Apple Watch Ultra"
            "Watch7,1", "Watch7,2", "Watch7,3", "Watch7,4" -> "Apple Watch Series 9"
            "Watch7,5" -> "Apple Watch Ultra 2"
            "Watch7,8", "Watch7,9", "Watch7,10", "Watch7,11" -> "Apple Watch Series 10"
            "Watch7,12" -> "Apple Watch Ultra 3"
            "Watch7,13", "Watch7,14", "Watch7,15", "Watch7,16" -> "Apple Watch SE 3"
            "Watch7,17", "Watch7,18", "Watch7,19", "Watch7,20" -> "Apple Watch Series 11"
            else -> "Apple Watch"
        }
    }
}
