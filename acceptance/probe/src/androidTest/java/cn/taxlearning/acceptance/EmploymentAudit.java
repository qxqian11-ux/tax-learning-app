package cn.taxlearning.acceptance;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.JSONArray;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EmploymentAudit extends UiReleaseRegression {
 final String a="课程示例电子商务有限责任公司",b="课程示例信息技术服务有限公司",c="课程示例工程装备有限公司";
 final String edited="课程示例电子商务有限责任公司（已编辑）";
 @Override @Before public void setup()throws Exception{
  super.setup();folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"employment/"+prefix);folder.mkdirs();
 }
 void count(int value){
  assertNotNull("Profile count should be "+value,d.wait(Until.findObject(By.res(pkg+":id/profile_employment_count").text(value+"家")),7000));
 }
 void profile()throws Exception{home();id("nav_4");}
 void listReadyEmployment(){assertNotNull(d.wait(Until.findObject(By.res(pkg+":id/employment_add")),7000));d.waitForIdle();SystemClock.sleep(250);}
 void set(String id,String value)throws Exception{
  UiObject2 input=d.findObject(By.res(pkg+":id/"+id));
  if(input==null){new UiScrollable(new UiSelector().className("android.widget.ScrollView")).scrollIntoView(new UiSelector().resourceId(pkg+":id/"+id));input=d.findObject(By.res(pkg+":id/"+id));}
  assertNotNull("Missing field "+id,input);input.setText(value);d.waitForIdle();
 }
 void row(String company,boolean longClick)throws Exception{
  UiObject2 r=d.findObject(By.desc("任职记录："+company));
  if(r==null){new UiScrollable(new UiSelector().className("android.widget.ScrollView")).scrollIntoView(new UiSelector().description("任职记录："+company));r=d.findObject(By.desc("任职记录："+company));}
  assertNotNull(r);if(longClick){android.graphics.Rect bounds=r.getVisibleBounds();int x=bounds.centerX(),y=bounds.centerY();d.executeShellCommand("input touchscreen swipe "+x+" "+y+" "+x+" "+y+" 1200");}else r.click();d.waitForIdle();
 }
 void remove(String company,boolean cancel)throws Exception{
  row(company,true);waitText("删除这条任职记录？");snap("delete-confirmation");
  if(cancel){click("取消");assertTrue(d.hasObject(By.text(company)));row(company,true);waitText("删除这条任职记录？");}
  click("确认删除");listReadyEmployment();assertTrue(d.wait(Until.gone(By.text(company)),6000));
 }
 @Test public void employmentCrudAndProfileCount()throws Exception{
  try{
   JSONArray before=records(export(true,"全部年度",file("income-before.json")));assertEquals(6,before.length());
   profile();count(0);snap("01-profile-empty");id("profile_employment");listReadyEmployment();expect("暂无任职受雇记录");
   id("employment_title");waitText("新增任职受雇");id("employment_save");waitText("操作未完成");click("知道了");
   set("employment_company",a);set("employment_credit","123456789012345678");set("employment_position","工程师");set("employment_start","2020-09-09");id("employment_save");listReadyEmployment();expect(a);
   d.pressBack();count(1);snap("02-profile-one");id("profile_employment");id("employment_add");waitText("新增任职受雇");set("employment_company",b);id("employment_save");listReadyEmployment();expect(b);expect("职务：-");
   d.pressBack();count(2);id("profile_employment");id("employment_add");waitText("新增任职受雇");set("employment_company",c);set("employment_start","2019-01-01");set("employment_end","2025-12-31");id("employment_save");listReadyEmployment();
   d.pressBack();count(3);snap("03-profile-three");id("profile_employment");snap("04-list-three");note("PASS title and add entry, validation, optional fields, count 0 -> 1 -> 2 -> 3 including ended employment");
   row(a,false);waitText("编辑任职受雇");assertEquals(a,d.findObject(By.res(pkg+":id/employment_company")).getText());
   set("employment_company",edited);set("employment_credit","987654321098765432");set("employment_position","项目负责人");set("employment_start","2021-02-03");set("employment_end","2020-01-01");id("employment_save");waitText("操作未完成");click("知道了");set("employment_end","2026-10-31");id("employment_save");listReadyEmployment();expect(edited);expect("职务：项目负责人");expect("任职受雇日期：2021-02-03");expect("离职日期：2026-10-31");snap("05-edited");
   d.pressBack();count(3);id("profile_employment");remove(b,true);d.pressBack();count(2);snap("06-profile-two");
   d.executeShellCommand("am force-stop "+pkg);assertEquals("",d.executeShellCommand("pidof "+pkg).trim());profile();count(2);id("profile_employment");expect(edited);expect(c);assertFalse(d.hasObject(By.text(b)));expect("统一社会信用代码：987654321098765432");expect("任职受雇日期：2021-02-03");expect("离职日期：2026-10-31");snap("07-restart-two");note("PASS editing all fields, unchanged edit count, long-press cancel/confirm delete, restart retains two rows and all edited values");
   remove(c,false);d.pressBack();count(1);id("profile_employment");remove(edited,false);expect("暂无任职受雇记录");d.pressBack();count(0);profile();count(0);id("profile_employment");expect("暂无任职受雇记录");snap("08-empty-after-restart");
   JSONArray after=records(export(true,"全部年度",file("income-after.json")));assertEquals(before.toString(),after.toString());note("PASS delete to zero, zero retained after restart, original six income records exactly unchanged");
   profile();count(0);note("ALL EMPLOYMENT CHECKS PASSED "+prefix);
  }catch(Throwable e){snap("failure");note("FAIL "+e);throw e;}
 }
 @Test public void cleanupInterruptedProbe()throws Exception{
  String previous=InstrumentationRegistry.getArguments().getString("previousRun");assertNotNull(previous);
  JSONArray before=records(savedFile(previous+"-income-before.json"));
  profile();count(3);id("profile_employment");remove(b,false);remove(c,false);remove(edited,false);
  d.pressBack();count(0);JSONArray after=records(export(true,"全部年度",file("income-cleanup.json")));
  assertEquals(before.toString(),after.toString());note("PASS cleanup after interrupted probe: zero employment rows, original six income rows unchanged");
 }
}
