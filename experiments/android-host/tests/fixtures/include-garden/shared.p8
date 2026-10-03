pico-8 cartridge // http://www.pico-8.com
version 42
__lua__
assert(false,"wrong tab included")
-->8
function draw_flower(x,y)
 rectfill(x-1,y,x+1,y+21,3)
 ovalfill(x-12,y+7,x,y+13,leaf)
 ovalfill(x,y+12,x+12,y+18,leaf)
 for a=0,3 do
  local t=a/4
  circfill(x+cos(t)*7,y+sin(t)*7,5,petal)
 end
 circfill(x,y,4,10)
end
