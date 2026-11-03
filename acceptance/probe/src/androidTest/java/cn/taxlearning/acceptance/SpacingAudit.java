package cn.taxlearning.acceptance;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SpacingAudit extends UiReleaseRegression {
 @Override @Before public void setup()throws Exception{super.setup();folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"spacing-111");folder.mkdirs();}
 @Test public void captureCalibration()throws Exception{
  try{
   home();snap("home");id("nav_4");snap("profile");id("nav_2");snap("services");
   find("收入纳税明细");snap("services-query");click("收入纳税明细");waitText("请选择纳税年度");snap("query");id("query_year");snap("year-picker");d.pressBack();id("query_submit");listReady();snap("list");
   detail("课程演示 · 设计实践");top();snap("detail");find("本期收入与扣除详情");snap("detail-lower");click("修改明细");snap("editor");d.pressBack();
   home();find("办理汇算想便捷 APP密码要记牢");snap("home-feed");
   UiScrollable page=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));page.setSwipeDeadZonePercentage(.22);page.scrollToEnd(10);snap("home-feed-bottom");
   note("PASS actual calibration captures "+pkg);
  }catch(Throwable e){snap("capture-failure");note("FAIL "+e);throw e;}
 }
 @Test public void actionsAndCategories()throws Exception{
  try{
   home();id("nav_4");id("profile_family");waitText("家庭成员信息");d.pressBack();id("profile_care");waitText("切换标准版");id("profile_care");
   id("nav_0");click("请输入想搜索的功能/服务");waitText("搜索功能 / 服务");d.findObject(By.clazz("android.widget.EditText")).setText("收入");click("收入纳税明细");waitText("请选择纳税年度");
   click("⋯");waitText("所得类型（含历史分类）");click("其他所得");click("完成");click("收起 ⌃");click("展开 ⌄");
   id("query_submit");listReady();id("income_filter");click("全部月份");click("2月");expect("筛选结果 1 笔 · 收入 ¥ 13,400.00");
   id("income_statistics");expect("43,400.00");note("PASS navigation, care mode, search, legacy category, query, filter and statistics");
  }catch(Throwable e){snap("actions-failure");note("FAIL "+e);throw e;}
 }
}
