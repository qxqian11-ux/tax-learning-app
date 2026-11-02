package cn.taxlearning.acceptance;

import androidx.test.uiautomator.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.os.SystemClock;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UiReleaseRegression extends ReleaseAudit {
 String prefix;final String sourceA="UI110验收2024工资",sourceB="UI110验收2025工资",sourceC="UI110验收2024稿酬";
 @Override @Before public void setup()throws Exception{super.setup();folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"ui-regression");folder.mkdirs();prefix=InstrumentationRegistry.getArguments().getString("runPrefix","ui110-"+System.currentTimeMillis());note("RUN "+prefix);}
 String file(String label){return prefix+"-"+label;}
 void id(String name){UiObject2 o=d.wait(Until.findObject(By.res(pkg+":id/"+name)),8000);assertNotNull("Missing id "+name,o);o.click();d.waitForIdle();SystemClock.sleep(400);}
 void listReady(){assertTrue(d.wait(Until.hasObject(By.res(pkg+":id/income_add")),8000));d.waitForIdle();SystemClock.sleep(300);}
 @Override UiObject2 find(String text)throws Exception{UiObject2 found=d.findObject(By.text(text));if(found!=null)return found;UiScrollable list=new UiScrollable(new UiSelector().scrollable(true));if(list.exists()){list.setSwipeDeadZonePercentage(.22);list.scrollIntoView(new UiSelector().text(text));}return waitText(text);}
 @Override void click(String s)throws Exception{for(int i=0;i<6;i++){try{UiObject2 o=find(s);UiObject2 nav=d.findObject(By.res(pkg+":id/nav_0"));if(nav!=null&&o.getVisibleBounds().bottom>=nav.getParent().getVisibleBounds().top){d.swipe(d.getDisplayWidth()/2,d.getDisplayHeight()*7/10,d.getDisplayWidth()/2,d.getDisplayHeight()*4/10,30);d.waitForIdle();continue;}o.click();d.waitForIdle();SystemClock.sleep(350);return;}catch(StaleObjectException e){if(i==5)throw e;}}fail("Cannot click "+s);}
 @Override void home()throws Exception{d.executeShellCommand("am force-stop "+pkg);note("force-stop PID after = "+d.executeShellCommand("pidof "+pkg).trim());d.executeShellCommand("am start -W -n "+pkg+"/.MainActivity");waitText("首页");}
 @Override void query(int year)throws Exception{home();id("nav_2");click("收入纳税明细");id("query_year");UiScrollable years=new UiScrollable(new UiSelector().className("android.widget.ListView"));years.scrollIntoView(new UiSelector().text(year+" 年"));click(year+" 年");id("query_submit");listReady();}
 @Override void backupPage()throws Exception{home();id("nav_2");click("数据备份与恢复");waitText("数据备份与恢复");}
 @Override void openBackupFile(String name)throws Exception{
  backupPage();click("选择 JSON 备份文件");UiObject2 roots=d.wait(Until.findObject(By.desc("Show roots")),7000);assertNotNull(roots);roots.click();d.waitForIdle();UiObject2 drawer=d.wait(Until.findObject(By.res("com.android.documentsui:id/roots_list")),7000);assertNotNull(drawer);UiObject2 downloads=drawer.findObject(By.text("Downloads"));assertNotNull(downloads);downloads.click();d.waitForIdle();assertTrue("Downloads directory selected",d.wait(Until.hasObject(By.res("com.android.documentsui:id/breadcrumb_text").text("Downloads")),7000));UiScrollable files=new UiScrollable(new UiSelector().resourceId("com.android.documentsui:id/dir_list"));if(!d.hasObject(By.text(name)))files.scrollIntoView(new UiSelector().text(name));click(name);SystemClock.sleep(500);
 }
 @Override void add(int year,String source,String income,String declared,String paid)throws Exception{query(year);id("income_add");waitText("新增收入明细");fill("收入来源 *",source);fill("扣缴义务人 *","UI重构验收模拟单位");fill("收入金额 *",income);fill("已申报税额 *",declared);fill("已缴税额 *",paid);click("保存明细");listReady();expect(source);note("UI added "+year+" "+source);}
 @Override void delete(String source,boolean cancelFirst)throws Exception{detail(source);click("删除明细");waitText("删除这笔收入？");if(cancelFirst){click("取消");expect(source);click("删除明细");}click("确认删除");listReady();}
 JSONObject source(JSONArray records,String name)throws Exception{for(int i=0;i<records.length();i++)if(records.getJSONObject(i).getString("source").equals(name))return records.getJSONObject(i);return null;}
 String money(long cents){return String.format(Locale.CHINA,"%,.2f",BigDecimal.valueOf(cents,2));}
 String filtered(JSONArray rows,int month,String category)throws Exception{int count=0;long income=0;for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(r.getInt("year")==2024&&(month==0||r.getInt("month")==month)&&(category==null||r.getString("category").equals(category))){count++;income+=r.getLong("incomeCents");}}return "筛选结果 "+count+" 笔 · 收入 ¥ "+money(income);}
 @Test public void releaseRegression()throws Exception{
  try{
   JSONArray baseline=records(export(true,"全部年度",file("baseline.json")));JSONArray legacy=records(savedFile("audit-final.json"));assertEquals("Existing 1.0.0 records must survive signed upgrade",legacy.toString(),baseline.toString());note("PASS signed upgrade retained all "+baseline.length()+" original record IDs and values");
   add(2024,sourceA,"1000.10","10.20","9.30");add(2025,sourceB,"2000.20","20.40","19.50");query(2024);detail(sourceA);click("修改明细");fill("收入金额 *","1500.35");fill("已申报税额 *","15.10");fill("已缴税额 *","14.90");click("保存明细");waitText("收入纳税明细详情");expect("1500.35元");snap("release-detail-edited");query(2025);delete(sourceB,true);
   home();query(2024);detail(sourceA);expect("1500.35元");expect("15.10元");expect("14.90元");snap("release-restart-2024");query(2025);assertFalse(d.hasObject(By.text(sourceB)));snap("release-restart-2025");
   JSONArray scenario=records(export(true,"全部年度",file("scenario.json")));assertEquals(baseline.length()+1,scenario.length());assertNotNull(source(scenario,sourceA));assertNull(source(scenario,sourceB));assertEquals(sum(baseline,2024,"incomeCents")+150035,sum(scenario,2024,"incomeCents"));assertEquals(sum(baseline,2025,"incomeCents"),sum(scenario,2025,"incomeCents"));note("PASS CRUD, independent years, cancellation, full process restart and exact totals");
   String csv=export(false,"2024 年",file("2024.csv"));assertTrue(csv.startsWith("\ufeff"));assertTrue(csv.contains(sourceA));assertTrue(csv.contains("1500.35"));assertTrue(csv.contains("15.10"));assertTrue(csv.contains("14.90"));assertFalse(csv.contains(sourceB));note("PASS actual CSV export via system picker with year and amount checks");
   restore(file("scenario.json"),scenario.length());assertEquals(scenario.toString(),records(export(true,"全部年度",file("duplicate.json"))).toString());query(2024);delete(sourceA,false);restore(file("scenario.json"),scenario.length());query(2024);expect(sourceA);note("PASS duplicate restore and recovering deleted record with original ID");
   id("income_add");waitText("新增收入明细");click("11月");click("2月");click("工资薪金");click("稿酬");fill("收入来源 *",sourceC);fill("扣缴义务人 *","UI重构验收模拟单位");fill("收入金额 *","320.25");fill("已申报税额 *","32.05");fill("已缴税额 *","30.01");click("保存明细");listReady();
   JSONArray complete=records(export(true,"全部年度",file("complete.json")));assertEquals(baseline.length()+2,complete.length());assertEquals(sum(baseline,2024,"incomeCents")+182060,sum(complete,2024,"incomeCents"));assertEquals(sum(baseline,2024,"declaredCents")+4715,sum(complete,2024,"declaredCents"));assertEquals(sum(baseline,2024,"paidCents")+4491,sum(complete,2024,"paidCents"));
   query(2024);id("income_filter");click("全部月份");click("11月");expect(filtered(complete,11,null));click("全部所得项目");click("稿酬");expect(filtered(complete,11,"稿酬"));click("11月");click("全部月份");expect(filtered(complete,0,"稿酬"));snap("release-filters");id("income_statistics");expect(money(sum(complete,2024,"incomeCents")));snap("release-statistics");note("PASS monthly and category filters, annual statistics independently checked from JSON");
   android.content.ContentResolver resolver=InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver();android.content.ContentValues values=new android.content.ContentValues();String bad=file("invalid.json");values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,bad);values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"application/json");values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Download/");android.net.Uri uri=resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);try(OutputStream out=resolver.openOutputStream(uri)){out.write("{\"format\":\"tax-learning-backup\",\"version\":1,\"records\":[{}]}".getBytes(StandardCharsets.UTF_8));}openBackupFile(bad);waitText("操作未完成");snap("release-invalid-json");click("知道了");assertEquals(complete.toString(),records(export(true,"全部年度",file("invalid-after.json"))).toString());note("PASS invalid JSON refused with all existing data preserved");
   query(2026);detail("课程演示 · 设计实践");click("删除明细");click("确认删除");listReady();home();query(2026);JSONArray noSeed=records(export(true,"全部年度",file("no-reseed.json")));assertEquals(complete.length()-1,noSeed.length());assertNull(source(noSeed,"课程演示 · 设计实践"));assertEquals(sum(complete,2026,"incomeCents")-320000,sum(noSeed,2026,"incomeCents"));restore(file("complete.json"),complete.length());assertEquals(complete.toString(),records(export(true,"全部年度",file("restored.json"))).toString());note("PASS deleted demo did not regenerate after restart; original dataset restored");
   query(2024);delete(sourceA,false);query(2024);delete(sourceC,false);home();id("nav_4");click("退出登录");click("确认退出");click("使用模拟身份登录");click("使用模拟身份");
   JSONArray finalRows=records(export(true,"全部年度",file("final.json")));assertEquals(baseline.toString(),finalRows.toString());note("PASS final original dataset unchanged after cleanup and demo logout/login");home();id("nav_4");snap("release-profile");note("ALL RELEASE REGRESSION CHECKS PASSED "+prefix);
  }catch(Throwable e){snap("release-failure");note("FAIL "+prefix+" "+e);throw e;}
 }
 @Override @Test public void dynamicYear()throws Exception{
  int expected=Integer.parseInt(InstrumentationRegistry.getArguments().getString("expectedYear","2027"));assertEquals(expected,Calendar.getInstance().get(Calendar.YEAR));home();id("nav_2");click("收入纳税明细");id("query_year");expect(expected+" 年");assertFalse(d.hasObject(By.text((expected+1)+" 年")));UiScrollable years=new UiScrollable(new UiSelector().className("android.widget.ListView"));years.scrollIntoView(new UiSelector().text("2019 年"));expect("2019 年");assertFalse(d.hasObject(By.text("2018 年")));snap("release-years-"+expected);note("PASS dynamic year bounds 2019.."+expected);d.pressBack();
 }
 @Test public void releaseVisuals()throws Exception{
  String shots=InstrumentationRegistry.getArguments().getString("shots","");
  home();snap(shots+"release-home");id("nav_2");snap(shots+"release-services");click("收入纳税明细");waitText("请选择纳税年度");snap(shots+"release-query");query(2026);snap(shots+"release-list");detail("课程演示 · 设计实践");snap(shots+"release-detail");click("修改明细");waitText("修改收入明细");snap(shots+"release-editor");d.pressBack();home();id("nav_4");snap(shots+"release-profile");id("nav_1");snap(shots+"release-todo");id("nav_3");snap(shots+"release-messages");note("PASS signed Release visual captures "+shots);
 }
 @Test public void smallScreenAndLargeFont()throws Exception{
  try{home();id("nav_4");snap("small-profile-top");UiScrollable page=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));page.setSwipeDeadZonePercentage(.22);page.scrollToEnd(10);UiObject2 logout=waitText("退出登录");UiObject2 nav=d.findObject(By.res(pkg+":id/nav_0"));assertTrue("Logout must scroll above floating nav",logout.getVisibleBounds().bottom<nav.getVisibleBounds().top);snap("small-profile-bottom");click("切换关怀版");id("nav_2");snap("small-care-services");id("nav_0");snap("small-care-home");id("nav_4");click("切换标准版");int previous=-1;for(int i=0;i<5;i++){UiObject2 item=d.findObject(By.res(pkg+":id/nav_"+i));assertNotNull(item);android.graphics.Rect bounds=item.getVisibleBounds();assertTrue(bounds.width()>0&&bounds.left>=previous);previous=bounds.right;}note("PASS 360dp screen, system large font, care mode, five nav bounds and reachable footer");}catch(Throwable e){snap("small-failure");note("FAIL small "+e);throw e;}
 }
 @Test public void cleanupInterruptedTest()throws Exception{
  d.executeShellCommand("am force-stop com.android.documentsui");query(2024);delete(sourceA,false);query(2024);delete(sourceC,false);JSONArray rows=records(export(true,"全部年度",file("cleanup.json")));assertEquals(records(savedFile("audit-final.json")).toString(),rows.toString());note("PASS interrupted test cleanup: original record IDs and values retained");
 }
 @Test public void finishRegression()throws Exception{
  try{
   JSONArray baseline=records(savedFile(file("baseline.json")));assertEquals(baseline.toString(),records(export(true,"全部年度",file("pre-login-final-check.json"))).toString());home();id("nav_4");click("退出登录");click("确认退出");expect("未登录");click("使用模拟身份登录");click("使用模拟身份");expect("学习同学");JSONArray finalRows=records(export(true,"全部年度",file("final.json")));assertEquals(baseline.toString(),finalRows.toString());note("PASS final original dataset unchanged after cleanup and demo logout/login");home();id("nav_4");snap("release-profile");note("ALL RELEASE REGRESSION CHECKS PASSED "+prefix);
  }catch(Throwable e){snap("finish-failure");note("FAIL finish "+prefix+" "+e);throw e;}
 }
}
