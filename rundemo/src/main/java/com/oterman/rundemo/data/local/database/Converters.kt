package com.oterman.rundemo.data.local.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room类型转换器
 */
class Converters {
    
    /**
     * Long -> Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    /**
     * Date -> Long
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * List<String> -> String (逗号分隔)
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    /**
     * String -> List<String>
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }
    
    /**
     * List<Double> -> String (逗号分隔)
     */
    @TypeConverter
    fun fromDoubleList(value: List<Double>?): String? {
        return value?.joinToString(",")
    }
    
    /**
     * String -> List<Double>
     */
    @TypeConverter
    fun toDoubleList(value: String?): List<Double>? {
        return value?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { it.toDoubleOrNull() }
    }
}

