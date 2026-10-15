package org.toyotacustom.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="history")
data class History(@PrimaryKey(autoGenerate=true) val id:Long=0,val time:Long=System.currentTimeMillis(),
 val session:String,val parameter:String,val before:String,val after:String,val result:String,val simulated:Boolean)
@Entity(tableName="definitions")
data class StoredDefinition(@PrimaryKey val id:String,val json:String)
@Dao interface Store {
 @Insert suspend fun add(item:History)
 @Query("SELECT * FROM history ORDER BY id DESC") fun history():Flow<List<History>>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun definition(items:List<StoredDefinition>)
 @Query("SELECT * FROM definitions") suspend fun definitions():List<StoredDefinition>
}
@Database(entities=[History::class,StoredDefinition::class],version=1,exportSchema=false)
abstract class LocalDatabase:RoomDatabase() { abstract fun store():Store }
