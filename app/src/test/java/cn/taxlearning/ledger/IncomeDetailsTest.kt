package cn.taxlearning.ledger
import org.junit.Assert.*
import org.junit.Test

class IncomeDetailsTest {
 private fun sample()=Income(year=2024,month=2,category="工资薪金",source="模拟",payer="模拟单位",incomeCents=10000,declaredCents=100,paidCents=100,date="2024-02-29")
 @Test fun moneyEditsAndAliasesKeepExactCents(){val r=IncomeDetails.update(sample(),"income","1500.35");assertEquals(150035L,r.incomeCents);assertEquals(100L,r.declaredCents);assertEquals("1500.35",Rules.decimal(r.incomeCents))}
 @Test fun componentEditsUpdateTotalAndSurviveJsonRoundTrip(){var r=sample();r=IncomeDetails.update(r,"pension","1670.96");r=IncomeDetails.update(r,"medical","417.74");r=IncomeDetails.update(r,"unemployment","83.55");r=IncomeDetails.update(r,"housing","2553.00");assertEquals("4725.25元",IncomeDetails.amount(r,"special"));assertEquals(listOf(r),BackupCodec.decode(BackupCodec.encode(listOf(r))))}
 @Test fun oldBackupsRemainCompatibleAndPeriodClampsLeapDay(){val r=sample();assertEquals(listOf(r),BackupCodec.decode(BackupCodec.encode(listOf(r))));val moved=IncomeDetails.update(r,"period","2025-02");assertEquals("2025-02-28",moved.date);assertEquals(2025,moved.year)}
 @Test fun invalidOptionalDataRejectedBeforeSave(){for(pair in listOf("pension" to "-1","medical" to "1.001","declarationDate" to "2024-02-30","period" to "2018-01")){try{IncomeDetails.update(sample(),pair.first,pair.second);fail("Invalid value accepted")}catch(expected:IllegalArgumentException){}}}
}
