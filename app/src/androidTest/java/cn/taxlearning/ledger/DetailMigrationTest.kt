package cn.taxlearning.ledger
import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DetailMigrationTest {
 @Test fun migrationRetainsOldRecordAndSeedMarkerAndDetailsPersist()=runBlocking {
  val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
  val name="detail-migration-${UUID.randomUUID()}.db";var repo:Repository?=null
  val id=UUID.randomUUID().toString()
  try {
   app.openOrCreateDatabase(name,0,null).use {db ->
    db.execSQL("CREATE TABLE incomes (id TEXT NOT NULL PRIMARY KEY, year INTEGER NOT NULL, month INTEGER NOT NULL, category TEXT NOT NULL, source TEXT NOT NULL, payer TEXT NOT NULL, incomeCents INTEGER NOT NULL, declaredCents INTEGER NOT NULL, paidCents INTEGER NOT NULL, date TEXT NOT NULL, note TEXT NOT NULL)")
    db.execSQL("CREATE INDEX index_incomes_year_month ON incomes(year,month)")
    db.execSQL("CREATE TABLE metadata (`key` TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
    db.execSQL("INSERT INTO metadata VALUES('seed-v1','done')")
    db.execSQL("INSERT INTO incomes VALUES(?,2024,2,'工资薪金','迁移测试','模拟单位',10000,100,100,'2024-02-29','旧数据')",arrayOf(id));db.version=1
   }
   var active=Repository(app,name);repo=active;assertEquals(1,active.all().size)
   val original=active.get(id)!!;assertEquals("{}",original.detailJson);assertEquals("旧数据",original.note)
   var updated=IncomeDetails.update(original,"payerTaxId","DEMO-123")
   updated=IncomeDetails.update(updated,"pension","10.20");active.save(updated,true)
   active.db.close();active=Repository(app,name);repo=active
   assertEquals(updated,active.get(id));assertEquals(1,active.all().size)
   val decoded=BackupCodec.decode(BackupCodec.encode(listOf(updated)));assertEquals(listOf(updated),decoded);assertEquals(0,active.restore(decoded).first)
  } finally {repo?.db?.close();app.deleteDatabase(name)}
 }
}
