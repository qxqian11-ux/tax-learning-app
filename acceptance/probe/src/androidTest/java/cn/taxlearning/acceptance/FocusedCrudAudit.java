package cn.taxlearning.acceptance;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.File;
import java.math.BigDecimal;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FocusedCrudAudit extends UiReleaseRegression {
 @Override @Before public void setup() throws Exception {
  super.setup();
  folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"focused-crud/"+prefix);folder.mkdirs();
  note("FOCUSED RUN "+prefix);
 }
 String plain(long cents){return BigDecimal.valueOf(cents,2).toPlainString();}
 void totals(JSONArray baseline,int year,long income,long declared,long paid)throws Exception{
  expect(plain(sum(baseline,year,"incomeCents")+income)+"元");
  expect(plain(sum(baseline,year,"declaredCents")+declared)+"元");
  click("收入合计 ⓘ：");
  UiObject2 message=d.wait(Until.findObject(By.res("android:id/message")),5000);
  assertNotNull(message);assertTrue(message.getText(),message.getText().contains("已缴 "+plain(sum(baseline,year,"paidCents")+paid)+" 元"));
  click("知道了");
 }
 @Test public void twoRequestedFeatures()throws Exception{
  String a="本次核验2024-"+prefix,b="本次核验2025-"+prefix;
  try {
   JSONArray before=records(export(true,"全部年度",file("before.json")));
   assertNull(source(before,a));assertNull(source(before,b));note("BASELINE "+before.length()+" records");
   add(2024,a,"1000.10","10.20","9.30");
   query(2024);expect(a);totals(before,2024,100010,1020,930);snap("01-created-2024");
   add(2025,b,"2000.20","20.40","19.50");
   query(2025);expect(b);assertFalse(d.hasObject(By.text(a)));totals(before,2025,200020,2040,1950);snap("02-created-2025");
   query(2024);assertFalse(d.hasObject(By.text(b)));detail(a);click("修改明细");
   fill("收入金额 *","1500.35");fill("已申报税额 *","15.10");fill("已缴税额 *","14.90");
   click("保存明细");waitText("收入纳税明细详情");expect("1500.35元");expect("15.10元");expect("14.90元");top();snap("03-modified-detail");
   d.executeShellCommand("am force-stop "+pkg);assertEquals("",d.executeShellCommand("pidof "+pkg).trim());note("PASS full process stopped; PID empty before restart");
   query(2024);expect(a);totals(before,2024,150035,1510,1490);snap("04-reopened-2024");
   query(2025);expect(b);totals(before,2025,200020,2040,1950);snap("05-reopened-2025");
   JSONArray kept=records(export(true,"全部年度",file("persisted.json")));
   assertEquals(before.length()+2,kept.length());
   JSONObject ra=source(kept,a),rb=source(kept,b);assertNotNull(ra);assertNotNull(rb);
   assertEquals(2024,ra.getInt("year"));assertEquals(150035,ra.getLong("incomeCents"));assertEquals(1510,ra.getLong("declaredCents"));assertEquals(1490,ra.getLong("paidCents"));
   assertEquals(2025,rb.getInt("year"));assertEquals(200020,rb.getLong("incomeCents"));
   note("PASS query to list, create, view, edit; both years retained after full process restart: 2024=1500.35/15.10/14.90 and 2025=2000.20/20.40/19.50");
   query(2025);delete(b,true);query(2025);assertFalse(d.hasObject(By.text(b)));totals(before,2025,0,0,0);snap("06-deleted-2025-reopened");
   query(2024);expect(a);totals(before,2024,150035,1510,1490);snap("07-2024-unaffected");
   JSONArray after=records(export(true,"全部年度",file("deleted.json")));
   assertEquals(before.length()+1,after.length());assertNull(source(after,b));assertEquals(ra.toString(),source(after,a).toString());
   assertEquals(sum(before,2025,"incomeCents"),sum(after,2025,"incomeCents"));note("PASS delete cancellation, confirmed deletion persists, other year unchanged");
   query(2024);delete(a,false);
   JSONArray clean=records(export(true,"全部年度",file("cleaned.json")));
   assertEquals(before.toString(),clean.toString());note("PASS cleanup: all original UUIDs, fields and amounts unchanged");
   note("ALL TWO REQUESTED FEATURES PASSED "+prefix);
  }catch(Throwable e){snap("focused-failure");note("FAIL "+prefix+" "+e);throw e;}
 }
 @Test public void cleanupFailedProbe()throws Exception{
  JSONArray baseline=records(savedFile("focused-20261103-094337-before.json"));
  query(2024);delete("本次核验2024-focused-20261103-094337",false);
  JSONArray restored=records(export(true,"全部年度",file("cleanup.json")));
  assertEquals(baseline.toString(),restored.toString());note("PASS failed probe cleanup restored original 6 records exactly");
 }

}
