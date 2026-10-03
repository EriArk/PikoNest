pico-8 cartridge // http://www.pico-8.com
version 42
__lua__
-- owned diagnostic: sample official oval pixels
-- output is data, never game or editor state
function _init()
 local widths,heights={},{}
 for n=1,32 do add(widths,n) add(heights,n) end
 for n in all({63,64,65,95,96,127,128}) do add(widths,n) end
 for n in all({47,48,63,64}) do add(heights,n) end
 printh("pikoos oval oracle 0.2.7")
 for variant=0,1 do
 for filled=0,1 do
  for w in all(widths) do
   for h in all(heights) do
    local ox,oy=0,0
    if variant==1 then ox=128-w oy=64-h end
    local x0,y0,x1,y1=ox,oy,ox+w-1,oy+h-1
    if variant==1 then x0,x1=x1,x0 y0,y1=y1,y0 end
    cls(0)
    if filled==1 then ovalfill(x0,y0,x1,y1,7)
    else oval(x0,y0,x1,y1,7) end
    local out=filled..","..w..","..h..","..variant..":"
    for y=0,h-1 do
     for x=0,w-1 do out..=pget(ox+x,oy+y)==7 and "1" or "0" end
    end
    printh(out)
   end
  end
 end
 end
 cls(1)
 printh("pikoos oval oracle complete")
 print("oval samples saved",25,58,11)
end
