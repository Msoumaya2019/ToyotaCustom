package org.toyotacustom.app

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
 @Test fun upgradePreservesHistoryAndReportSurvivesReopen()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="toyota-migration-test-${UUID.randomUUID()}.db"
  val path=context.getDatabasePath(name);path.parentFile?.mkdirs()
  try {
   SQLiteDatabase.openOrCreateDatabase(path,null).use {old->
    old.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,time INTEGER NOT NULL,session TEXT NOT NULL,parameter TEXT NOT NULL,`before` TEXT NOT NULL,`after` TEXT NOT NULL,result TEXT NOT NULL,simulated INTEGER NOT NULL)")
    old.execSQL("CREATE TABLE definitions (id TEXT NOT NULL,json TEXT NOT NULL,PRIMARY KEY(id))")
    old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
    old.execSQL("INSERT INTO room_master_table VALUES(42,'db7e268d5bd87217e483e20ff066ec50')")
    old.execSQL("INSERT INTO history VALUES(1,1,'legacy','test','avant','après','conservé',1)")
    old.execSQL("INSERT INTO definitions VALUES('test','{}')")
    old.version=1
   }
   fun open()=Room.databaseBuilder(context,LocalDatabase::class.java,name).addMigrations(DatabaseMigrations.FROM_1_TO_2).build()
   open().let {db->try {
    assertEquals("conservé",db.store().history().first().single().result)
    assertEquals("test",db.store().definitions().single().id)
    db.store().saveReport(SavedReport("session-test",1,true,"{\"simulation\":true}"))
   }finally{db.close()}}
   open().let {db->try {
    assertEquals("session-test",db.store().reports().first().single().id)
    assertTrue(db.store().reports().first().single().simulated)
    assertEquals(1,db.store().history().first().size)
   }finally{db.close()}}
  }finally{context.deleteDatabase(name)}
 }
}
