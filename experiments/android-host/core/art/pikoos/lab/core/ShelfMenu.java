package art.pikoos.lab.core;

import art.pikoos.lab.core.WorkshopSession.Action;

/** Shared portable shelf commands. The view and controller consume the same list. */
public final class ShelfMenu {
    public enum Command {
        PLAY("Play"), WORKSHOP("Workshop"), NEW("New project"), COPY("Copy project"),
        IMPORT("Import .p8"), EXPORT("Export .p8"), FILTER("Change filter"),
        REFRESH("Refresh games"), FOLDERS("Folders & runtime");
        public final String label;
        Command(String label){this.label=label;}
    }
    public final Command[] commands;
    public boolean open;
    public int selected;
    public ShelfMenu(Command... commands){this.commands=commands.clone();}
    public void show(){open=true;selected=0;}
    public Command act(Action action){
        if(action==Action.UP)selected=Math.max(0,selected-1);
        if(action==Action.DOWN)selected=Math.min(commands.length-1,selected+1);
        if(action==Action.CANCEL||action==Action.MENU)open=false;
        if(action==Action.CONFIRM){open=false;return commands[selected];}
        return null;
    }
}
