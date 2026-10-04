pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
checks=0
for y=0,127 do for x=0,127 do sset(x,y,0) end end
for y=0,7 do for x=0,7 do sset(x,y,8) end end
for y=0,15 do for x=16,39 do sset(x,y,11) end end
function check_0(t,c,w,h)
 cls(1)
 local time=function() return t end
do
 -- animation: sx,sy,w,h,end_seconds
 local frames={
  {0,0,8,8,0x0.4000},
  {16,0,24,16,0x0.c000},
 }
 local elapsed=time()
 elapsed%=0x0.c000
 for i=1,#frames do
  local f=frames[i]
  if elapsed<f[5] or i==#frames then
   sspr(f[1],f[2],f[3],f[4],
    20,30)
   break
  end
 end
end
 assert(pget(20,30)==c and pget(20+w-1,30+h-1)==c,"frame "..checks)
 assert(pget(20+w,30)==1 and pget(20,30+h)==1,"bounds "..checks)
 checks+=1
end
function check_1(t,c,w,h)
 cls(1)
 local time=function() return t end
do
 -- animation: sx,sy,w,h,end_seconds
 local frames={
  {0,0,8,8,0x0.4000},
  {16,0,24,16,0x0.c000},
 }
 local elapsed=time()
 for i=1,#frames do
  local f=frames[i]
  if elapsed<f[5] or i==#frames then
   sspr(f[1],f[2],f[3],f[4],
    20,30)
   break
  end
 end
end
 assert(pget(20,30)==c and pget(20+w-1,30+h-1)==c,"frame "..checks)
 assert(pget(20+w,30)==1 and pget(20,30+h)==1,"bounds "..checks)
 checks+=1
end
check_0(0,8,8,8)
check_0(0x0.3fff,8,8,8)
check_0(.25,11,24,16)
check_0(0x0.bfff,11,24,16)
check_0(.75,8,8,8)
check_0(1,11,24,16)
check_0(30000,8,8,8)
check_0(30000.25,11,24,16)
check_1(0,8,8,8)
check_1(.25,11,24,16)
check_1(.75,11,24,16)
check_1(30000,11,24,16)
function small(t,c)
 cls(1)
 local time=function() return t end
do
 -- animation: sx,sy,w,h,end_seconds
 local frames={
  {0,0,8,8,0x0.0ccc},
  {16,0,24,16,0x0.2666},
 }
 local elapsed=time()
 elapsed%=0x0.2666
 for i=1,#frames do
  local f=frames[i]
  if elapsed<f[5] or i==#frames then
   sspr(f[1],f[2],f[3],f[4],
    20,30)
   break
  end
 end
end
 assert(pget(20,30)==c,"fraction "..checks) checks+=1
end
small(0x0.0ccb,8)
small(0x0.0ccc,11)
small(0x0.2665,11)
small(0x0.2666,8)
function _draw()
 cls(1)
 print("animation: "..checks.."/16",8,40,11)
 print("timing / loop / regions",8,56,7)
end
