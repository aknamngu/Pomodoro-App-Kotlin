package com.example.pomodoro2

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. ENTITY: LƯU TRỮ NHIỆM VỤ & SỰ KIỆN LỊCH
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val rewardDrops: Int,
    val progress: Int = 0,
    val maxProgress: Int = 1,
    val isSystem: Boolean = true,
    val isClaimed: Boolean = false,
    val eventTimestamp: Long? = null // Lưu thời gian sự kiện (nếu có)
)

// 2. ENTITY: LƯU TRỮ LỊCH SỬ TẬP TRUNG
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskName: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val icon: String = "🌿"
)

// 3. ENTITY: LƯU TRỮ CÂY TRONG VƯỜN
@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey val plotNumber: Int,
    val plantType: String,
    val timesWatered: Int = 0,
    val isHarvested: Boolean = false
)

// 4. DAO: CÁC CÂU LỆNH TRUY VẤN
@Dao
interface AuraDao {
    // Truy vấn Task
    @Query("SELECT * FROM tasks ORDER BY isSystem DESC, eventTimestamp ASC, id ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskWithId(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :keyword || '%' LIMIT 1")
    suspend fun findTaskByKeyword(keyword: String): TaskEntity?

    // Truy vấn History
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insertHistory(history: HistoryEntity)

    @Query("SELECT SUM(durationMinutes) FROM history")
    fun getTotalFocusTime(): Flow<Int?>

    // Truy vấn Plant
    @Query("SELECT * FROM plants")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: PlantEntity)

    @Update
    suspend fun updatePlant(plant: PlantEntity)

    @Delete
    suspend fun deletePlant(plant: PlantEntity)

    @Query("DELETE FROM plants")
    suspend fun deleteAllPlants() 
}

// 5. DATABASE: CẤU HÌNH ROOM
@Database(entities = [TaskEntity::class, HistoryEntity::class, PlantEntity::class], version = 3, exportSchema = false)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun auraDao(): AuraDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getDatabase(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "aura_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
