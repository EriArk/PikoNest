pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
checks=0
for x=0,127 do sset(x,0,x%14+1) end
camera() cls(0) camera(0,17)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=3
 local _bg_phase=flr(
  ((((0)%_bg_w)*0)%_bg_w
  -(_bg_cx*0.5)%_bg_w)%_bg_w)
 if true then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,3,1,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
local cx,cy=camera()
assert(cx==0 and cy==17,"camera restore")
local expected="12312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312312"
for x=0,127 do assert(pget(x,24)==tonum("0x"..sub(expected,x+1,x+1)),"stripe "..checks.." x "..x) end
checks+=1
camera() cls(0) camera(64,17)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=3
 local _bg_phase=flr(
  ((((1)%_bg_w)*4)%_bg_w
  -(_bg_cx*0.5)%_bg_w)%_bg_w)
 if true then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,3,1,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
local cx,cy=camera()
assert(cx==64 and cy==17,"camera restore")
local expected="23123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123"
for x=0,127 do assert(pget(x,24)==tonum("0x"..sub(expected,x+1,x+1)),"stripe "..checks.." x "..x) end
checks+=1
camera() cls(0) camera(-31,17)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=3
 local _bg_phase=flr(
  ((((5)%_bg_w)*-3)%_bg_w
  -(_bg_cx*0.25)%_bg_w)%_bg_w)
 if true then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,3,1,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
local cx,cy=camera()
assert(cx==-31 and cy==17,"camera restore")
local expected="31231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231231"
for x=0,127 do assert(pget(x,24)==tonum("0x"..sub(expected,x+1,x+1)),"stripe "..checks.." x "..x) end
checks+=1
camera() cls(0) camera(32767,17)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=16
 local _bg_phase=flr(
  ((((1)%_bg_w)*32)%_bg_w
  -(_bg_cx*1)%_bg_w)%_bg_w)
 if true then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,16,1,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
local cx,cy=camera()
assert(cx==32767 and cy==17,"camera restore")
local expected="2123456789abcde12123456789abcde12123456789abcde12123456789abcde12123456789abcde12123456789abcde12123456789abcde12123456789abcde1"
for x=0,127 do assert(pget(x,24)==tonum("0x"..sub(expected,x+1,x+1)),"stripe "..checks.." x "..x) end
checks+=1
camera() cls(0) camera(-32768,17)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=16
 local _bg_phase=flr(
  ((((1)%_bg_w)*-32)%_bg_w
  -(_bg_cx*1)%_bg_w)%_bg_w)
 if true then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,16,1,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
local cx,cy=camera()
assert(cx==-32768 and cy==17,"camera restore")
local expected="123456789abcde12123456789abcde12123456789abcde12123456789abcde12123456789abcde12123456789abcde12123456789abcde12123456789abcde12"
for x=0,127 do assert(pget(x,24)==tonum("0x"..sub(expected,x+1,x+1)),"stripe "..checks.." x "..x) end
checks+=1
camera() cls(0) camera(32767,17)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=128
 local _bg_phase=flr(
  ((((32760)%_bg_w)*32)%_bg_w
  -(_bg_cx*0.75)%_bg_w)%_bg_w)
 if true then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,128,1,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
local cx,cy=camera()
assert(cx==32767 and cy==17,"camera restore")
local expected="123456789abcde123456789abcde123456789abcde123456789abcde123456789abcde123456789abcde123456789abcde123456789abcde123456789abcde12"
for x=0,127 do assert(pget(x,24)==tonum("0x"..sub(expected,x+1,x+1)),"stripe "..checks.." x "..x) end
checks+=1
camera() cls(0) camera(-32768,17)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=7
 local _bg_phase=flr(
  ((((-32768)%_bg_w)*-32)%_bg_w
  -(_bg_cx*0.25)%_bg_w)%_bg_w)
 if true then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,7,1,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
local cx,cy=camera()
assert(cx==-32768 and cy==17,"camera restore")
local expected="23456712345671234567123456712345671234567123456712345671234567123456712345671234567123456712345671234567123456712345671234567123"
for x=0,127 do assert(pget(x,24)==tonum("0x"..sub(expected,x+1,x+1)),"stripe "..checks.." x "..x) end
checks+=1
camera() cls(0) camera(0,17)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=1
 local _bg_phase=flr(
  ((((0)%_bg_w)*0)%_bg_w
  -(_bg_cx*0)%_bg_w)%_bg_w)
 if true then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,1,1,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
local cx,cy=camera()
assert(cx==0 and cy==17,"camera restore")
local expected="11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"
for x=0,127 do assert(pget(x,24)==tonum("0x"..sub(expected,x+1,x+1)),"stripe "..checks.." x "..x) end
checks+=1
camera() cls(2)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=16
 local _bg_phase=flr(
  (((time()%_bg_w)*4)%_bg_w
  -(_bg_cx*0.5)%_bg_w)%_bg_w)
 if false then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,16,16,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
assert(pget(0,24)==2,"hidden") checks+=1
camera() cls(0) palt(1,true) pal(2,8) clip(10,24,10,1)
do
 local _bg_cx,_bg_cy=camera()
 local _bg_w=16
 local _bg_phase=flr(
  (((time()%_bg_w)*0)%_bg_w
  -(_bg_cx*0)%_bg_w)%_bg_w)
 if true then
  for _bg_dx=_bg_phase-_bg_w,127,_bg_w do
   sspr(0,0,16,1,_bg_dx,24)
  end
 end
 camera(_bg_cx,_bg_cy)
end
assert(pget(0,24)==0 and pget(16,24)==0 and pget(17,24)==8 and pget(20,24)==0,"draw state")
rectfill(0,0,127,127,11)
assert(pget(10,24)==11 and pget(20,24)==0,"clip unchanged")
checks+=1
clip() pal() palt()
function _draw()
 cls(1) print("background: "..checks.."/10",8,48,11)
 print("repeat / camera / state",8,64,7)
end
