pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
checks=0
function check(ex,ey)
 local x,y=camera()
 assert(x==ex and y==ey,"room "..checks..": "..x..","..y)
 checks+=1
end
camera(flr(mid(0,(0),2*128-1)/128)*128,flr(mid(0,(0),2*128-1)/128)*128)
check(0,0)
camera(flr(mid(0,(0x7f.ffff),2*128-1)/128)*128,flr(mid(0,(0x7f.ffff),2*128-1)/128)*128)
check(0,0)
camera(flr(mid(0,(128),2*128-1)/128)*128,flr(mid(0,(127),2*128-1)/128)*128)
check(128,0)
camera(flr(mid(0,(127),2*128-1)/128)*128,flr(mid(0,(128),2*128-1)/128)*128)
check(0,128)
camera(flr(mid(0,(128),2*128-1)/128)*128,flr(mid(0,(128),2*128-1)/128)*128)
check(128,128)
camera(flr(mid(0,(-1),2*128-1)/128)*128,flr(mid(0,(-32768),2*128-1)/128)*128)
check(0,0)
camera(flr(mid(0,(256),2*128-1)/128)*128,flr(mid(0,(256),2*128-1)/128)*128)
check(128,128)
camera(flr(mid(0,(0x7fff.ffff),8*128-1)/128)*128,flr(mid(0,(0x7fff.ffff),4*128-1)/128)*128)
check(896,384)
camera(flr(mid(0,(200),1*128-1)/128)*128,flr(mid(0,(200),1*128-1)/128)*128)
check(0,0)
camera(flr(mid(0,(384),3*128-1)/128)*128,flr(mid(0,(256),2*128-1)/128)*128)
check(256,128)
camera(flr(mid(0,(255.5),8*128-1)/128)*128,flr(mid(0,(383.5),4*128-1)/128)*128)
check(128,256)
camera(flr(mid(0,(256),8*128-1)/128)*128,flr(mid(0,(384),4*128-1)/128)*128)
check(256,384)
camera(flr(mid(0,(127.5),2*128-1)/128)*128,flr(mid(0,(0),2*128-1)/128)*128)
check(0,0)
camera(flr(mid(0,(128.5),2*128-1)/128)*128,flr(mid(0,(0),2*128-1)/128)*128)
check(128,0)
calls=0
function target() calls+=1 return 128 end
camera(flr(mid(0,(target()),2*128-1)/128)*128,flr(mid(0,(target()),2*128-1)/128)*128)
assert(calls==2,"targets once")
check(128,128)
cls(1)
camera(flr(mid(0,(128),2*128-1)/128)*128,flr(mid(0,(0),2*128-1)/128)*128)
rectfill(132,8,135,11,14)
camera()
assert(pget(4,8)==14 and pget(124,8)==1,"world offset")
checks+=1
rectfill(2,2,5,5,11)
assert(pget(2,2)==11,"hud")
checks+=1
camera(33,44)
camera(flr(mid(0,(0),2*128-1)/128)*128,flr(mid(0,(0),2*128-1)/128)*128)
check(0,0)
function _draw()
 cls(1)
 print("rooms: "..checks.."/18",8,48,11)
 print("edges / fractions / hud",8,64,7)
end
