package art.pikoos.runtimelab;

import android.os.Handler;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Android codes end here. Core and view only receive semantic actions. */
final class ControllerInput {
    interface Sink { void send(Action action); }
    interface Mapping { boolean swapAB(); }
    private final Sink sink;
    private final Mapping mapping;
    private final Handler handler=new Handler();
    private Action heldAxis,heldKey;
    private final Runnable repeat=new Runnable(){public void run(){if(heldAxis!=null){sink.send(heldAxis);handler.postDelayed(this,110);}}};
    ControllerInput(Sink sink,Mapping mapping){this.sink=sink;this.mapping=mapping;}
    void reset(){heldAxis=null;heldKey=null;handler.removeCallbacks(repeat);}
    private static boolean navigation(Action a){return a==Action.UP||a==Action.DOWN||a==Action.LEFT||a==Action.RIGHT;}
    boolean key(KeyEvent event){
        Action action;
        switch(event.getKeyCode()){
            case KeyEvent.KEYCODE_DPAD_UP:action=Action.UP;break;
            case KeyEvent.KEYCODE_DPAD_DOWN:action=Action.DOWN;break;
            case KeyEvent.KEYCODE_DPAD_LEFT:action=Action.LEFT;break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:action=Action.RIGHT;break;
            case KeyEvent.KEYCODE_BUTTON_A:action=mapping.swapAB()?Action.CANCEL:Action.CONFIRM;break;
            case KeyEvent.KEYCODE_BUTTON_B:action=mapping.swapAB()?Action.CONFIRM:Action.CANCEL;break;
            case KeyEvent.KEYCODE_ENTER:case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_Z:action=Action.CONFIRM;break;
            case KeyEvent.KEYCODE_BACK:case KeyEvent.KEYCODE_ESCAPE:action=Action.CANCEL;break;
            case KeyEvent.KEYCODE_BUTTON_X:case KeyEvent.KEYCODE_X:action=Action.CONTEXT;break;
            case KeyEvent.KEYCODE_BUTTON_Y:case KeyEvent.KEYCODE_DEL:action=Action.UNDO;break;
            case KeyEvent.KEYCODE_BUTTON_L1:case KeyEvent.KEYCODE_Q:action=Action.PREVIOUS;break;
            case KeyEvent.KEYCODE_BUTTON_R1:case KeyEvent.KEYCODE_E:action=Action.NEXT;break;
            case KeyEvent.KEYCODE_BUTTON_START:case KeyEvent.KEYCODE_F5:action=Action.TEST;break;
            case KeyEvent.KEYCODE_BUTTON_SELECT:case KeyEvent.KEYCODE_MENU:action=Action.MENU;break;
            default:return false;
        }
        if(event.getAction()==KeyEvent.ACTION_DOWN&&(event.getRepeatCount()==0||navigation(action))){
            // Some controllers report their D-pad as both keys and hat axes.
            if(navigation(action))heldKey=action;
            if(!navigation(action)||heldAxis!=action)sink.send(action);
        }
        if(event.getAction()==KeyEvent.ACTION_UP&&heldKey==action)heldKey=null;
        return true;
    }
    boolean motion(MotionEvent event){
        if((event.getSource()&InputDevice.SOURCE_JOYSTICK)!=InputDevice.SOURCE_JOYSTICK||event.getAction()!=MotionEvent.ACTION_MOVE)return false;
        float x=event.getAxisValue(MotionEvent.AXIS_HAT_X),y=event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        if(Math.abs(x)<.5f&&Math.abs(y)<.5f){x=event.getAxisValue(MotionEvent.AXIS_X);y=event.getAxisValue(MotionEvent.AXIS_Y);}
        Action next=Math.max(Math.abs(x),Math.abs(y))<.55f?null:
            Math.abs(x)>Math.abs(y)?(x>0?Action.RIGHT:Action.LEFT):(y>0?Action.DOWN:Action.UP);
        if(next!=heldAxis){handler.removeCallbacks(repeat);heldAxis=next;if(next!=null){if(next!=heldKey)sink.send(next);handler.postDelayed(repeat,350);}}
        return true;
    }
}
