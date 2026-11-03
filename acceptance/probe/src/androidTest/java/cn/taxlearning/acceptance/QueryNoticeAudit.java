package cn.taxlearning.acceptance;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

/** Run named methods only; inherited historical methods target previous layouts. */
@RunWith(AndroidJUnit4.class)
public class QueryNoticeAudit extends UiReleaseRegression {
 @Override @Before public void setup()throws Exception{
  super.setup();folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"query-notices/"+prefix);folder.mkdirs();
 }
 UiObject2 byId(String name){UiObject2 o=d.wait(Until.findObject(By.res(pkg+":id/"+name)),7000);assertNotNull("Missing id "+name,o);return o;}
 void visibleId(String name)throws Exception{
  UiObject2 item=d.findObject(By.res(pkg+":id/"+name));
  if(item==null || item.getVisibleBounds().isEmpty()){
   UiScrollable scroll=new UiScrollable(new UiSelector().resourceId(pkg+":id/page_scroll"));
   if(scroll.exists())scroll.scrollIntoView(new UiSelector().resourceId(pkg+":id/"+name));
  }
  id(name);
 }
 void openQuery()throws Exception{home();id("nav_2");click("收入纳税明细");waitText("请选择纳税年度");}
 void chooseWheelYear(int year)throws Exception{
  UiScrollable wheelScroll=new UiScrollable(new UiSelector().resourceId(pkg+":id/query_year_scroll"));
  assertTrue("Expected wheel scrolling surface",wheelScroll.exists());wheelScroll.setSwipeDeadZonePercentage(.15);
  wheelScroll.scrollIntoView(new UiSelector().text(String.valueOf(year)));
  UiObject2 wheel=byId("query_year_wheel"),value=wheel.findObject(By.text(String.valueOf(year)));
  assertNotNull("Wheel year "+year,value);value.click();d.waitForIdle();
  assertNotNull("Wheel selected year "+year,d.wait(Until.findObject(By.res(pkg+":id/query_year_wheel").desc("纳税年度，当前选择："+year)),7000));
 }
 void yearIs(int year){assertNotNull("Applied year "+year,byId("query_year").findObject(By.text(String.valueOf(year))));}
 @Override void query(int year)throws Exception{openQuery();id("query_year");chooseWheelYear(year);id("query_year_confirm");yearIs(year);visibleId("query_submit");listReady();}
 @Override void listReady(){byId("income_title_add");d.waitForIdle();SystemClock.sleep(250);}
 @Override void add(int year,String source,String income,String declared,String paid)throws Exception{
  query(year);id("income_title_add");waitText("新增收入明细");fill("收入来源 *",source);fill("扣缴义务人 *","UI130验收模拟单位");fill("收入金额 *",income);fill("已申报税额 *",declared);fill("已缴税额 *",paid);click("保存明细");listReady();expect(source);
 }
 void assertNoIncomeFooter(){
  for(String name:new String[]{"income_filter","income_statistics","income_add"})assertFalse("Removed footer id "+name,d.hasObject(By.res(pkg+":id/"+name)));
  assertFalse(d.hasObject(By.text("筛选")));assertFalse(d.hasObject(By.text("年度统计")));assertFalse(d.hasObject(By.text("＋ 新增")));
 }
 String employmentState()throws Exception{
  home();id("nav_4");String count=byId("profile_employment_count").getText();
  for(int i=0;i<20 && count.equals("…");i++){SystemClock.sleep(150);count=byId("profile_employment_count").getText();}
  assertFalse("Employment count must finish loading",count.equals("…"));id("profile_employment");waitText("任职受雇");
  List<String> rows=new ArrayList<>();for(UiObject2 row:d.findObjects(By.descStartsWith("任职记录：")))rows.add(row.getContentDescription());
  if(count.equals("0家"))assertTrue(d.hasObject(By.text("暂无任职受雇记录")));
  Collections.sort(rows);return count+"|"+rows.toString();
 }
 void totals(JSONArray baseline,int year,long income,long declared,long paid)throws Exception{
  expect(plain(sum(baseline,year,"incomeCents")+income)+"元");expect(plain(sum(baseline,year,"declaredCents")+declared)+"元");
  id("income_summary_help");waitText(year+" 年度汇总");UiObject2 message=d.wait(Until.findObject(By.res("android:id/message")),6000);assertNotNull(message);
  assertTrue(message.getText(),message.getText().contains("已缴 "+plain(sum(baseline,year,"paidCents")+paid)+" 元"));click("知道了");
 }
 String plain(long cents){return java.math.BigDecimal.valueOf(cents,2).toPlainString();}
 void stateFile(String name,String value)throws Exception{try(FileOutputStream out=new FileOutputStream(new File(folder,name))){out.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}}
 String priorState(String previous,String name)throws Exception{
  File file=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"query-notices/"+previous+"/"+name);
  try(InputStream input=new FileInputStream(file);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);return new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);}
 }
 @Test public void queryAndIncomeChanges()throws Exception{
  String a="UI130核验2024-"+prefix,b="UI130核验2025-"+prefix;
  try{
   JSONArray before=records(export(true,"全部年度",file("income-before.json")));assertEquals(6,before.length());String employmentBefore=employmentState();stateFile("query-employment-before.txt",employmentBefore);
   openQuery();int current=Calendar.getInstance().get(Calendar.YEAR);yearIs(current);expect("其他类型");assertEquals(4,d.findObjects(By.clazz("android.widget.CheckBox")).size());snap("query-01-collapsed");
   id("query_year");chooseWheelYear(2019);assertNull(byId("query_year_wheel").findObject(By.text("2018")));snap("query-02-wheel-min");
   chooseWheelYear(current);assertNull(byId("query_year_wheel").findObject(By.text(String.valueOf(current+1))));chooseWheelYear(2022);snap("query-03-wheel-center");id("query_year_cancel");yearIs(current);
   id("query_year");chooseWheelYear(2024);id("query_year_confirm");yearIs(2024);note("PASS native wheel min 2019, current-year max, cancel does not apply, confirm applies");
   visibleId("query_category_toggle");expect("经营所得");expect("偶然所得");assertEquals(9,d.findObjects(By.clazz("android.widget.CheckBox")).size());snap("query-04-expanded");visibleId("query_category_toggle");expect("其他类型");assertEquals(4,d.findObjects(By.clazz("android.widget.CheckBox")).size());
   visibleId("query_submit");listReady();assertNoIncomeFooter();byId("income_summary_help");snap("query-05-list-without-footer");
   add(2024,a,"1000.10","10.20","9.30");query(2024);totals(before,2024,100010,1020,930);assertNoIncomeFooter();snap("query-06-added-2024");
   add(2025,b,"2000.20","20.40","19.50");query(2024);detail(a);click("修改明细");fill("收入金额 *","1500.35");fill("已申报税额 *","15.10");fill("已缴税额 *","14.90");click("保存明细");waitText("收入纳税明细详情");expect("1500.35元");snap("query-07-edited");
   query(2025);delete(b,true);d.executeShellCommand("am force-stop "+pkg);assertEquals("",d.executeShellCommand("pidof "+pkg).trim());
   query(2024);expect(a);assertFalse(d.hasObject(By.descContains(b)));totals(before,2024,150035,1510,1490);snap("query-08-restart-2024");query(2025);assertFalse(d.hasObject(By.descContains(b)));totals(before,2025,0,0,0);snap("query-09-deleted-2025");
   JSONArray scenario=records(export(true,"全部年度",file("income-scenario.json")));assertEquals(before.length()+1,scenario.length());assertNotNull(source(scenario,a));assertNull(source(scenario,b));
   assertEquals(sum(before,2024,"incomeCents")+150035,sum(scenario,2024,"incomeCents"));assertEquals(sum(before,2024,"declaredCents")+1510,sum(scenario,2024,"declaredCents"));assertEquals(sum(before,2024,"paidCents")+1490,sum(scenario,2024,"paidCents"));
   query(2024);delete(a,false);JSONArray after=records(export(true,"全部年度",file("income-final.json")));assertEquals(before.toString(),after.toString());assertEquals(employmentBefore,employmentState());
   note("ALL QUERY/INCOME CHECKS PASSED: title-to-add, no footer, help dialog, years isolated, edits/deletion survive force-stop, original income/employment unchanged "+prefix);
  }catch(Throwable e){snap("failure-query");note("FAIL query "+e);throw e;}
 }

 AccessibilityNodeInfo nodeWithId(AccessibilityNodeInfo node,String id){
  if(node==null)return null;if(id.equals(node.getViewIdResourceName()))return node;
  for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=nodeWithId(node.getChild(i),id);if(found!=null)return found;}return null;
 }
 AccessibilityNodeInfo marqueeNode(){AccessibilityNodeInfo node=nodeWithId(InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow(),pkg+":id/notice_marquee");assertNotNull("Marquee accessibility node",node);node.refresh();return node;}
 void saveBitmap(Bitmap bitmap,String name)throws Exception{try(FileOutputStream stream=new FileOutputStream(new File(folder,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}}
 void verifyMarqueeMotion(String label)throws Exception{
  UiObject2 marquee=byId("notice_marquee");Rect bounds=marquee.getVisibleBounds();assertFalse(bounds.isEmpty());
  AccessibilityNodeInfo node=marqueeNode();
  for(int i=0;i<60 && !node.getExtras().getBoolean("cn.taxlearning.notice.running");i++){SystemClock.sleep(100);node=marqueeNode();}
  float first=node.getExtras().getFloat("cn.taxlearning.notice.offsetPx",Float.NaN);assertFalse(Float.isNaN(first));assertTrue("Marquee must start drawing once data loads",node.getExtras().getBoolean("cn.taxlearning.notice.running"));
  Bitmap full1=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(full1);Bitmap crop1=Bitmap.createBitmap(full1,bounds.left,bounds.top,bounds.width(),bounds.height());saveBitmap(crop1,label+"-motion-before.png");full1.recycle();
  SystemClock.sleep(800);node=marqueeNode();float second=node.getExtras().getFloat("cn.taxlearning.notice.offsetPx",Float.NaN);assertFalse(Float.isNaN(second));assertTrue("Actual scroll offset must advance",Math.abs(second-first)>.1f);
  Bitmap full2=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(full2);Bitmap crop2=Bitmap.createBitmap(full2,bounds.left,bounds.top,bounds.width(),bounds.height());saveBitmap(crop2,label+"-motion-after.png");full2.recycle();
  int changed=0;for(int y=0;y<crop1.getHeight();y++)for(int x=0;x<crop1.getWidth();x++)if(crop1.getPixel(x,y)!=crop2.getPixel(x,y))changed++;
  crop1.recycle();crop2.recycle();assertTrue("Rendered marquee pixels must visibly move",changed>10);note("PASS actual marquee offset "+first+" -> "+second+", changed crop pixels="+changed);
 }
 void longPress(UiObject2 object)throws Exception{Rect bounds=object.getVisibleBounds();assertFalse(bounds.isEmpty());int x=bounds.centerX(),y=bounds.centerY();d.executeShellCommand("input touchscreen swipe "+x+" "+y+" "+x+" "+y+" 1200");d.waitForIdle();}
 void noticeList()throws Exception{home();longPress(byId("home_notice"));byId("notice_add");waitText("广播提醒");}
 List<String> noticeRows(){List<String> rows=new ArrayList<>();for(UiObject2 row:d.findObjects(By.descStartsWith("广播提醒：")))rows.add(row.getContentDescription());Collections.sort(rows);return rows;}
 UiObject2 noticeRow(String content)throws Exception{
  UiObject2 row=d.findObject(By.desc("广播提醒："+content));
  if(row==null){new UiScrollable(new UiSelector().resourceId(pkg+":id/page_scroll")).scrollIntoView(new UiSelector().description("广播提醒："+content));row=d.findObject(By.desc("广播提醒："+content));}
  assertNotNull("Notice row "+content,row);return row;
 }
 void addNotice(String content)throws Exception{id("notice_add");waitText("新增广播提醒");byId("notice_content").setText(content);id("notice_save");byId("notice_add");assertNotNull(noticeRow(content));}
 void deleteNotice(String content,boolean cancel)throws Exception{
  longPress(noticeRow(content));waitText("删除这条广播？");snap("notice-delete-confirmation");
  if(cancel){click("取消");assertNotNull(noticeRow(content));longPress(noticeRow(content));waitText("删除这条广播？");}
  click("确认删除");byId("notice_add");assertTrue(d.wait(Until.gone(By.desc("广播提醒："+content)),6000));
 }
 @Test public void noticeManagementAndRestart()throws Exception{
  String a="UI130广播A-"+prefix+"：请核对本机收入记录",b="UI130广播B-"+prefix+"：妥善保存学习记录",edited=a+"（已修改）";
  try{
   JSONArray incomeBefore=records(export(true,"全部年度",file("notice-income-before.json")));String employmentBefore=employmentState();stateFile("notice-employment-before.txt",employmentBefore);
   home();verifyMarqueeMotion("notice-01");snap("notice-02-home");longPress(byId("home_notice"));byId("notice_add");List<String> before=noticeRows();assertFalse(before.isEmpty());stateFile("notice-before.json",new JSONArray(before).toString());snap("notice-03-initial-list");
   id("notice_add");id("notice_save");waitText("操作未完成");click("知道了");byId("notice_content").setText(a);id("notice_save");byId("notice_add");assertNotNull(noticeRow(a));addNotice(b);
   noticeRow(a).click();waitText("编辑广播提醒");assertEquals(a,byId("notice_content").getText());byId("notice_content").setText(edited);id("notice_save");byId("notice_add");assertNotNull(noticeRow(edited));assertFalse(d.hasObject(By.desc("广播提醒："+a)));snap("notice-04-edited-list");
   deleteNotice(b,true);d.executeShellCommand("am force-stop "+pkg);assertEquals("",d.executeShellCommand("pidof "+pkg).trim());noticeList();assertNotNull(noticeRow(edited));assertFalse(d.hasObject(By.desc("广播提醒："+b)));snap("notice-05-restart");
   home();verifyMarqueeMotion("notice-06");deleteNoticeFromHome(edited);assertEquals(before,noticeRows());home();verifyMarqueeMotion("notice-07-restored-original");
   JSONArray incomeAfter=records(export(true,"全部年度",file("notice-income-final.json")));assertEquals(incomeBefore.toString(),incomeAfter.toString());assertEquals(employmentBefore,employmentState());
   note("ALL NOTICE CHECKS PASSED: real motion, long-press entry, validation, create/edit/delete/cancel, force-stop retains edits/deletion, original notice/income/employment state restored "+prefix);
  }catch(Throwable e){snap("failure-notice");note("FAIL notice "+e);throw e;}
 }
 void deleteNoticeFromHome(String content)throws Exception{noticeList();deleteNotice(content,false);}
 boolean ownNoticeExists(String content)throws Exception{
  if(d.hasObject(By.desc("广播提醒："+content)))return true;
  UiScrollable scroll=new UiScrollable(new UiSelector().resourceId(pkg+":id/page_scroll"));
  if(scroll.exists())scroll.scrollIntoView(new UiSelector().description("广播提醒："+content));
  return d.hasObject(By.desc("广播提醒："+content));
 }
 @Test public void presentationOnly()throws Exception{
  try{
   openQuery();int current=Calendar.getInstance().get(Calendar.YEAR);yearIs(current);snap("final-query-collapsed");
   id("query_year");chooseWheelYear(2022);snap("final-year-wheel");id("query_year_cancel");yearIs(current);
   id("query_year");chooseWheelYear(2024);id("query_year_confirm");yearIs(2024);visibleId("query_submit");listReady();assertNoIncomeFooter();byId("income_summary_help");snap("final-income-list");
   note("PASS presentation-only captures: collapsed query, centered native year dialog, cancel/confirm, footer-free income list; no records changed");
  }catch(Throwable e){snap("failure-presentation");note("FAIL presentation "+e);throw e;}
 }
 /** Recovery only: -e cleanupRun previous-prefix -e cleanupKind query|notice -e runPrefix a-new-prefix. */
 @Test public void cleanupOwnRecords()throws Exception{
  String previous=InstrumentationRegistry.getArguments().getString("cleanupRun"),kind=InstrumentationRegistry.getArguments().getString("cleanupKind");
  assertNotNull(previous);assertTrue(previous.matches("[A-Za-z0-9_-]+"));assertFalse("Use a separate cleanup evidence prefix",previous.equals(prefix));assertTrue("query".equals(kind)||"notice".equals(kind));
  try{
   String baselineFile=previous+("query".equals(kind)?"-income-before.json":"-notice-income-before.json");JSONArray before=records(savedFile(baselineFile));
   if("query".equals(kind)){
    String a="UI130核验2024-"+previous,b="UI130核验2025-"+previous;
    query(2024);if(d.hasObject(By.descContains(a))){delete(a,false);note("CLEANUP removed own "+a);}
    query(2025);if(d.hasObject(By.descContains(b))){delete(b,false);note("CLEANUP removed own "+b);}
   }else{
    String a="UI130广播A-"+previous+"：请核对本机收入记录",b="UI130广播B-"+previous+"：妥善保存学习记录";
    noticeList();for(String content:new String[]{a,a+"（已修改）",b})if(ownNoticeExists(content)){deleteNotice(content,false);note("CLEANUP removed own "+content);}
    List<String> baselineRows=new ArrayList<>();JSONArray savedRows=new JSONArray(priorState(previous,"notice-before.json"));for(int i=0;i<savedRows.length();i++)baselineRows.add(savedRows.getString(i));Collections.sort(baselineRows);assertEquals(baselineRows,noticeRows());
   }
   JSONArray after=records(export(true,"全部年度",file("cleanup-income-final.json")));assertEquals(before.toString(),after.toString());assertEquals(priorState(previous,kind+"-employment-before.txt"),employmentState());
   note("PASS cleanup removed only own "+kind+" records; original income/employment state unchanged");
  }catch(Throwable e){snap("failure-cleanup");note("FAIL cleanup "+e);throw e;}
 }
}
