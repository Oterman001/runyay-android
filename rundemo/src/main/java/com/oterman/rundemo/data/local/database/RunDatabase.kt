package com.oterman.rundemo.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.oterman.rundemo.data.local.dao.DailyHealthDao
import com.oterman.rundemo.data.local.dao.OverallVdotDao
import com.oterman.rundemo.data.local.dao.PBRecordDao
import com.oterman.rundemo.data.local.dao.RunAbilityZoneDao
import com.oterman.rundemo.data.local.dao.RunRecordDao
import com.oterman.rundemo.data.local.dao.RunSamplePointDao
import com.oterman.rundemo.data.local.dao.RunSegmentDao
import com.oterman.rundemo.data.local.dao.RunningShoeDao
import com.oterman.rundemo.data.local.dao.TrainPlanDao
import com.oterman.rundemo.data.local.entity.DailyHealthEntity
import com.oterman.rundemo.data.local.entity.OverallVdotEntity
import com.oterman.rundemo.data.local.entity.PBRecordEntity
import com.oterman.rundemo.data.local.entity.RunAbilityZoneEntity
import com.oterman.rundemo.data.local.entity.RunRecordEntity
import com.oterman.rundemo.data.local.entity.RunSamplePointEntity
import com.oterman.rundemo.data.local.entity.RunSegmentEntity
import com.oterman.rundemo.data.local.entity.RunningShoeEntity
import com.oterman.rundemo.data.local.entity.TrainPlanEntity

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
        DailyHealthEntity::class,
        RunningShoeEntity::class,
        TrainPlanEntity::class
    ],
    version = 8,
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
    abstract fun runningShoeDao(): RunningShoeDao
    abstract fun trainPlanDao(): TrainPlanDao
    
    companion object {
        private const val DATABASE_NAME = "run_database"

        @Volatile
        private var INSTANCE: RunDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // run_record: 添加 userId 字段 + 索引
                db.execSQL("ALTER TABLE run_record ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_run_record_userId ON run_record(userId)")

                // pb_record: 添加 userId 字段 + 复合索引
                db.execSQL("ALTER TABLE pb_record ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pb_record_userId_type_subType ON pb_record(userId, type, subType)")

                // overall_vdot: 添加 userId 字段 + 复合索引
                db.execSQL("ALTER TABLE overall_vdot ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_overall_vdot_userId_date ON overall_vdot(userId, date)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // overall_vdot: 添加 confidence 字段
                db.execSQL("ALTER TABLE overall_vdot ADD COLUMN confidence REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS running_shoe (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        brand TEXT,
                        model TEXT,
                        shoeSize TEXT,
                        nickname TEXT,
                        shoeType TEXT NOT NULL DEFAULT 'training',
                        price REAL,
                        expectedLifespan REAL NOT NULL DEFAULT 700.0,
                        firstUseDate INTEGER,
                        retireDate INTEGER,
                        initialDistance REAL NOT NULL DEFAULT 0.0,
                        totalDistance REAL NOT NULL DEFAULT 0.0,
                        totalDuration REAL NOT NULL DEFAULT 0.0,
                        totalRuns INTEGER NOT NULL DEFAULT 0,
                        imagePath TEXT,
                        imageUrl TEXT,
                        notes TEXT,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        color TEXT,
                        syncStatus TEXT NOT NULL DEFAULT 'localOnly',
                        syncRetryCount INTEGER NOT NULL DEFAULT 0,
                        serverShoeId TEXT,
                        lastSyncAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_running_shoe_userId ON running_shoe(userId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE run_record ADD COLUMN activityTimeZone TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS train_plan (
                        planId TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        trainWholeType TEXT NOT NULL DEFAULT 'SELF_DEFINE',
                        scheduledDate TEXT,
                        hardLevel INTEGER,
                        finishFlag TEXT DEFAULT 'N',
                        locationType TEXT,
                        workoutId TEXT,
                        version INTEGER,
                        detailJson TEXT,
                        lastSyncAt INTEGER NOT NULL DEFAULT 0,
                        isDirty INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_train_plan_userId ON train_plan(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_train_plan_scheduledDate ON train_plan(scheduledDate)")
            }
        }

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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
