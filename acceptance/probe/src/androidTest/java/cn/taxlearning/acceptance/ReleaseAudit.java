package cn.taxlearning.acceptance;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ReleaseAudit {
 final String pkg=InstrumentationRegistry.getArguments().getString("targetPackage","cn.taxlearning.ledger");
 UiDevice d;
 File folder;
 @Before public void setup() throws Exception {
  d=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
  folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"audit"); folder.mkdirs();
 }
 void note(String s) throws Exception { try(FileOutputStream f=new FileOutputStream(new File(folder,"steps.log"),true)){f.write((new Date()+" "+s+"\n").getBytes(StandardCharsets.UTF_8));} }
 void snap(String name) throws Exception { d.waitForIdle();SystemClock.sleep(900);d.dumpWindowHierarchy(new File(folder,name+".xml"));SystemClock.sleep(300);d.takeScreenshot(new File(folder,name+".png")); }
 UiObject2 waitText(String text) { UiObject2 o=d.wait(Until.findObject(By.text(text)),6000); assertNotNull("Missing text: "+text,o);return o; }
 UiObject2 find(String text) throws Exception {
  UiObject2 o=d.findObject(By.text(text)); if(o!=null)return o;
  UiScrollable s=new UiScrollable(new UiSelector().scrollable(true));
  if(s.exists())s.scrollIntoView(new UiSelector().text(text));
  return waitText(text);
 }
 void click(String text) throws Exception {for(int i=0;i<3;i++){try{find(text).click();d.waitForIdle();SystemClock.sleep(350);return;}catch(StaleObjectException e){if(i==2)throw e;SystemClock.sleep(500);}}}
 void home() throws Exception { d.executeShellCommand("am force-stop "+pkg); note("force-stop PID after = "+d.executeShellCommand("pidof "+pkg).trim()); d.executeShellCommand("am start -W -n "+pkg+"/.MainActivity");waitText("税知账"); }
 void query(int year) throws Exception {
  home();click("去查询  →");waitText("选择查询条件");
  d.findObject(By.clazz("android.widget.Spinner")).click();click(year+" 年");click("查询");waitText("收入纳税明细 · "+year);SystemClock.sleep(300);
 }
 void top() throws Exception {UiScrollable s=new UiScrollable(new UiSelector().scrollable(true));if(s.exists())s.scrollToBeginning(12);}
 void expect(String text) throws Exception { assertNotNull(find(text)); }
 AccessibilityNodeInfo hint(AccessibilityNodeInfo n,String key) {
  if(n==null)return null;
  if(n.getClassName()!=null && n.getClassName().toString().contains("EditText") && n.getHintText()!=null && n.getHintText().toString().equals(key))return n;
  for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=hint(n.getChild(i),key);if(found!=null)return found;}return null;
 }
 void fill(String label,String value) throws Exception {
  AccessibilityNodeInfo n=null;
  for(int i=0;i<12;i++) {n=hint(InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow(),label);if(n!=null)break;
   new UiScrollable(new UiSelector().scrollable(true)).scrollForward(); }
  assertNotNull("Missing input hint "+label,n);
  Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);
  assertTrue(n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b));d.waitForIdle();
 }
 void add(int year,String source,String income,String declared,String paid) throws Exception {
  query(year);click("＋ 新增收入明细");waitText("新增收入明细");
  fill("收入来源 *",source);fill("扣缴义务人 *","课程验收模拟单位");fill("收入金额 *",income);fill("已申报税额 *",declared);fill("已缴税额 *",paid);
  click("保存明细");waitText("收入纳税明细 · "+year);expect(source);note("UI added "+year+" "+source+" "+income+"/"+declared+"/"+paid);
 }
 void detail(String source) throws Exception {click(source);waitText("收入纳税明细详情");}
 void delete(String source,boolean cancelFirst) throws Exception {
  detail(source);click("删除明细");waitText("删除这笔收入？");
  if(cancelFirst){click("取消");expect(source);note("Delete cancellation retained "+source);click("删除明细");}
  click("确认删除");waitText("收入纳税明细 · "+(source.contains("2025")?2025:2024));
 }
 @Test public void coreAndPersistence() throws Exception {
  try {
   home();expect("把每一份收入，记清楚");snap("01-home");click("我的");expect("学习同学");expect("个人信息");snap("02-mine");click("首页");expect("把每一份收入，记清楚");note("PASS homepage/mine/bottom navigation");
   click("去查询  →");waitText("选择查询条件");d.findObject(By.clazz("android.widget.Spinner")).click();
   expect("2019 年");expect("2026 年");assertFalse(d.hasObject(By.text("2018 年")));assertFalse(d.hasObject(By.text("2027 年")));snap("03-years-2026");d.pressBack();note("PASS year range 2019..2026 on device year 2026");
   query(2024);expect("暂时没有收入明细");query(2025);expect("暂时没有收入明细");
   add(2024,"验收2024工资","1000.10","10.20","9.30");detail("验收2024工资");expect("¥ 1,000.10");expect("¥ 10.20");expect("¥ 9.30");snap("04-detail-before");
   add(2025,"验收2025工资","2000.20","20.40","19.50");
   query(2024);assertFalse(d.hasObject(By.text("验收2025工资")));detail("验收2024工资");click("修改明细");waitText("修改收入明细");
   fill("收入金额 *","1500.35");fill("已申报税额 *","15.10");fill("已缴税额 *","14.90");click("保存明细");waitText("收入纳税明细详情");expect("¥ 1,500.35");note("PASS UI edit 2024 to 1500.35/15.10/14.90");
   query(2025);delete("验收2025工资",true);expect("暂时没有收入明细");note("PASS UI delete 2025");
   home();note("PERSISTENCE full process stopped and restarted");
   query(2024);expect("1,500.35");expect("已申报税额  ¥ 15.10");expect("已缴税额      ¥ 14.90");expect("验收2024工资");top();snap("05-restarted-2024");
   query(2025);expect("0.00");expect("暂时没有收入明细");top();snap("06-restarted-2025");note("PASS requested 2024/2025 scenario, totals 1500.35/15.10/14.90 and 0/0/0");
   home();click("我的");click("数据备份与恢复");click("备份为 JSON");click("全部年度");SystemClock.sleep(1000);snap("07-saf-create");note("PHASE1 PASSED; SAF create dialog opened");
  } catch(Throwable e){snap("failure-core");note("FAIL "+e);throw e;}
 }
 @Test public void snapshot() throws Exception {snap("current");}
 void backupPage() throws Exception {home();click("我的");click("数据备份与恢复");waitText("数据备份与恢复");}
 String savedFile(String name) throws Exception {
  String data="";
  for(int i=0;i<30;i++){try(android.os.ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("cat /sdcard/Download/"+name);InputStream stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int length;while((length=stream.read(buffer))!=-1)bytes.write(buffer,0,length);data=new String(bytes.toByteArray(),StandardCharsets.UTF_8);}if(data.startsWith("{")||data.startsWith("\ufeff"))break;SystemClock.sleep(200);}
  try(FileOutputStream out=new FileOutputStream(new File(folder,name))){out.write(data.getBytes(StandardCharsets.UTF_8));}return data;
 }
 void saveDocument(String name) throws Exception {
  waitText("SAVE");UiObject2 filename=d.findObject(By.res("android:id/title").clazz("android.widget.EditText"));assertNotNull(filename);filename.setText(name);assertEquals(name,filename.getText());snap("saf-save-"+name);click("SAVE");
  waitText("数据备份与恢复");SystemClock.sleep(600);
 }
 String export(boolean json,String scope,String name) throws Exception {
  backupPage();click(json?"备份为 JSON":"导出 CSV 明细");click(scope);saveDocument(name);return savedFile(name);
 }
 void openBackupFile(String name) throws Exception {
  backupPage();click("选择 JSON 备份文件");
  UiObject2 roots=d.wait(Until.findObject(By.desc("Show roots")),5000);assertNotNull(roots);roots.click();d.waitForIdle();SystemClock.sleep(600);click("Downloads");SystemClock.sleep(600);click(name);SystemClock.sleep(500);
 }
 JSONArray records(String raw) throws Exception {return new JSONObject(raw.replace("\ufeff","")).getJSONArray("records");}
 long sum(JSONArray a,int year,String key) throws Exception {long total=0;for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);if(r.getInt("year")==year)total+=r.getLong(key);}return total;}
 void restore(String name,int count) throws Exception {openBackupFile(name);waitText("恢复 "+count+" 条模拟记录？");snap("restore-confirm");click("确认恢复");SystemClock.sleep(800);}
 @Test public void filesFiltersAndStats() throws Exception {
  try {
   // Reuse the real JSON already exported through SAF in phase 1.
   JSONArray before=records(savedFile("audit-before.json"));assertEquals(5,before.length());assertEquals(150035,sum(before,2024,"incomeCents"));assertEquals(0,sum(before,2025,"incomeCents"));note("PASS JSON all-year export through SAF: 5 records, 2024=150035 cents, 2025=0");
   String csv=export(false,"2024 年","audit-2024-verified.csv");assertTrue(csv.startsWith("\ufeff"));assertTrue(csv.contains("验收2024工资"));assertTrue(csv.contains("1500.35"));assertTrue(csv.contains("15.10"));assertTrue(csv.contains("14.90"));assertFalse(csv.contains("验收2025工资"));note("PASS CSV year-2024 export through SAF, UTF-8 BOM and actual amounts checked");
   restore("audit-before.json",5);JSONArray duplicate=records(export(true,"全部年度","audit-duplicate.json"));assertEquals(before.toString(),duplicate.toString());note("PASS duplicate JSON restore leaves all 5 records identical");
   query(2024);delete("验收2024工资",false);expect("暂时没有收入明细");restore("audit-before.json",5);query(2024);expect("1,500.35");expect("验收2024工资");note("PASS restoring deleted record via system file picker");
   query(2024);click("＋ 新增收入明细");waitText("新增收入明细");click("11月");click("2月");click("工资薪金");click("稿酬");
   fill("收入来源 *","验收2024稿酬");fill("扣缴义务人 *","课程验收模拟单位");fill("收入金额 *","320.25");fill("已申报税额 *","32.05");fill("已缴税额 *","30.01");click("保存明细");waitText("收入纳税明细 · 2024");expect("1,820.60");expect("已申报税额  ¥ 47.15");expect("已缴税额      ¥ 44.91");
   click("全部月份");click("11月");expect("筛选结果 1 笔 · 收入 ¥ 1,500.35");snap("08-filter-november");
   click("全部所得项目");click("稿酬");expect("筛选结果 0 笔 · 收入 ¥ 0.00");top();expect("1,820.60");note("PASS month/category combined filter and unchanged annual total");
   click("11月");click("全部月份");expect("筛选结果 1 笔 · 收入 ¥ 320.25");snap("09-filter-category");
   top();click("查看年度统计");expect("1,820.60");expect("2月   ¥ 320.25");snap("10-monthly-stats");expect("11月   ¥ 1,500.35");expect("工资薪金   ¥ 1,500.35");expect("稿酬   ¥ 320.25");snap("11-category-stats");note("PASS annual, filtered, monthly, category totals 1820.60/47.15/44.91");
   JSONArray after=records(export(true,"全部年度","audit-after.json"));assertEquals(6,after.length());assertEquals(182060,sum(after,2024,"incomeCents"));assertEquals(4715,sum(after,2024,"declaredCents"));assertEquals(4491,sum(after,2024,"paidCents"));note("PASS actual final JSON independently confirms UI totals");
  }catch(Throwable e){snap("failure-files");note("FAIL "+e);throw e;}
 }
 @Test public void recoverySeedAndIcon() throws Exception {
  try {
   JSONArray baseline=records(export(true,"全部年度","audit-validation-before.json"));assertEquals(6,baseline.length());
   android.content.ContentResolver resolver=InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver();
   android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"audit-invalid.json");values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"application/json");values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Download/");
   android.net.Uri uri=resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
   try(OutputStream os=resolver.openOutputStream(uri)){os.write("{\"format\":\"tax-learning-backup\",\"version\":1,\"records\":[{}]}".getBytes(StandardCharsets.UTF_8));}
   openBackupFile("audit-invalid.json");waitText("操作未完成");snap("12-invalid-restore");click("知道了");
   JSONArray checked=records(export(true,"全部年度","audit-validation-after.json"));assertEquals(baseline.toString(),checked.toString());note("PASS malformed JSON rejected; all original records unchanged");
   query(2026);detail("课程演示 · 设计实践");click("删除明细");click("确认删除");waitText("收入纳税明细 · 2026");home();query(2026);expect("40,200.00");snap("13-seed-not-regenerated");
   JSONArray noSeed=records(export(true,"全部年度","audit-no-reseed.json"));assertEquals(5,noSeed.length());assertEquals(4020000,sum(noSeed,2026,"incomeCents"));note("PASS deleted first-install demo stayed deleted after process restart; remaining annual income 40200.00");
   restore("audit-after.json",6);JSONArray restored=records(export(true,"全部年度","audit-final.json"));assertEquals(baseline.toString(),restored.toString());note("PASS restored test baseline, exact record IDs and values preserved");
   android.graphics.drawable.Drawable icon=InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageManager().getApplicationIcon(pkg);
   android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(256,256,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);icon.setBounds(0,0,256,256);icon.draw(canvas);
   try(FileOutputStream output=new FileOutputStream(new File(folder,"14-release-icon.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();
   d.pressHome();SystemClock.sleep(500);d.swipe(d.getDisplayWidth()/2,d.getDisplayHeight()*9/10,d.getDisplayWidth()/2,d.getDisplayHeight()/4,25);SystemClock.sleep(1000);snap("15-launcher-icon");note("PASS installed Release icon loaded by PackageManager; launcher screenshot retained");
  }catch(Throwable e){snap("failure-recovery");note("FAIL "+e);throw e;}
 }
 @Test public void dynamicYear() throws Exception {
  int expected=Integer.parseInt(InstrumentationRegistry.getArguments().getString("expectedYear","2027"));
  assertEquals(expected,Calendar.getInstance().get(Calendar.YEAR));home();click("去查询  →");waitText("选择查询条件");d.findObject(By.clazz("android.widget.Spinner")).click();expect(expected+" 年");expect("2019 年");assertFalse(d.hasObject(By.text((expected+1)+" 年")));assertFalse(d.hasObject(By.text("2018 年")));snap("16-years-"+expected);note("PASS dynamic year: device year "+expected+", query bounds 2019.."+expected);d.pressBack();
 }
 @Test public void installedIcon() throws Exception {
  android.graphics.drawable.Drawable icon=InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageManager().getApplicationIcon(pkg);
  android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(256,256,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);icon.setBounds(0,0,256,256);icon.draw(canvas);
  try(FileOutputStream output=new FileOutputStream(new File(folder,"14-release-icon.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();
  d.pressHome();SystemClock.sleep(500);d.swipe(d.getDisplayWidth()/2,d.getDisplayHeight()*9/10,d.getDisplayWidth()/2,d.getDisplayHeight()/4,25);SystemClock.sleep(1000);snap("15-launcher-icon");note("PASS installed Release icon loaded by PackageManager; launcher screenshot retained");
 }
}
