package art.pikoos.runtimeexperiment;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import art.pikoos.lab.core.RuntimeMenu;
import art.pikoos.lab.core.RuntimeMenu.Action;
import art.pikoos.lab.core.RuntimeMenu.State;
import java.lang.reflect.*;
import java.util.*;

/** Android-only input boundary around the pinned Godot Activity, using public window APIs. */
public final class RuntimeControls implements Application.ActivityLifecycleCallbacks {
    private static RuntimeControls installed;
    private final Map<Activity,Controls> active=new HashMap<>();
    public static void install(Application app){installed=new RuntimeControls();app.registerActivityLifecycleCallbacks(installed);}
    /** Called by the pinned Godot process monitor after its EXITED journal write, before quitting. */
    public static void sessionExited(){
        final java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        Runnable finish=()->{try{if(installed!=null)for(Controls c:installed.active.values())c.returnToCaller();}finally{done.countDown();}};
        if(Looper.myLooper()==Looper.getMainLooper())finish.run();
        else{new Handler(Looper.getMainLooper()).post(finish);try{done.await(1,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    }
    public void onActivityCreated(Activity a,Bundle b){}
    public void onActivityStarted(Activity a){}
    public void onActivityResumed(Activity a){
        if(!a.getClass().getName().equals("com.godot.game.GodotApp"))return;
        Controls c=active.get(a);if(c==null){c=new Controls(a);active.put(a,c);}
        if(a.getIntent().getBooleanExtra("pikoos.controls",false)){c.enabled=true;c.swapAB=a.getIntent().getBooleanExtra("pikoos.swapAB",false);}
        c.captureReturn();
        c.resumed=true;c.attach();
        if(c.model.state==State.OPEN&&c.dialog==null)c.show();
    }
    public void onActivityPaused(Activity a){Controls c=active.get(a);if(c!=null){if(a.isFinishing())c.returnToCaller();c.resumed=false;}}
    public void onActivityStopped(Activity a){Controls c=active.get(a);if(c!=null)c.background();}
    public void onActivitySaveInstanceState(Activity a,Bundle b){}
    public void onActivityDestroyed(Activity a){Controls c=active.remove(a);if(c!=null)c.background();}

    private static final class Controls {
        final Activity activity;final Handler handler=new Handler(Looper.getMainLooper());
        final RuntimeMenu model=new RuntimeMenu();
        Window.Callback delegate,proxy;Dialog dialog;MenuView view,progress;boolean resumed,enabled,swapAB;int generation;float axis;
        final Set<Integer> swallowed=new HashSet<>();
        PendingIntent returnTo;String token="";boolean returned;
        Controls(Activity a){activity=a;}
        void captureReturn(){
            PendingIntent candidate=activity.getIntent().getParcelableExtra("pikoos.return");
            String next=activity.getIntent().getStringExtra("pikoos.session");
            if(candidate!=null&&"art.pikoos.runtimelab".equals(candidate.getCreatorPackage())&&art.pikoos.lab.core.RuntimeSession.validToken(next)){
                returnTo=candidate;token=next;
            }
        }
        void returnToCaller(){
            if(returned||returnTo==null||!resumed)return;
            if(SessionStatus.phase(activity,token)!=art.pikoos.lab.core.RuntimeSession.Phase.EXITED)return;
            try{returnTo.send();returned=true;android.util.Log.i("PIKOOS.Exit","Returning to session origin");}
            catch(PendingIntent.CanceledException e){android.util.Log.w("PIKOOS.Exit","Origin no longer available");}
        }
        boolean swap(){return swapAB;}
        void attach(){
            Window w=activity.getWindow();if(w.getCallback()==proxy)return;
            delegate=w.getCallback();final Window.Callback original=delegate;
            proxy=(Window.Callback)Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class<?>[]{Window.Callback.class},(p,m,args)->{
                if(m.getName().equals("dispatchKeyEvent")&&key((KeyEvent)args[0]))return true;
                if(m.getName().equals("dispatchGenericMotionEvent")&&motion((MotionEvent)args[0]))return true;
                try{return m.invoke(original,args);}catch(InvocationTargetException ex){throw ex.getCause();}
            });w.setCallback(proxy);
        }
        boolean key(KeyEvent e){
            if(!enabled)return false;
            int k=e.getKeyCode();boolean menu=k==KeyEvent.KEYCODE_BUTTON_SELECT||k==KeyEvent.KEYCODE_MENU||k==KeyEvent.KEYCODE_BACK;
            if(model.state==State.CLOSED&&swallowed.contains(k)){if(e.getAction()==KeyEvent.ACTION_UP)swallowed.remove(k);return true;}
            if(model.state==State.CLOSED&&!menu)return false;
            // Volume and system keys retain their normal Android behavior.
            if(k==KeyEvent.KEYCODE_VOLUME_UP||k==KeyEvent.KEYCODE_VOLUME_DOWN||k==KeyEvent.KEYCODE_VOLUME_MUTE)return false;
            if(e.getAction()==KeyEvent.ACTION_UP)swallowed.remove(k);else swallowed.add(k);
            if(e.getAction()!=KeyEvent.ACTION_DOWN||e.getRepeatCount()!=0)return true;
            Action a=null;
            if(menu)a=Action.MENU;
            else if(k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_LEFT)a=Action.PREVIOUS;
            else if(k==KeyEvent.KEYCODE_DPAD_DOWN||k==KeyEvent.KEYCODE_DPAD_RIGHT)a=Action.NEXT;
            else if(k==(swap()?KeyEvent.KEYCODE_BUTTON_B:KeyEvent.KEYCODE_BUTTON_A)||k==KeyEvent.KEYCODE_ENTER||k==KeyEvent.KEYCODE_DPAD_CENTER)a=Action.CONFIRM;
            else if(k==(swap()?KeyEvent.KEYCODE_BUTTON_A:KeyEvent.KEYCODE_BUTTON_B)||k==KeyEvent.KEYCODE_ESCAPE)a=Action.CANCEL;
            if(a!=null)act(a);return true;
        }
        boolean motion(MotionEvent e){
            if(model.state==State.CLOSED)return false;
            if((e.getSource()&InputDevice.SOURCE_JOYSTICK)!=InputDevice.SOURCE_JOYSTICK)return false;
            float y=e.getAxisValue(MotionEvent.AXIS_HAT_Y);if(Math.abs(y)<.5f)y=e.getAxisValue(MotionEvent.AXIS_Y);
            float next=Math.abs(y)<.55f?0:Math.signum(y);
            if(next!=axis&&next!=0)act(next>0?Action.NEXT:Action.PREVIOUS);axis=next;return true;
        }
        void act(Action a){
            State before=model.state;model.act(a);
            if(model.state==State.CLOSED)hide();
            else if(model.state==State.OPEN){if(dialog==null)show();else view.invalidate();}
            else if(before!=State.EXIT_REQUESTED)requestExit();
        }
        void show(){
            if(!resumed||activity.isFinishing()||activity.isDestroyed())return;
            axis=0;dialog=new Dialog(activity,android.R.style.Theme_Black_NoTitleBar_Fullscreen){
                @Override public boolean dispatchKeyEvent(KeyEvent e){return key(e)||super.dispatchKeyEvent(e);}
                @Override public boolean dispatchGenericMotionEvent(MotionEvent e){return motion(e)||super.dispatchGenericMotionEvent(e);}
                @Override public void onBackPressed(){act(Action.CANCEL);}
            };
            dialog.setCancelable(false);view=new MenuView(this);dialog.setContentView(view);
            Window w=dialog.getWindow();w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.7f);
            w.getDecorView().setSystemUiVisibility(5894);dialog.show();w.setLayout(-1,-1);
        }
        void hide(){if(dialog!=null){Dialog d=dialog;dialog=null;d.dismiss();}if(progress!=null){((ViewGroup)activity.getWindow().getDecorView()).removeView(progress);progress=null;}axis=0;}
        void background(){generation++;handler.removeCallbacksAndMessages(null);swallowed.clear();model.background();hide();}
        void requestExit(){
            hide();final int request=++generation;
            progress=new MenuView(this);((ViewGroup)activity.getWindow().getDecorView()).addView(progress,new ViewGroup.LayoutParams(-1,-1));
            // Let Godot regain focus/resume before its normal input path receives Ctrl+Q.
            handler.postDelayed(()->{
                if(request!=generation||!resumed||!activity.hasWindowFocus()){unconfirmed(request);return;}
                android.util.Log.i("PIKOOS.Exit","Requesting official Ctrl+Q");
                long now=SystemClock.uptimeMillis();
                try{
                    send(now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_CTRL_LEFT,KeyEvent.META_CTRL_ON|KeyEvent.META_CTRL_LEFT_ON);
                    send(now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_Q,KeyEvent.META_CTRL_ON|KeyEvent.META_CTRL_LEFT_ON);
                    send(now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_Q,KeyEvent.META_CTRL_ON|KeyEvent.META_CTRL_LEFT_ON);
                    send(now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_CTRL_LEFT,0);
                }catch(RuntimeException e){unconfirmed(request);return;}
                handler.postDelayed(()->unconfirmed(request),4000);
            },250);
        }
        void send(long now,int action,int key,int modifiers){delegate.dispatchKeyEvent(new KeyEvent(now,SystemClock.uptimeMillis(),action,key,0,modifiers,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,InputDevice.SOURCE_KEYBOARD));}
        void unconfirmed(int request){
            if(request!=generation||activity.isFinishing()||activity.isDestroyed())return;
            hide();model.unconfirmed();if(resumed)show();
            android.util.Log.w("PIKOOS.Exit","Exit not confirmed; no forced process termination");
        }
    }
    private static final class MenuView extends View {
        final Controls controls;final Paint paint=new Paint();final Typeface font;
        final RectF[] buttons={new RectF(),new RectF()};float scale;int pressed=-1;
        MenuView(Controls c){super(c.activity);controls=c;font=Typeface.createFromAsset(c.activity.getAssets(),"pikoos/Tiny5-Regular.ttf");setFocusable(true);setContentDescription("Меню игры: продолжить или завершить игру");}
        void box(Canvas c,float x,float y,float w,float h,int color){paint.setColor(color);c.drawRect(x,y,x+w,y+h,paint);}
        void text(Canvas c,String s,float x,float y,float size,int color){paint.setTypeface(font);paint.setTextSize(size);paint.setColor(color);c.drawText(s,x,y,paint);}
        @Override protected void onDraw(Canvas canvas){
            scale=Math.min(getWidth()/400f,getHeight()/480f);Canvas c=canvas; c.save();c.scale(scale,scale);
            float w=getWidth()/scale,h=getHeight()/scale,left=(w-368)/2,top=(h-324)/2;
            box(c,left,top,368,324,0xff1d2b53);box(c,left,top,368,4,0xffff77a8);
            text(c,"PIKOOS / МЕНЮ ИГРЫ",left+16,top+39,24,0xffff77a8);
            if(controls.model.state==State.EXIT_REQUESTED){
                text(c,"Завершаем игру...",left+16,top+102,26,0xffffec27);
                text(c,"Ждём выхода PICO-8.",left+16,top+140,20,0xffc2c3c7);
                text(c,"Затем вернёмся к предыдущему",left+16,top+189,18,0xfffff1e8);
                text(c,"экрану.",left+16,top+213,18,0xfffff1e8);c.restore();return;
            }
            if(controls.model.failed){
                text(c,"Игра пока не завершилась.",left+16,top+78,20,0xffffec27);
                text(c,"Можно продолжить или повторить.",left+16,top+103,18,0xffc2c3c7);
            }else{
                text(c,"Вернуться из игры?",left+16,top+79,26,0xfffff1e8);
                text(c,"Прогресс зависит от сохранений",left+16,top+108,18,0xffc2c3c7);
                text(c,"самой игры.",left+16,top+130,18,0xffc2c3c7);
            }
            for(int i=0;i<2;i++){
                RectF b=buttons[i];b.set(left+16,top+155+i*59,left+352,top+205+i*59);
                boolean selected=controls.model.selection==i;box(c,b.left,b.top,b.width(),b.height(),selected?0xffffec27:0xff000000);
                text(c,(selected?"> ":"  ")+(i==0?"Продолжить":"Завершить игру"),b.left+12,b.top+33,24,selected?0xff1d2b53:0xfffff1e8);
            }
            text(c,(controls.swap()?"B":"A")+": выбрать     "+(controls.swap()?"A":"B")+": назад",left+16,top+303,18,0xffc2c3c7);c.restore();
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(controls.model.state!=State.OPEN)return true;
            float x=e.getX()/scale,y=e.getY()/scale;
            if(e.getAction()==MotionEvent.ACTION_DOWN){pressed=-1;for(int i=0;i<2;i++)if(buttons[i].contains(x,y)){pressed=i;controls.model.selection=i;invalidate();}}
            if(e.getAction()==MotionEvent.ACTION_UP){int selected=pressed;pressed=-1;if(selected>=0&&buttons[selected].contains(x,y))controls.act(Action.CONFIRM);performClick();}
            if(e.getAction()==MotionEvent.ACTION_CANCEL)pressed=-1;return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
