package cn.taxlearning.acceptance;

import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UiRebuildAudit {
 final String pkg="cn.taxlearning.ledger.debug";
 UiDevice d;File folder;
 @Before public void setup(){d=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"ui-rebuild");folder.mkdirs();}
 void note(String s)throws Exception{try(FileOutputStream f=new FileOutputStream(new File(folder,"steps.log"),true)){f.write((new java.util.Date()+" "+s+"\n").getBytes(StandardCharsets.UTF_8));}}
 UiObject2 expect(String s){UiObject2 o=d.wait(Until.findObject(By.text(s)),7000);assertNotNull("Missing "+s,o);return o;}
 void click(String s)throws Exception{for(int i=0;i<6;i++){try{UiObject2 o=d.findObject(By.text(s));if(o==null){UiScrollable sc=new UiScrollable(new UiSelector().scrollable(true));if(sc.exists())sc.scrollIntoView(new UiSelector().text(s));o=expect(s);}if(d.hasObject(By.res(pkg+":id/nav_0")) && o.getVisibleBounds().centerY()>d.getDisplayHeight()*.78){d.swipe(d.getDisplayWidth()/2,d.getDisplayHeight()*7/10,d.getDisplayWidth()/2,d.getDisplayHeight()*4/10,30);d.waitForIdle();continue;}o.click();d.waitForIdle();SystemClock.sleep(350);return;}catch(StaleObjectException e){if(i==5)throw e;}}fail("Could not safely click "+s);}
 void id(String name){UiObject2 o=d.wait(Until.findObject(By.res(pkg+":id/"+name)),7000);assertNotNull("Missing id "+name,o);o.click();d.waitForIdle();SystemClock.sleep(400);}
 void snap(String name)throws Exception{d.waitForIdle();SystemClock.sleep(800);d.dumpWindowHierarchy(new File(folder,name+".xml"));d.takeScreenshot(new File(folder,name+".png"));}
 void back(){d.pressBack();d.waitForIdle();SystemClock.sleep(400);}
 @Test public void stageOneProfileAndNavigation()throws Exception{
  try{
   d.executeShellCommand("am force-stop "+pkg);d.executeShellCommand("am start -W -n "+pkg+"/cn.taxlearning.ledger.MainActivity");
   for(String title:new String[]{"首页","待办","办&查","消息","我的"})expect(title);
   id("nav_4");expect("学习同学");expect("4人");expect("3家");expect("1张");snap("stage1-profile");note("PASS five-item nav and reference profile structure");
   id("profile_family");expect("家庭成员信息");back();id("profile_employment");expect("任职受雇信息");back();id("profile_bank");expect("银行卡信息");back();id("profile_personal");expect("个人信息");back();note("PASS all four identity/shortcut destinations");
   id("profile_identity_toggle");snap("stage1-identity-visible");id("profile_identity_toggle");
   id("profile_care");expect("切换标准版");snap("stage1-care");id("profile_care");expect("切换关怀版");note("PASS care mode and masked identity toggle");
   id("profile_security");expect("安全中心");click("数据备份与恢复");expect("备份为 JSON");back();back();
   id("profile_help");expect("帮助中心");back();id("profile_consult");expect("我要咨询");back();
   click("关于&更新");expect("关于&更新");back();
   click("退出登录");expect("确认退出");click("取消");expect("退出登录");click("退出登录");click("确认退出");expect("未登录");click("使用模拟身份登录");click("使用模拟身份");expect("学习同学");note("PASS menus, logout confirmation and local demo login");
   id("nav_1");expect("暂无待办事项");snap("stage1-todo");id("nav_2");expect("收入纳税明细");snap("stage1-services");id("nav_3");click("欢迎使用税知账学习版");expect("欢迎使用");back();id("nav_0");click("收入纳税明细");expect("选择查询条件");back();id("nav_4");snap("stage1-profile-final");note("PASS all five destinations and existing income-query entry");
  }catch(Throwable e){snap("stage1-failure");note("FAIL "+e);throw e;}
 }
 @Test public void stageTwoHomeAndServices()throws Exception{
  try{
   d.executeShellCommand("am force-stop "+pkg);d.executeShellCommand("am start -W -n "+pkg+"/cn.taxlearning.ledger.MainActivity");expect("重点服务推荐");snap("stage2-home");
   click("请输入想搜索的功能/服务");expect("搜索功能 / 服务");UiObject2 input=d.findObject(By.clazz("android.widget.EditText"));input.setText("收入");SystemClock.sleep(350);click("收入纳税明细");expect("选择查询条件");back();note("PASS functional service search and income destination");
   click("查看记录");expect("收入纳税明细 · 2025");back();
   d.swipe(d.getDisplayWidth()/2,d.getDisplayHeight()*3/4,d.getDisplayWidth()/2,d.getDisplayHeight()/2,30);d.waitForIdle();UiScrollable strip=new UiScrollable(new UiSelector().className("android.widget.HorizontalScrollView"));strip.setAsHorizontalList();assertTrue(strip.exists());strip.scrollToEnd(5);d.waitForIdle();snap("stage2-home-shortcuts-end");click("更多功能");expect("办税");snap("stage2-services-top");
   click("收入纳税明细");expect("选择查询条件");back();snap("stage2-services-query");click("数据备份与恢复");expect("备份为 JSON");back();note("PASS home horizontal shortcuts, complete services catalogue and backup entry");
   id("nav_0");click("政策解读");expect("综合所得年度汇算学习");snap("stage2-home-feed");click("综合所得年度汇算学习");expect("综合所得年度汇算学习");back();id("nav_4");id("nav_0");expect("综合所得年度汇算学习");note("PASS fixed search header, article tabs/detail and preserved page scroll/feed state");
  }catch(Throwable e){snap("stage2-failure");note("FAIL "+e);throw e;}
 }
 @Test public void stageThreeQueryListDetail()throws Exception{
  try{
   d.executeShellCommand("am force-stop "+pkg);d.executeShellCommand("am start -W -n "+pkg+"/cn.taxlearning.ledger.MainActivity");id("nav_2");click("收入纳税明细");expect("请选择纳税年度");snap("stage3-query");
   id("query_year");expect("选择纳税年度");UiScrollable years=new UiScrollable(new UiSelector().className("android.widget.ListView"));years.scrollIntoView(new UiSelector().text("2019 年"));click("2019 年");expect("2019");id("query_year");years=new UiScrollable(new UiSelector().className("android.widget.ListView"));years.scrollToBeginning(10);click("2026 年");expect("2026");note("PASS native year picker 2019..2026");
   click("收起 ⌃");snap("stage3-query-collapsed");id("query_submit");expect("43400.00元");snap("stage3-list");
   click("课程演示 · 设计实践");expect("纳税明细信息");expect("3200.00元");snap("stage3-detail");click("查看税款计算 ›");expect("模拟税款记录");click("知道了");click("修改明细");expect("修改收入明细");snap("stage3-editor-existing");back();click("删除明细");expect("删除这笔收入？");click("取消");back();note("PASS repository totals, record detail, tax explanation, edit entry and deletion cancellation");
   id("income_filter");click("全部月份");click("2月");expect("筛选结果 1 笔 · 收入 ¥ 13,400.00");click("全部所得项目");click("稿酬");expect("筛选结果 0 笔 · 收入 ¥ 0.00");snap("stage3-filter-empty");id("income_statistics");expect("43,400.00");snap("stage3-statistics");note("PASS monthly/category filters and unchanged full-year statistics");
  }catch(Throwable e){snap("stage3-failure");note("FAIL "+e);throw e;}
 }
 @Test public void stageFourEditorKeyboardAndValidation()throws Exception{
  try{
   d.executeShellCommand("am force-stop "+pkg);d.executeShellCommand("am start -W -n "+pkg+"/cn.taxlearning.ledger.MainActivity");id("nav_2");click("收入纳税明细");id("query_submit");id("income_add");expect("新增收入明细");snap("stage4-editor");click("保存明细");expect("操作未完成");click("知道了");
   UiScrollable form=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));form.scrollIntoView(new UiSelector().className("android.widget.EditText"));UiObject2 input=d.findObject(By.clazz("android.widget.EditText"));assertNotNull(input);input.click();input.setText("UI键盘检查（未保存）");d.waitForIdle();snap("stage4-keyboard");UiObject2 save=expect("保存明细");assertTrue(save.getVisibleBounds().bottom<d.getDisplayHeight());d.pressBack();d.waitForIdle();if(!d.hasObject(By.text("放弃尚未保存的修改？"))){d.pressBack();d.waitForIdle();}expect("放弃尚未保存的修改？");click("放弃修改");assertTrue(d.wait(Until.hasObject(By.res(pkg+":id/income_add")),7000));note("PASS grouped editor, required validation, keyboard resize, visible save and discard confirmation");
  }catch(Throwable e){snap("stage4-failure");note("FAIL "+e);throw e;}
 }
}
