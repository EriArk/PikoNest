package art.pikoos.lab.core;

/** Image and a rectangular collision body, expressed in ordinary PICO-8 pixels. */
public final class HeroBinding {
    public final SpriteRegion image;
    public final int left,top,width,height;
    public HeroBinding(SpriteRegion image,int left,int top,int width,int height){
        if(left<0||top<0||width<1||height<1||width>image.width||height>image.height
            ||left>image.width-width||top>image.height-height)throw new IllegalArgumentException("Body outside hero image");
        this.image=image;this.left=left;this.top=top;this.width=width;this.height=height;
    }
    public static HeroBinding legacy(int slot){return new HeroBinding(new SpriteRegion(slot*16,0,16,16),4,0,8,16);}
    public static HeroBinding fromPixels(WorkshopCartridge cart,SpriteRegion image){
        int left=image.width,top=image.height,right=-1,bottom=-1;
        for(int y=0;y<image.height;y++)for(int x=0;x<image.width;x++)if(cart.pixel(image,x,y)!=0){
            left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);
        }
        if(right<0)throw new IllegalArgumentException("Сначала нарисуй хотя бы один пиксель");
        return new HeroBinding(image,left,top,right-left+1,bottom-top+1);
    }
    public boolean sameImage(SpriteRegion r){return image.x==r.x&&image.y==r.y&&image.width==r.width&&image.height==r.height;}
    public boolean overlaps(SpriteRegion r){return image.x<r.x+r.width&&r.x<image.x+image.width&&image.y<r.y+r.height&&r.y<image.y+image.height;}
    public int card(){return image.y==0&&image.x%16==0&&image.width==16&&image.height==16?image.x/16:-1;}
    public int spawnX(){return Math.max(0,Math.min(128-width,(int)Math.floor(54-width/2.0)));}
    public int spawnY(){return 101-height;}
}
