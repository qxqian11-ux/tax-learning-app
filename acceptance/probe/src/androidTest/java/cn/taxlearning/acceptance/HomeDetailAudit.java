package cn.taxlearning.acceptance;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.File;
import java.util.List;
import static org.junit.Assert.*;

/** Visual/touch regression only. Run this named method, not inherited historical data tests. */
@RunWith(AndroidJUnit4.class)
public class HomeDetailAudit extends QueryNoticeAudit {
 @Override @Before public void setup()throws Exception{
  super.setup();folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"home-detail/"+prefix);folder.mkdirs();
 }
 UiScrollable homeScroll(){UiScrollable scroll=new UiScrollable(new UiSelector().className("android.widget.ScrollView"));scroll.setSwipeDeadZonePercentage(.22);return scroll;}
 void homeTop()throws Exception{
  home();id("nav_0");UiScrollable scroll=homeScroll();if(scroll.exists())scroll.scrollToBeginning(8);
  byId("home_search_bar");byId("home_notice");d.waitForIdle();
 }
 UiObject2 visibleHomeText(String label)throws Exception{
  UiScrollable scroll=homeScroll();
  UiObject2 item=d.findObject(By.text(label));
  if(item==null || item.getVisibleBounds().isEmpty())if(scroll.exists())scroll.scrollIntoView(new UiSelector().text(label));
  for(int i=0;i<8;i++){
   item=d.findObject(By.text(label));
   if(item!=null && !item.getVisibleBounds().isEmpty()){
    Rect bounds=item.getVisibleBounds(),bar=byId("home_search_bar").getVisibleBounds(),nav=byId("nav_0").getVisibleBounds();
    if(bounds.top>=bar.bottom && bounds.bottom<nav.top-8)return item;
    if(bounds.top<bar.bottom)d.swipe(d.getDisplayWidth()/2,d.getDisplayHeight()*4/10,d.getDisplayWidth()/2,d.getDisplayHeight()*6/10,30);
    else d.swipe(d.getDisplayWidth()/2,d.getDisplayHeight()*7/10,d.getDisplayWidth()/2,d.getDisplayHeight()*4/10,30);
   }else{
    if(scroll.exists())scroll.scrollIntoView(new UiSelector().text(label));
   }
   d.waitForIdle();SystemClock.sleep(150);
  }
  fail("Home text must be visibly reachable above floating navigation: "+label);return null;
 }
 void homeTextClick(String label)throws Exception{visibleHomeText(label).click();d.waitForIdle();SystemClock.sleep(250);}
 void homeViewClick(String name)throws Exception{
  UiObject2 item=byId(name);Rect bounds=item.getVisibleBounds(),nav=byId("nav_0").getVisibleBounds();
  if(bounds.isEmpty() || bounds.bottom>=nav.top-8){UiScrollable scroll=homeScroll();if(scroll.exists())scroll.scrollIntoView(new UiSelector().resourceId(pkg+":id/"+name));item=byId(name);}
  assertFalse("Visible home control "+name,item.getVisibleBounds().isEmpty());item.click();d.waitForIdle();SystemClock.sleep(250);
 }
 JSONObject bounds(String id)throws Exception{
  Rect rect=byId(id).getVisibleBounds();float unit=400f/d.getDisplayWidth();
  return new JSONObject().put("id",id).put("boundsPx",new JSONArray(new int[]{rect.left,rect.top,rect.right,rect.bottom}))
   .put("widthUnits",rect.width()*unit).put("heightUnits",rect.height()*unit);
 }
 void measureHomeHeader()throws Exception{
  Rect bar=byId("home_search_bar").getVisibleBounds(),field=byId("home_search_field").getVisibleBounds();
  float scale=400f/d.getDisplayWidth();assertEquals("Fixed search bar height",46f,bar.height()*scale,1f);assertEquals("Search field height",35f,field.height()*scale,1f);
  Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
  JSONArray samples=new JSONArray();int maxStep=0;
  for(float fraction:new float[]{.04f,.15f,.30f,.50f,.70f,.85f,.96f}){
   int x=Math.min(image.getWidth()-1,Math.max(0,Math.round(image.getWidth()*fraction)));int sampleMax=0;
   for(int y=bar.bottom-3;y<bar.bottom+3;y++){
    int a=image.getPixel(x,y),b=image.getPixel(x,y+1);
    int delta=Math.max(Math.abs(Color.red(a)-Color.red(b)),Math.max(Math.abs(Color.green(a)-Color.green(b)),Math.abs(Color.blue(a)-Color.blue(b))));sampleMax=Math.max(sampleMax,delta);
   }
   maxStep=Math.max(maxStep,sampleMax);samples.put(new JSONObject().put("x",x).put("maxAdjacentRowChannelDelta",sampleMax));
  }
  Bitmap seam=Bitmap.createBitmap(image,0,bar.bottom-12,image.getWidth(),25);saveBitmap(seam,"02-search-seam-strip.png");seam.recycle();image.recycle();
  JSONArray measured=new JSONArray();for(String id:new String[]{"home_search_bar","home_search_field","home_search_scan","home_hero","home_hero_topic","home_hero_record","home_deduction","home_deduction_fill","home_deduction_lookup","home_shortcuts","home_notice"})measured.put(bounds(id));
  stateFile("home-header-metrics.json",new JSONObject().put("unit","screen-width normalized to 400").put("measurements",measured).put("seamSamples",samples).put("maxSeamStep",maxStep).toString(2));
  assertTrue("Search/header boundary must not introduce a visible one-pixel seam; actual delta="+maxStep,maxStep<=10);
  note("PASS search bar 46u / field 35u; adjacent seam max channel delta="+maxStep);
 }
 @Test public void homeVisualControlsAndStatePreservation()throws Exception{
  try{
   JSONArray before=records(export(true,"全部年度",file("income-before.json")));assertEquals("Original acceptance dataset",6,before.length());
   String employmentBefore=employmentState();assertEquals("0家|[]",employmentBefore);stateFile("employment-before.txt",employmentBefore);
   noticeList();List<String> noticeBefore=noticeRows();assertEquals("Original default notice",1,noticeBefore.size());stateFile("notice-before.json",new JSONArray(noticeBefore).toString());

   homeTop();snap("01-home-top");measureHomeHeader();
   id("home_search_field");waitText("搜索功能 / 服务");UiObject2 search=d.findObject(By.clazz("android.widget.EditText"));assertNotNull(search);search.setText("收入");click("收入纳税明细");waitText("请选择纳税年度");snap("03-search-to-query");d.pressBack();byId("home_search_bar");
   id("home_search_scan");waitText("扫一扫");d.pressBack();byId("home_search_bar");
   homeViewClick("home_hero_record");listReady();assertNoIncomeFooter();id("income_title_add");waitText("新增收入明细");d.pressBack();listReady();d.pressBack();byId("home_search_bar");
   homeViewClick("home_deduction_fill");waitText("专项附加扣除填报");d.pressBack();byId("home_search_bar");
   Rect fillEdge=byId("home_deduction_fill").getVisibleBounds();assertFalse("Blue slanted-edge control must be visible",fillEdge.isEmpty());
   int fillEdgeX=fillEdge.left+Math.round(fillEdge.width()*120f/138f),fillEdgeY=fillEdge.top+Math.round(fillEdge.height()*10f/52f);
   assertTrue("Tap inside blue visible slanted edge",d.click(fillEdgeX,fillEdgeY));d.waitForIdle();waitText("专项附加扣除填报");snap("03b-deduction-blue-edge");d.pressBack();byId("home_search_bar");
   note("PASS actual blue slanted-edge tap routed to fill at normalized local (120/138,10/52)");
   homeViewClick("home_deduction_lookup");waitText("专项附加扣除查询");d.pressBack();byId("home_search_bar");note("PASS search, scan, hero-record and deduction controls; title-to-add editor opened and left without saving");

   UiScrollable carousel=new UiScrollable(new UiSelector().resourceId(pkg+":id/home_shortcuts"));assertTrue(carousel.exists());carousel.setAsHorizontalList();carousel.setSwipeDeadZonePercentage(.15);carousel.scrollToEnd(5);waitText("更多功能");snap("04-home-carousel-end-and-nav");carousel.scrollToBeginning(5);
   homeTextClick("收入纳税明细");waitText("请选择纳税年度");d.pressBack();byId("home_shortcuts");
   homeTextClick("通知公告");waitText("学习版使用说明");snap("05-home-notice-feed");homeTextClick("警示案例");visibleHomeText("办理汇算想便捷 APP密码要记牢");snap("06-home-case-feed");homeTextClick("办理汇算想便捷 APP密码要记牢");waitText("办理汇算想便捷 APP密码要记牢");d.pressBack();byId("home_news_tabs");
   homeScroll().scrollToEnd(8);snap("07-home-feed-bottom");
   id("nav_1");waitText("暂无待办事项");id("nav_2");waitText("办税");id("nav_3");waitText("服务消息与通知");id("nav_4");waitText("学习同学");assertEquals("0家",byId("profile_employment_count").getText());id("nav_0");homeScroll().scrollToBeginning(8);byId("home_notice");snap("08-home-return-top");
   verifyMarqueeMotion("09-home-return");longPress(byId("home_notice"));byId("notice_add");assertEquals(noticeBefore,noticeRows());snap("10-broadcast-management-readonly");
   JSONArray after=records(export(true,"全部年度",file("income-final.json")));assertEquals(before.toString(),after.toString());assertEquals(employmentBefore,employmentState());noticeList();assertEquals(noticeBefore,noticeRows());homeTop();
   note("ALL HOME DETAIL CHECKS PASSED: fixed header seam, search/scan, hero/deduction, carousel, news, five-tab navigation, live marquee and management entry; no business records saved/deleted; original six income rows and original employment/notice state unchanged "+prefix);
  }catch(Throwable e){snap("failure-home-detail");note("FAIL home detail "+e);throw e;}
 }
}
