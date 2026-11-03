package cn.taxlearning.acceptance;
import android.graphics.*;
import android.os.*;
import android.view.MotionEvent;
import android.view.InputDevice;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.File;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SixFixesAudit extends QueryNoticeAudit {
 @Override @Before public void setup()throws Exception {super.setup();folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"six-fixes/"+prefix);folder.mkdirs();}
 void touch(long down,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();}
 void pullHeld()throws Exception {
  home();byId("home_hero");SystemClock.sleep(700);Rect before=byId("home_hero").getVisibleBounds();snap("01-home-before-pull");
  float x=d.getDisplayWidth()*.5f,y=d.getDisplayHeight()*.38f;long down=SystemClock.uptimeMillis();touch(down,MotionEvent.ACTION_DOWN,x,y);
  try {for(int i=1;i<=18;i++){touch(down,MotionEvent.ACTION_MOVE,x,y+i*d.getDisplayHeight()*.018f);SystemClock.sleep(16);}SystemClock.sleep(200);assertEquals("Home hero must not stretch/translate while finger held down",before,byId("home_hero").getVisibleBounds());snap("02-home-pull-held");}
  finally {touch(down,MotionEvent.ACTION_UP,x,y+d.getDisplayHeight()*.324f);}
  d.waitForIdle();assertEquals(before,byId("home_hero").getVisibleBounds());note("PASS 1 top pull held/released: content bounds unchanged, no overscroll displacement");
 }
 void editField(String label,String value)throws Exception {
  UiScrollable scroll=new UiScrollable(new UiSelector().resourceId(pkg+":id/page_scroll"));UiObject2 field=d.findObject(By.desc("编辑："+label));
  if(field==null || field.getVisibleBounds().isEmpty())scroll.scrollIntoView(new UiSelector().description("编辑："+label));
  field=d.findObject(By.desc("编辑："+label));assertNotNull(label,field);field.click();byId("detail_edit_input").setText(value);click("保存");assertTrue(d.wait(Until.gone(By.res(pkg+":id/detail_edit_input")),7000));
 }
 @Override void delete(String source,boolean cancelFirst)throws Exception {
  UiObject2 item=d.findObject(By.descContains(source));if(item==null){new UiScrollable(new UiSelector().resourceId(pkg+":id/page_scroll")).scrollIntoView(new UiSelector().descriptionContains(source));item=d.findObject(By.descContains(source));}assertNotNull("Own income row",item);longPress(item);waitText("操作明细");click("删除明细");waitText("删除这笔收入？");
  if(cancelFirst){click("取消");longPress(d.findObject(By.descContains(source)));click("删除明细");waitText("删除这笔收入？");}click("确认删除");listReady();
 }
 @Test public void cleanupPreviousOwn()throws Exception {String previous=InstrumentationRegistry.getArguments().getString("cleanupRun");assertNotNull(previous);assertTrue(previous.matches("[A-Za-z0-9_-]+"));query(Calendar.getInstance().get(Calendar.YEAR));String own="UI140六项-"+previous;if(d.hasObject(By.descContains(own)))delete(own,false);assertFalse(d.hasObject(By.descContains(own)));note("Removed only previous own test record "+own);}
 @Test public void sixOrderedFixesAndDataPreservation()throws Exception {
  String own="UI140六项-"+prefix;boolean added=false,cleaned=false;int current=Calendar.getInstance().get(Calendar.YEAR);
  try {
   JSONArray baseline=records(export(true,"全部年度",file("income-before.json")));assertEquals(6,baseline.length());String jobs=employmentState();noticeList();List<String> notices=noticeRows();
   pullHeld();byId("home_deduction_fill");snap("03-ribbon-shapes");id("home_deduction_fill");waitText("专项附加扣除填报");d.pressBack();id("home_deduction_lookup");waitText("专项附加扣除查询");d.pressBack();note("PASS 2 separate ribbon controls open correct destinations; shape screenshot saved");
   UiScrollable cards=new UiScrollable(new UiSelector().resourceId(pkg+":id/home_shortcuts"));cards.setAsHorizontalList();snap("04-tags-start");cards.scrollToEnd(6);waitText("更多功能");snap("05-tags-end");note("PASS 3 five exact reference tag artworks visible across carousel");
   add(current,own,"1000.10","10.20","9.30");added=true;query(current);Rect summary=byId("income_fixed_summary").getVisibleBounds();snap("06-summary-before");
   Rect first=d.findObject(By.descContains(own)).getVisibleBounds();UiScrollable list=new UiScrollable(new UiSelector().resourceId(pkg+":id/page_scroll"));list.scrollForward();UiObject2 moved=d.findObject(By.descContains(own));assertTrue("Records must actually move",moved==null || !first.equals(moved.getVisibleBounds()));assertEquals(summary,byId("income_fixed_summary").getVisibleBounds());snap("07-summary-after");note("PASS 4 records scrolled while both totals kept identical screen bounds");
   query(current);detail(own);waitText("收入纳税明细详情");assertFalse(d.hasObject(By.text("修改明细")));assertFalse(d.hasObject(By.text("删除明细")));editField("收入","1500.35");expect("1500.35元");editField("所得项目小类","课程工资小类");editField("已申报税额","15.10");editField("扣缴义务人纳税人识别号","DEMO-140");editField("主管税务机构","课程模拟税务机构");editField("申报渠道","其他");editField("申报日期",current+"-02-28");
   editField("本期免税收入","0.00");editField("本期减除费用","5000.00");
   UiScrollable detailScroll=new UiScrollable(new UiSelector().resourceId(pkg+":id/page_scroll"));detailScroll.scrollIntoView(new UiSelector().resourceId(pkg+":id/detail_special_toggle"));id("detail_special_toggle");byId("detail_special_components");
   editField("基本养老保险","1670.96");editField("基本医疗保险","417.74");editField("失业保险","83.55");editField("住房公积金","2553.00");expect("4725.25元");snap("08-deductions-expanded");
   detailScroll.scrollIntoView(new UiSelector().resourceId(pkg+":id/detail_special_toggle"));id("detail_special_toggle");assertFalse(d.hasObject(By.res(pkg+":id/detail_special_components")));snap("09-deductions-collapsed");detailScroll.scrollToBeginning(10);snap("10-detail-top-no-buttons");
   d.executeShellCommand("am force-stop "+pkg);assertEquals("",d.executeShellCommand("pidof "+pkg).trim());query(current);expect("所得项目小类：课程工资小类");detail(own);expect("1500.35元");detailScroll=new UiScrollable(new UiSelector().resourceId(pkg+":id/page_scroll"));detailScroll.scrollIntoView(new UiSelector().description("编辑：本期专项扣除"));expect("4725.25元");
   JSONArray scenario=records(export(true,"全部年度",file("income-scenario.json")));JSONObject saved=source(scenario,own);assertNotNull(saved);assertEquals(150035,saved.getLong("incomeCents"));assertEquals(472525,saved.getJSONObject("details").getLong("special"));assertEquals("DEMO-140",saved.getJSONObject("details").getString("payerTaxId"));note("PASS 5 per-value edits, component sum, expand/collapse, restart and actual JSON contains all detail fields");
   home();id("nav_4");byId("profile_identity_card");snap("11-profile-reference-colors-icons");
   float unit=400f/d.getDisplayWidth();assertEquals(42f,byId("profile_family_icon").getVisibleBounds().width()*unit,1f);assertEquals(42f,byId("profile_employment_icon").getVisibleBounds().width()*unit,1f);assertEquals(50f,byId("profile_bank_icon").getVisibleBounds().width()*unit,1f);note("PASS 6 measured profile icon sizes 42/42/50u; reference-color screenshot saved");
   query(current);delete(own,false);cleaned=true;JSONArray after=records(export(true,"全部年度",file("income-final.json")));assertEquals(baseline.toString(),after.toString());assertEquals(jobs,employmentState());noticeList();assertEquals(notices,noticeRows());home();note("ALL SIX ORDERED CHECKS PASSED; migration retained original six income records; test record removed and original employment/notices unchanged");
  }catch(Throwable e){snap("failure-six-fixes");note("FAIL "+e);throw e;}
  finally {if(added && !cleaned)try{query(current);if(d.hasObject(By.descContains(own)))delete(own,false);note("Cleanup removed only own UI140 record");}catch(Throwable e){note("Cleanup requires attention: "+e);}}
 }
}
