pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
checks=0
function step()
do
 local _gate_x,_gate_y=x,y
 local _gate_a=_gate_x>=96 and _gate_x<96+16
  and _gate_y>=48 and _gate_y<48+32
 local _gate_b=_gate_x>=160 and _gate_x<160+16
  and _gate_y>=48 and _gate_y<48+32
 if not _gate_a and not _gate_b then
  gate_busy=false
 elseif not gate_busy then
  gate_busy=true
  if _gate_a then
   x,y=160,48
  else
   x,y=96,48
  end
 end
end
end
function check(a,b,c)
 assert(x==a and y==b and gate_busy==c,"gate "..checks)
 checks+=1
end
x=64 y=64 step() check(64,64,false)
x=96 y=48 step() check(160,48,true)
for i=1,120 do step() end
check(160,48,true)
x=175 y=79 step() check(175,79,true)
x=176 step() check(176,79,false)
x=160 step() check(96,48,true)
x=112 step() check(112,48,false)
x=96 y=80 step() check(96,80,false)
y=47 step() check(96,47,false)
x=95 y=48 step() check(95,48,false)
x=111.999 y=79.999 step() check(160,48,true)
x=-32768 y=-32768 step() check(-32768,-32768,false)
x=0x7fff.ffff y=0x7fff.ffff step() check(x,y,false)
gate_busy=nil x=160 y=48 step() check(96,48,true)
x=-16384 y=-16384
do
 local _gate_x,_gate_y=x,y
 local _gate_a=_gate_x>=-16384 and _gate_x<-16384+1024
  and _gate_y>=-16384 and _gate_y<-16384+1024
 local _gate_b=_gate_x>=16383 and _gate_x<16383+1024
  and _gate_y>=16383 and _gate_y<16383+1024
 if not _gate_a and not _gate_b then
  other_busy=false
 elseif not other_busy then
  other_busy=true
  if _gate_a then
   x,y=16383,16383
  else
   x,y=-16384,-16384
  end
 end
end
assert(x==16383 and y==16383 and other_busy,"extreme endpoints")
checks+=1
assert(gate_busy,"independent latch")
checks+=1
x=96 y=48 gate_busy=false step()
camera(flr(mid(0,(x),2*128-1)/128)*128,flr(mid(0,(y),2*128-1)/128)*128)
local cx,cy=camera()
assert(cx==128 and cy==0,"arrival camera")
checks+=1
function _draw()
 cls(1)
 print("doors: "..checks.."/17",8,48,11)
 print("entry / exit / lock / camera",8,64,7)
end
