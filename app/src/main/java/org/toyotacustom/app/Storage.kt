package org.toyotacustom.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName="history")
data class History(@PrimaryKey(autoGenerate=true) val id:Long=0,val time:Long=System.currentTimeMillis(),
 val session:String,val parameter:String,val before:String,val after:String,val result:String,val simulated:Boolean)
@Entity(tableName="definitions")
data class StoredDefinition(@PrimaryKey val id:String,val json:String)
@Entity(tableName="session_reports")
data class SavedReport(@PrimaryKey val id:String,val createdAt:Long,val simulated:Boolean,val json:String)
@Dao interface Store {
 @Insert suspend fun add(item:History)
 @Query("SELECT * FROM history ORDER BY id DESC") fun history():Flow<List<History>>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun definition(items:List<StoredDefinition>)
 @Query("SELECT * FROM definitions") suspend fun definitions():List<StoredDefinition>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveReport(report:SavedReport)
 @Query("SELECT * FROM session_reports ORDER BY createdAt DESC LIMIT 50") fun reports():Flow<List<SavedReport>>
}
object DatabaseMigrations {
 val FROM_1_TO_2=object:Migration(1,2) {
  override fun migrate(db:SupportSQLiteDatabase) {
   db.execSQL("CREATE TABLE IF NOT EXISTS `session_reports` (`id` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `simulated` INTEGER NOT NULL, `json` TEXT NOT NULL, PRIMARY KEY(`id`))")
  }
 }
}
@Database(entities=[History::class,StoredDefinition::class,SavedReport::class],version=2,exportSchema=false)
abstract class LocalDatabase:RoomDatabase() { abstract fun store():Store }
