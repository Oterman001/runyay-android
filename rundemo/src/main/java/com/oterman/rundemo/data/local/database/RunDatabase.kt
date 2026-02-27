package com.oterman.rundemo.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.oterman.rundemo.data.local.dao.DailyHealthDao
import com.oterman.rundemo.data.local.dao.OverallVdotDao
import com.oterman.rundemo.data.local.dao.PBRecordDao
import com.oterman.rundemo.data.local.dao.RunAbilityZoneDao
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.local.entity.DailyHealthEntity
import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.data.local.entity.PBRecordEntity
import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity

/**
 * Room数据库定义
 * 
 * 包含的表：
 * - run_record: 跑步记录主表
 * - run_sample_point: 采样点表（合并了序列数据和GPS轨迹）
 * - run_segment: 分段表
 * - run_ability_zone: 能力区间表
 */
@Database(
    entities = [
        RunRecordEntity::class,
        RunSamplePointEntity::class,
        RunSegmentEntity::class,
        RunAbilityZoneEntity::class,
        PBRecordEntity::class,
        OverallVdotEntity::class,
        DailyHealthEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RunDatabase : RoomDatabase() {
    
    abstract fun runRecordDao(): RunRecordDao
    abstract fun runSamplePointDao(): RunSamplePointDao
    abstract fun runSegmentDao(): RunSegmentDao
    abstract fun runAbilityZoneDao(): RunAbilityZoneDao
    abstract fun pbRecordDao(): PBRecordDao
    abstract fun overallVdotDao(): OverallVdotDao
    abstract fun dailyHealthDao(): DailyHealthDao
    
    companion object {
        private const val DATABASE_NAME = "run_database"
        
        @Volatile
        private var INSTANCE: RunDatabase? = null
        
        /**
         * 获取数据库单例
         */
        fun getInstance(context: Context): RunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RunDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // 版本升级时如果没有Migration则销毁重建
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

