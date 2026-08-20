package opb.myniceapp.dint.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UnlockLogEntity::class], version = 1, exportSchema = false)
abstract class UnlockLogDatabase : RoomDatabase() {
    abstract fun unlockLogDao(): UnlockLogDao

    companion object {
        @Volatile
        private var instance: UnlockLogDatabase? = null

        fun getInstance(context: Context): UnlockLogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    UnlockLogDatabase::class.java,
                    "unlock_logs.db",
                ).build().also { instance = it }
            }
    }
}
