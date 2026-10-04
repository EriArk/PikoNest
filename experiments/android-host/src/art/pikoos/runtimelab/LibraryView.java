package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.Canvas;
import art.pikoos.lab.core.LibrarySession;
import art.pikoos.lab.core.WorkshopCartridge;
import art.pikoos.lab.core.HeroBinding;
import art.pikoos.lab.core.SpriteRegion;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Editable projects use the same navigation and visual roles as the Play shelf. */
final class LibraryView extends ShelfSurface {
    private final LibrarySession s;
    private final Runnable changed,folders,play;
    private final String active;
    LibraryView(Context context,LibrarySession session,String active,boolean swap,Runnable changed,Runnable folders,Runnable play){
        super(context,swap);s=session;this.active=active;this.changed=changed;this.folders=folders;this.play=play;
        setContentDescription("Workshop projects. Up and down: project. Confirm: open. Select: new, copy, import, export. Back: Play.");
    }
    void action(Action action){s.act(action);changed.run();invalidate();}
    private void command(int command){s.command(command);changed.run();invalidate();}
    @Override protected void onDraw(Canvas canvas){
        begin(canvas);header(true,play,()->{});
        text("Your projects",16,132,24,7);
        button("+ New",w-114,106,98,false,()->command(1));
        int count=rows(),start=s.selected/count*count;float width=listWidth();
        for(int i=start;i<Math.min(s.entries().size(),start+count);i++){
            final int chosen=i;LibrarySession.Entry entry=s.entries().get(i);
            row(entry.title,entry.cart==null?"Could not open":entry.id.equals(active)?"Last opened":"PICO-8 project",i,s.selected,start,width,
                ()->{s.selected=chosen;s.focus=0;changed.run();invalidate();});
        }
        LibrarySession.Entry entry=s.current();
        if(w>=540&&entry!=null){float x=40+width,size=w-x-16;
            float actual=Math.max(1,(int)(size*scale/64))*64/scale;
            artwork(entry.cart,x+(size-actual)/2,150,actual);}
        if(entry==null){text("Make something yours",16,189,20,14);wrap("Start with a blank project, or import a .p8 game.",16,225,w-32,6,3);}
        if(entry!=null)fit(entry.title,16,h-109,20,7,w-32);
        fit(entry==null?"No project needed to play games.":(s.selected+1)+" / "+s.entries().size()+" projects"+(w<500?"":"  ·  Select for actions"),16,h-79,18,6,w-32);
        footer(entry==null?"New":"Open",w<500?"↑↓":"↑↓ Move",Action.DOWN);
        if(s.mode!=LibrarySession.Mode.SHELF)dialog();else menu(s.menu);
        c.restore();
    }
    private void artwork(WorkshopCartridge cart,float x,float y,float size){
        float unit=size/64;c.save();c.translate(x,y);c.scale(unit,unit);
        rect(0,0,64,64,1);
        if(cart!=null&&!cart.hasHero()){
            rect(8,6,48,52,2);outline(8,6,48,52,13);
            for(int i=0;i<4;i++)for(int py=0;py<16;py++)for(int px=0;px<16;px++){
                int color=cart.pixel(i,px,py);if(color!=0)rect(14+(i%2)*20+px,12+(i/2)*20+py,1,1,color);
            }
            c.restore();return;
        }
        for(int i=0;i<13;i++)rect((i*19+3)%64,(i*11+2)%32,1,1,i%3==0?7:13);
        rect(47,6,7,9,15);rect(50,4,6,8,1);rect(0,49,64,15,2);rect(0,49,64,3,3);
        for(int i=0;i<5;i++){rect(3+i*13,54,4,2,4);rect(5+i*13,45,1,4,3);rect(4+i*13,44,3,2,14);}
        if(cart!=null){
            HeroBinding hero=cart.hero();SpriteRegion r=hero.image;
            float cell=Math.min(1,Math.min(48f/r.width,38f/r.height));
            float hx=32-(hero.left+hero.width/2f)*cell,hy=49-(hero.top+hero.height)*cell;
            for(int py=0;py<r.height;py++)for(int px=0;px<r.width;px++){int color=cart.pixel(r,px,py);if(color!=0)rect(hx+px*cell,hy+py*cell,cell,cell,color);}
        }
        else {rect(28,22,7,15,9);rect(28,40,7,4,9);}
        c.restore();
    }
    private void dialog(){
        hits.clear();p.setColor(0xdd000000);c.drawRect(0,0,w,h,p);
        if(s.exporting!=null&&s.mode!=LibrarySession.Mode.ERROR){exportDialog();return;}
        if(s.mode==LibrarySession.Mode.IMPORT||s.mode==LibrarySession.Mode.READING){importDialog();return;}
        float dw=Math.min(w-32,460),x=(w-dw)/2,y=(h-300)/2;
        rect(x,y,dw,300,1);rect(x,y,dw,3,14);
        boolean create=s.mode==LibrarySession.Mode.CREATE;
        if(s.mode==LibrarySession.Mode.COPY){
            text("Copy project",x+18,y+40,24,14);
            wrap(s.current().title,x+18,y+84,dw-36,7,2);
            wrap("Create a separate editable copy. The original stays unchanged.",x+18,y+145,dw-36,6,3);
            button(ok()+" Copy",x+16,y+238,(dw-44)/2,true,()->action(Action.CONFIRM));
            button(back()+" Cancel",x+28+(dw-44)/2,y+238,(dw-44)/2,false,()->action(Action.CANCEL));return;
        }
        fit(create?"New project":"Could not complete",x+18,y+40,26,create?14:9,dw-36);
        if(create){
            String[] titles={"Moon Garden","Blank","Lights"};
            String[] descriptions={"A character, jumping and platforms.","An empty game for your ideas.","Light up the board. Edit its tiles."};
            button("<",x+16,y+58,44,false,()->action(Action.LEFT));
            button(">",x+dw-60,y+58,44,false,()->action(Action.RIGHT));
            fit(titles[s.templateChoice],x+74,y+88,24,7,dw-148);
            wrap(descriptions[s.templateChoice],x+18,y+132,dw-36,7,2);
            text((s.templateChoice+1)+" / "+s.templateCount()+"  ·  ← → choose",x+18,y+182,18,6);
            fit(s.templateChoice==1?"Build your own game.":"A separate project.",x+18,y+208,18,6,dw-36);
        }else{
            text("Your original projects are safe.",x+18,y+82,18,7);
            String message=s.error;
            for(int row=0;row<4&&!message.isEmpty();row++){
                p.setTextSize(PixelText.size(18));int count=message.length();while(count>1&&p.measureText(message.substring(0,count))>dw-36)count--;
                text(message.substring(0,count),x+18,y+119+row*25,18,6);message=message.substring(count);
            }
        }
        button(ok()+(create?" Create":" Back"),x+16,y+238,create?(dw-44)/2:dw-32,true,()->action(Action.CONFIRM));
        if(create)button(back()+" Cancel",x+28+(dw-44)/2,y+238,(dw-44)/2,false,()->action(Action.CANCEL));
    }
    private void exportDialog(){
        float dw=Math.min(w-32,480),x=(w-dw)/2,y=(h-400)/2;
        boolean saved=s.mode==LibrarySession.Mode.EXPORT_SAVED,writing=s.mode==LibrarySession.Mode.EXPORT_WRITING||s.mode==LibrarySession.Mode.EXPORT_PICKER;
        boolean uncertain=s.mode==LibrarySession.Mode.EXPORT_UNCERTAIN;
        rect(x,y,dw,400,1);rect(x,y,dw,3,saved?11:14);
        text(saved?"Export saved":uncertain?"Check the exported file":"Export project",x+18,y+38,24,saved?11:uncertain?9:14);
        artwork(s.exporting.cart,x+18,y+62,96);
        fit(saved?s.exporting.resultName:s.exporting.filename,x+128,y+85,22,10,dw-146);
        fit(s.exporting.title,x+128,y+115,18,7,dw-146);
        text((s.exporting.cart.bytes().length+1023)/1024+" KiB · .p8",x+128,y+144,18,6);
        if(writing){
            text(s.mode==LibrarySession.Mode.EXPORT_PICKER?"Choose a folder in Android…":"Writing and verifying…",x+18,y+205,20,10);
            fit("Your project stays in the workshop.",x+18,y+245,18,6,dw-36);return;
        }
        if(saved){
            text("Copy matches the saved project.",x+18,y+198,20,11);
            fit("Read back and verified after writing.",x+18,y+229,18,7,dw-36);
            fit("Open this file in ordinary PICO-8.",x+18,y+277,18,6,dw-36);
            fit("File verification is not a game test.",x+18,y+307,16,6,dw-36);
            button(ok()+" Done",x+16,y+340,dw-32,true,()->action(Action.CONFIRM));return;
        }
        if(uncertain){
            text("Could not verify the written file.",x+18,y+198,18,9);
            fit("The destination may be incomplete.",x+18,y+229,18,7,dw-36);
            fit("Your workshop project is safe.",x+18,y+266,18,11,dw-36);
            fit("Retry chooses a new destination.",x+18,y+302,16,6,dw-36);
        }else{
            text("The saved version of your project.",x+18,y+198,20,7);
            fit("An ordinary .p8 copy in your folder.",x+18,y+229,18,11,dw-36);
            fit("Exports this cartridge only.",x+18,y+267,18,6,dw-36);
            fit("Linked files and history are excluded.",x+18,y+296,17,6,dw-36);
        }
        button(ok()+(uncertain?" Retry":" Export"),x+16,y+340,(dw-44)/2,true,()->action(Action.CONFIRM));
        button(back()+" Back",x+28+(dw-44)/2,y+340,(dw-44)/2,false,()->action(Action.CANCEL));
    }
    private void importDialog(){
        float dw=Math.min(w-32,480),x=(w-dw)/2,y=(h-400)/2;
        rect(x,y,dw,400,1);rect(x,y,dw,3,14);
        text("Import project",x+18,y+38,24,14);
        if(s.mode==LibrarySession.Mode.READING){
            text("Reading the selected file…",x+18,y+120,22,10);
            fit("Larger files may take a moment.",x+18,y+158,16,6,dw-36);
            button(back()+" Cancel",x+16,y+340,dw-32,true,()->action(Action.CANCEL));return;
        }
        artwork(s.importing.cart,x+18,y+62,96);
        fit(s.importing.filename,x+128,y+85,22,10,dw-146);
        fit("Cartridge sprites",x+128,y+114,16,6,dw-146);
        text((s.importing.cart.bytes().length+1023)/1024+" KiB · .p8",x+128,y+143,18,7);
        text("Creates a separate editable copy.",x+18,y+196,20,7);
        text("The source file stays unchanged.",x+18,y+224,18,11);
        fit("Only this file is imported. Linked",x+18,y+266,17,6,dw-36);
        fit("files and other carts are excluded.",x+18,y+290,17,6,dw-36);
        fit("After importing, use Start to test.",x+18,y+320,16,6,dw-36);
        button(ok()+" Import",x+16,y+340,(dw-44)/2,true,()->action(Action.CONFIRM));
        button(back()+" Cancel",x+28+(dw-44)/2,y+340,(dw-44)/2,false,()->action(Action.CANCEL));
    }
}
