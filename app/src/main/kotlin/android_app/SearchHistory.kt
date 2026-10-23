package android_app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.util.Locale

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["normalizedQuery"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val query: String,
    val normalizedQuery: String,
    val searchedAt: Long
)

@Dao
interface SearchHistoryDao {

    @Query(
        "SELECT * FROM search_history " +
                "ORDER BY searchedAt DESC LIMIT 10"
    )
    fun observeRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistoryEntity)

    @Query(
        "DELETE FROM search_history " +
                "WHERE id NOT IN (" +
                "SELECT id FROM search_history " +
                "ORDER BY searchedAt DESC LIMIT :limit)"
    )
    suspend fun keepLatest(limit: Int = 10)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}

@Database(
    entities = [SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SearchHistoryDatabase : RoomDatabase() {

    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: SearchHistoryDatabase? = null

        fun getDatabase(context: Context): SearchHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SearchHistoryDatabase::class.java,
                    "travel_search_history.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class SearchHistoryRepository(
    private val dao: SearchHistoryDao
) {
    val recentSearches: Flow<List<SearchHistoryEntity>> =
        dao.observeRecentSearches()

    suspend fun recordSearch(query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return

        dao.insert(
            SearchHistoryEntity(
                query = cleaned,
                normalizedQuery = cleaned.lowercase(Locale.ROOT),
                searchedAt = System.currentTimeMillis()
            )
        )

        dao.keepLatest(10)
    }

    suspend fun delete(id: Int) {
        dao.deleteById(id)
    }

    suspend fun clear() {
        dao.clearAll()
    }
}
