pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
checks=0
function check(x,y,w,h,ex,ey)
 local a,b=camera()
 assert(a==ex and b==ey,"camera case "..checks..": "..a..","..b)
 checks+=1
end
camera(mid(64,(0),max(64,32*8-64))-64,mid(64,(0),max(64,32*8-64))-64)
check(0,0,32,32,0,0)
camera(mid(64,(128),max(64,32*8-64))-64,mid(64,(96),max(64,32*8-64))-64)
check(128,96,32,32,64,32)
camera(mid(64,(256),max(64,32*8-64))-64,mid(64,(256),max(64,32*8-64))-64)
check(256,256,32,32,128,128)
camera(mid(64,(-32768),max(64,128*8-64))-64,mid(64,(0x7fff.ffff),max(64,64*8-64))-64)
check(-32768,0x7fff.ffff,128,64,0,384)
camera(mid(64,(64.75),max(64,32*8-64))-64,mid(64,(80.5),max(64,32*8-64))-64)
check(64.75,80.5,32,32,0,16)
camera(mid(64,(500),max(64,1*8-64))-64,mid(64,(500),max(64,8*8-64))-64)
check(500,500,1,8,0,0)
camera(mid(64,(999),max(64,16*8-64))-64,mid(64,(-20),max(64,16*8-64))-64)
check(999,-20,16,16,0,0)
camera(mid(64,(960),max(64,128*8-64))-64,mid(64,(448),max(64,64*8-64))-64)
check(960,448,128,64,896,384)
camera(mid(64,(192),max(64,32*8-64))-64,mid(64,(192),max(64,32*8-64))-64)
check(192,192,32,32,128,128)
calls=0
function target() calls+=1 return 120 end
camera(mid(64,(target()),max(64,32*8-64))-64,mid(64,(96),max(64,32*8-64))-64)
assert(calls==1,"target once")
checks+=1
camera()
cls(1)
camera(40,20)
rectfill(48,30,50,32,14)
camera()
assert(pget(8,10)==14 and pget(48,30)==1,"world to screen")
checks+=1
rectfill(2,2,4,4,11)
assert(pget(2,2)==11,"fixed hud")
checks+=1
camera(-3,7)
local a,b=camera()
assert(a==-3 and b==7,"reset previous state")
local c,d=camera()
assert(c==0 and d==0,"reset origin")
checks+=1
function _draw()
 cls(1)
 print("camera: "..checks.."/13",8,48,11)
 print("bounds / world / hud",8,64,7)
end
