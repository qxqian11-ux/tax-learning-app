package cn.taxlearning.acceptance;

import android.graphics.*;
import android.os.*;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.File;
import java.util.*;
import static org.junit.Assert.*;

/** Actual signed-release UI audit. Run this named method only. */
@RunWith(AndroidJUnit4.class)
public class NoticePlaybackAudit extends QueryNoticeAudit {
 int shortPass=0;
 @Override @Before public void setup()throws Exception {super.setup();folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"notice-playback/"+prefix);folder.mkdirs();}
 Bundle state(){return new Bundle(marqueeNode().getExtras());}
 int index(Bundle b){return b.getInt("cn.taxlearning.notice.index");}
 float x(Bundle b){return b.getFloat("cn.taxlearning.notice.iconX");}
 boolean scrolling(Bundle b){return "scroll".equals(b.getString("cn.taxlearning.notice.mode"));}
 Bitmap crop(){Bitmap full=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();Rect r=byId("notice_marquee").getVisibleBounds();Bitmap image=Bitmap.createBitmap(full,r.left,r.top,r.width(),r.height());full.recycle();return image;}
 int orangeLeft(Bitmap b){int left=b.getWidth();for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++){int c=b.getPixel(x,y);if(Color.red(c)>180 && Color.green(c)>70 && Color.green(c)<205 && Color.blue(c)<110)left=Math.min(left,x);}assertTrue("Actual orange icon pixels must be visible",left<b.getWidth());return left;}
 void verifyShort()throws Exception {
  d.waitForIdle();SystemClock.sleep(650);shortPass++;
  for(int i=0;i<50 && state().getFloat("cn.taxlearning.notice.itemWidth")<=0;i++)SystemClock.sleep(100);
  assertTrue(state().getFloat("cn.taxlearning.notice.itemWidth")>0);
  assertEquals("个人所得税综合所得年度汇算学习提醒",marqueeNode().getText().toString());
  Bundle first=state();assertFalse(scrolling(first));assertEquals(0f,x(first),.01f);assertFalse(first.getBoolean("cn.taxlearning.notice.running"));
  Bitmap a=crop();SystemClock.sleep(1600);Bundle second=state();Bitmap b=crop();assertEquals(index(first),index(second));assertEquals(0f,x(second),.01f);
  saveBitmap(a,"short-"+shortPass+"-still-before.png");saveBitmap(b,"short-"+shortPass+"-still-after.png");assertTrue("Static item screenshots must match after entry animation settles",a.sameAs(b));a.recycle();b.recycle();note("PASS single fitting item: text and icon static; screenshots 1600ms apart are identical");
 }
 void verifySequence()throws Exception {
  long until=SystemClock.uptimeMillis()+150000;int previous=-1;long completed=-1;boolean shortSeen=false,firstLong=false,secondLong=false,motion=false,exitSeen=false;JSONArray timeline=new JSONArray();
  while(SystemClock.uptimeMillis()<until){
   Bundle s=state();int current=index(s);float icon=x(s),text=s.getFloat("cn.taxlearning.notice.textX");long count=s.getLong("cn.taxlearning.notice.completed");
   assertEquals(36f*d.getDisplayWidth()/400f,text-icon,2f);
   if(previous!=-1 && current!=previous){assertEquals((previous+1)%4,current);assertEquals(completed+1,count);if(previous==1 || previous==3){assertTrue(s.getFloat("cn.taxlearning.notice.lastExitRight")<=0);exitSeen=true;}}
   if(current!=previous){timeline.put(new JSONObject().put("index",current).put("completed",count).put("mode",s.getString("cn.taxlearning.notice.mode")).put("x",icon).put("lastExitRight",String.valueOf(s.getFloat("cn.taxlearning.notice.lastExitRight"))));previous=current;completed=count;}
   if(current==0 || current==2){assertFalse(scrolling(s));assertEquals(0f,icon,.01f);if(current==2)shortSeen=true;}
   else {assertTrue(scrolling(s));if(current==1)firstLong=true;if(current==3)secondLong=true;
    float viewport=s.getFloat("cn.taxlearning.notice.viewportWidth");
    if(!motion && icon>viewport*.25f && icon<viewport*.75f){Bitmap a=crop();Bundle aState=state();int aLeft=orangeLeft(a);SystemClock.sleep(700);Bitmap b=crop();Bundle bState=state();int bLeft=orangeLeft(b);assertEquals(index(aState),index(bState));assertTrue(x(bState)<x(aState));assertTrue("Icon pixels must move left",bLeft<aLeft);assertFalse(a.sameAs(b));saveBitmap(a,"03-long-icon-before.png");saveBitmap(b,"04-long-icon-after.png");a.recycle();b.recycle();motion=true;note("PASS actual orange icon pixels moved left "+aLeft+" -> "+bLeft+"; text/icon share translation");}
   }
   if(firstLong && secondLong && shortSeen && motion && exitSeen && count>=4){stateFile("sequence-timeline.json",timeline.toString(2));note("PASS short/long/short/long sequential cycle; next starts after long tail exits, no overlapping next item");return;}
   SystemClock.sleep(120);
  }fail("Timed out waiting for complete sequential playback");
 }
 @Test public void sequentialBroadcastAndOriginalData()throws Exception {
  String a="广播长内容甲：请逐项核对本机收入纳税明细并妥善保存学习记录与备份文件";
  String b="短提醒";
  String c="广播长内容乙：本条完整展示结束后才切换下一条内容且图标需要一起移动";
  List<String> before=null;boolean restored=false;
  try {
   JSONArray incomeBefore=records(export(true,"全部年度",file("income-before.json")));String employmentBefore=employmentState();
   noticeList();before=noticeRows();assertEquals("Dedicated baseline has one default notice",1,before.size());stateFile("notice-before.json",new JSONArray(before).toString());
   home();byId("notice_marquee");verifyShort();snap("05-short-home");noticeList();addNotice(a);addNotice(b);addNotice(c);
   home();byId("notice_marquee");verifySequence();snap("06-sequence-home");
   d.executeShellCommand("am force-stop "+pkg);assertEquals("",d.executeShellCommand("pidof "+pkg).trim());noticeList();assertNotNull(noticeRow(a));assertNotNull(noticeRow(b));assertNotNull(noticeRow(c));note("PASS added playback samples survive full process stop/restart");
   for(String value:new String[]{a,b,c})deleteNotice(value,false);assertEquals(before,noticeRows());restored=true;home();verifyShort();
   JSONArray incomeAfter=records(export(true,"全部年度",file("income-final.json")));assertEquals(incomeBefore.toString(),incomeAfter.toString());assertEquals(employmentBefore,employmentState());noticeList();assertEquals(before,noticeRows());home();
   note("ALL PLAYBACK CHECKS PASSED; sample notices removed; original income/employment/notice data unchanged");
  }catch(Throwable error){snap("failure-playback");note("FAIL "+error);throw error;}
  finally {if(before!=null && !restored)try{noticeList();for(String value:new String[]{a,b,c})if(ownNoticeExists(value))deleteNotice(value,false);assertEquals(before,noticeRows());home();note("Cleanup restored original notice list after failed audit");}catch(Throwable cleanup){note("Cleanup requires attention: "+cleanup);}}
 }
}
