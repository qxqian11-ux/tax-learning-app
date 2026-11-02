package cn.taxlearning.ledger

import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    private fun sample()=Income(id="a51218c2-e963-4d59-a002-d10102a252da",year=2024,month=2,category="工资薪金",source="模拟工资",payer="模拟公司",incomeCents=123456,declaredCents=101,paidCents=99,date="2024-02-29",note="示例")
    private fun rejects(block: () -> Unit) { try { block(); fail("应拒绝无效输入") } catch(e: IllegalArgumentException) { assertTrue(e.message!!.isNotBlank()) } }
    @Test fun moneyNeverUsesFloatingPoint() {
        assertEquals(30L,Rules.cents("0.10","收入")+Rules.cents("0.20","收入"))
        assertEquals("0.01",Rules.decimal(1)); assertEquals("1234.56",Rules.decimal(123456))
        listOf("-1","1.234","1e2","NaN","","1,000","9999999999999999999999999","10000000000").forEach { rejects { Rules.cents(it,"收入") } }
        assertEquals(0L,Rules.cents("0","收入")); assertEquals(Rules.MAX_CENTS,Rules.cents("9999999999.99","收入"))
    }
    @Test fun yearsAndDatesAreValidated() {
        Rules.validate(sample(),2024)
        rejects { Rules.validate(sample().copy(year=2018),2024) }
        rejects { Rules.validate(sample().copy(year=2025),2024) }
        rejects { Rules.validate(sample().copy(month=13),2024) }
        rejects { Rules.validate(sample().copy(source="  "),2024) }
        rejects { Rules.validate(sample().copy(category="未知"),2024) }
        rejects { Rules.validate(sample().copy(paidCents=-1),2024) }
        assertFalse(Rules.dateValid("2023-02-29")); assertFalse(Rules.dateValid("2024-02-30")); assertFalse(Rules.dateValid("2024-01-01x"))
    }
    @Test fun backupRoundTripsExactlyAndRejectsCorruption() {
        val r=sample().copy(source="=1+1,\"模拟\"\n记录")
        assertEquals(listOf(r),BackupCodec.decode(BackupCodec.encode(listOf(r)),2024))
        rejects { BackupCodec.decode("{}",2024) }
        rejects { BackupCodec.decode(BackupCodec.encode(listOf(r)).replace("123456","1.25"),2024) }
        rejects { BackupCodec.decode(BackupCodec.encode(listOf(r,r)),2024) }
        rejects { BackupCodec.decode(BackupCodec.encode(listOf(r)).replace("2024-02-29","2023-02-29"),2024) }
        rejects { BackupCodec.decode(BackupCodec.encode(listOf(r)),2023) }
        assertTrue(BackupCodec.csv(listOf(r)).contains("'"+"=1+1,"))
        assertTrue(BackupCodec.csv(listOf(r)).startsWith("\uFEFF"))
    }
    @Test fun totalsRemainExact() {
        val rows=listOf(sample().copy(incomeCents=10),sample().copy(incomeCents=20))
        assertEquals(30L,rows.totals().income); assertEquals(202L,rows.totals().declared); assertEquals(198L,rows.totals().paid)
    }
}
