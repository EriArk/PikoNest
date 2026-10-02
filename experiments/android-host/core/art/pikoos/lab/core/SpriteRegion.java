package art.pikoos.lab.core;

/** A pixel rectangle on the ordinary 128x128 PICO-8 sheet, not an allocated object. */
public final class SpriteRegion {
    public final int x, y, width, height;
    public SpriteRegion(int x,int y,int width,int height){
        if(x<0||y<0||width<1||height<1||width>128||height>128||x>128-width||y>128-height)
            throw new IllegalArgumentException("Rectangle outside PICO-8 sprite sheet");
        this.x=x;this.y=y;this.width=width;this.height=height;
    }
    public boolean sharesMap(){return y+height>64;}
    public void checkPixel(int px,int py){
        if(px<0||py<0||px>=width||py>=height)throw new IllegalArgumentException("Pixel outside selected region");
    }
}
