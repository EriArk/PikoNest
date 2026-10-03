package art.pikoos.lab.core;

import java.util.Locale;

/** Runtime transport format; editable projects remain ordinary text .p8. */
public enum CartridgeFormat {
    P8(".p8","text/plain"), P8_PNG(".p8.png","image/png");
    public final String extension,mime;
    CartridgeFormat(String extension,String mime){this.extension=extension;this.mime=mime;}
    public static CartridgeFormat of(String filename){
        String lower=filename.toLowerCase(Locale.ROOT);
        if(lower.endsWith(P8_PNG.extension))return P8_PNG;
        if(lower.endsWith(P8.extension))return P8;
        throw new IllegalArgumentException("Выбери .p8 или .p8.png");
    }
}
