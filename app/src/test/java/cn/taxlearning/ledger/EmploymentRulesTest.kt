package cn.taxlearning.ledger

import org.junit.Assert.*
import org.junit.Test

class EmploymentRulesTest {
    private fun rejects(row: Employment) {
        try { EmploymentRules.validate(row);fail("无效记录应被拒绝") }
        catch(e:IllegalArgumentException) { assertFalse(e.message.isNullOrBlank()) }
    }
    @Test fun optionalFieldsAndEmploymentDates() {
        val row=Employment(companyName="课程模拟单位")
        EmploymentRules.validate(row)
        EmploymentRules.validate(row.copy(creditCode="123456789012345678",position="工程师",startDate="2020-09-09",endDate="2024-02-29"))
        rejects(row.copy(companyName="  "))
        rejects(row.copy(creditCode="123"))
        rejects(row.copy(startDate="2023-02-29"))
        rejects(row.copy(startDate="2024-03-01",endDate="2024-02-29"))
        EmploymentRules.validate(row.copy(startDate="2024-02-29",endDate="2024-02-29"))
    }
}
