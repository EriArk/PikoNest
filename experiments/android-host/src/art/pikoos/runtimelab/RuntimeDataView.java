package art.pikoos.runtimelab;

import android.graphics.*;
import android.view.*;
import art.pikoos.lab.core.RuntimeHomeMigration;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Import review in the workshop palette; touch and controller share a path. */
final class RuntimeDataView extends View {
    final RuntimeDataActivity activity;final Paint p=new Paint();Canvas c;float scale,w,h,left,width,buttonY,choiceY;
    RuntimeDataView(RuntimeDataActivity a){super(a);activity=a;PixelText.configure(a,p);setFocusable(true);setContentDescription("Import old PICO-8 data; review conflicts before activation");}
    void rect(float x,float y,float width,float height,int color){p.setColor(WorkshopView.COLORS[color]);c.drawRect(x,y,x+width,y+height,p);}
    void text(String s,float x,float y,int size,int color){p.setColor(WorkshopView.COLORS[color]);p.setTextSize(PixelText.size(size));c.drawText(s,x,y,p);}
    void fit(String s,float x,float y,int size,int color,float width){p.setTextSize(PixelText.size(size));while(s.length()>1&&p.measureText(s)>width)s=s.substring(0,s.length()-2)+"…";text(s,x,y,size,color);}
    float wrap(String s,float y,int size,int color){String line="";p.setTextSize(PixelText.size(size));for(String word:s.split(" ")){String next=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&p.measureText(next)>width){fit(line,left,y,size,color,width);y+=size+5;line=word;}else line=next;}if(!line.isEmpty()){fit(line,left,y,size,color,width);y+=size+5;}return y;}
    @Override protected void onDraw(Canvas canvas){
        c=canvas;scale=Math.min(getWidth()/400f,getHeight()/620f);w=getWidth()/scale;h=getHeight()/scale;left=Math.max(16,(w-630)/2);width=Math.min(w-32,630);c.save();c.scale(scale,scale);
        rect(0,0,w,h,1);rect(0,0,w,42,2);text("PikoNest",16,30,28,7);text("DATA",w-83,29,18,14);
        text("Bring your old data",left,88,30,7);
        float y=wrap("Saves, downloaded carts and PICO-8 settings. Your projects stay in Workshop.",125,18,6);
        RuntimeMigrationJob job=activity.job;RuntimeHomeMigration.Plan plan=job==null?null:job.plan;choiceY=-1;
        if(plan!=null){
            rect(left,y+3,width,92,0);text(plan.source.size()+" files  /  "+String.format(java.util.Locale.ROOT,"%.1f MiB",plan.sourceBytes/1048576.0),left+14,y+32,22,11);
            fit(plan.added.size()+" new · "+plan.same.size()+" identical · "+plan.conflicts.size()+" different",left+14,y+66,18,6,width-28);y+=119;
            if(!job.applied&&!job.finished&&!job.cancelled){
                y=wrap("When the same file is different:",y,18,7);choiceY=y-4;rect(left,choiceY,width,39,2);
                fit("< "+(activity.importedWins?"Use imported versions":"Keep current versions")+" >",left+12,y+22,21,10,width-24);y+=52;
                if(!plan.conflicts.isEmpty()){int index=Math.floorMod(activity.conflictIndex,plan.conflicts.size());fit((index+1)+"/"+plan.conflicts.size()+"  "+plan.conflicts.get(index)+"  [up/down]",left,y,17,13,width);y+=26;}
            }
            y=wrap(job.applied?"Verified copy activated. Previous data and the source folder are both kept.":"Only a verified copy becomes active. Previous data and source files are kept.",y,18,6);
        }else y=wrap("Choose the old data folder containing config.txt, carts, cdata or bbs. For the earlier wrapper, choose Documents / pico8 / data.",y+18,20,7);
        String problem=job!=null&&!job.error.isEmpty()?job.error:activity.notice;
        if(!problem.isEmpty())y=wrap(problem,y+10,18,9);
        buttonY=Math.min(Math.max(y+10,h-110),h-72);
        rect(left,buttonY,width,43,job!=null&&job.busy?5:2);rect(left,buttonY,4,43,10);
        boolean swap=activity.getSharedPreferences("library-ui",0).getBoolean("swapAB",false);
        String action=job!=null&&job.busy?job.phase:(swap?"B":"A")+"  "+(job!=null&&job.applied?"Done":job!=null&&job.ready&&!job.cancelled?"Import verified copy":"Choose old data folder");
        fit(action,left+14,buttonY+29,22,10,width-28);text((swap?"A":"B")+" Back / cancel",left,h-12,17,6);c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_UP){float y=e.getY()/scale;if(y>=h-32)activity.action(Action.CANCEL);else if(choiceY>=0&&y>=choiceY&&y<=choiceY+39)activity.action(Action.RIGHT);else if(y>=buttonY&&y<=buttonY+43)activity.action(Action.CONFIRM);performClick();return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
