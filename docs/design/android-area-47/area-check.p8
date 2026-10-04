pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
checks=0
function check(ok,why)
 assert(ok,why)
 checks+=1
end
function free_box(x,y,w,h)
 if w<=0 or h<=0 then
  return false
 end
 if x>=128 or y>=128 then
  return false
 end
 if x<0 then w+=x x=0 end
 if y<0 then h+=y y=0 end
 if w<=0 or h<=0 then
  return false
 end
 w=min(w,128-x)
 h=min(h,128-y)
 local r=ceil(x+w)-1
 local b=ceil(y+h)-1
 for j=flr(y/8),flr(b/8) do
  for i=flr(x/8),flr(r/8) do
   if fget(mget(i,j),7) then
    return true
   end
  end
 end
 return false
end
function wall_box(x,y,w,h)
 if w<=0 or h<=0 then
  return false
 end
 if x<0 or y<0 or
  x>=128 or y>=128 then
  return true
 end
 if w>128-x or h>128-y then
  return true
 end
 local r=ceil(x+w)-1
 local b=ceil(y+h)-1
 for j=flr(y/8),flr(b/8) do
  for i=flr(x/8),flr(r/8) do
   if fget(mget(i,j),7) then
    return true
   end
  end
 end
 return false
end
function large_box(x,y,w,h)
 if w<=0 or h<=0 then
  return false
 end
 if x>=1024 or y>=512 then
  return false
 end
 if x<0 then w+=x x=0 end
 if y<0 then h+=y y=0 end
 if w<=0 or h<=0 then
  return false
 end
 w=min(w,1024-x)
 h=min(h,512-y)
 local r=ceil(x+w)-1
 local b=ceil(y+h)-1
 for j=flr(y/8),flr(b/8) do
  for i=flr(x/8),flr(r/8) do
   if fget(mget(i,j),7) then
    return true
   end
  end
 end
 return false
end
function six_box(x,y,w,h)
 if w<=0 or h<=0 then
  return false
 end
 if x>=128 or y>=128 then
  return false
 end
 if x<0 then w+=x x=0 end
 if y<0 then h+=y y=0 end
 if w<=0 or h<=0 then
  return false
 end
 w=min(w,128-x)
 h=min(h,128-y)
 local r=ceil(x+w)-1
 local b=ceil(y+h)-1
 for j=flr(y/8),flr(b/8) do
  for i=flr(x/8),flr(r/8) do
   if fget(mget(i,j),6) then
    return true
   end
  end
 end
 return false
end
for y=0,63 do for x=0,127 do mset(x,y,0) end end
for i=0,255 do fset(i,0) end
fset(17,128) mset(4,5,17)
check(free_box(32,40,8,8)==true,"free0")
check(free_box(24,40,8,8)==false,"free1")
check(free_box(24,40,8.0000152587890625,8)==true,"free2")
check(free_box(32,32,8,8)==false,"free3")
check(free_box(32,32,8,8.0000152587890625)==true,"free4")
check(free_box(40,40,8,8)==false,"free5")
check(free_box(16,24,40,40)==true,"free6")
check(free_box(0,0,0,8)==false,"free7")
check(free_box(32,40,-1,8)==false,"free8")
check(free_box(32,40,8,-1)==false,"free9")
check(free_box(32.5,40.5,0x0.0001,0x0.0001)==true,"free10")
check(free_box(-4,40,40,8)==true,"free11")
check(free_box(-40,40,40,8)==false,"free12")
check(free_box(128,0,8,8)==false,"free13")
check(free_box(0,128,8,8)==false,"free14")
check(free_box(0,0,32767,32767)==true,"free15")
check(free_box(32760,0,16,16)==false,"free16")
check(free_box(-32768,40,32767,8)==false,"free17")
check(free_box(120,40,32767,8)==false,"free18")
check(free_box(0,0,16,16)==false,"free19")
check(free_box(127.5,127.5,1,1)==false,"free20")
check(wall_box(-1,0,8,8),"boundary")
check(wall_box(128,0,8,8),"boundary")
check(wall_box(0,128,8,8),"boundary")
check(wall_box(120,40,32767,8),"boundary")
check(wall_box(32760,0,16,16),"boundary")
check(wall_box(127.5,127.5,1,1),"boundary")
check(not wall_box(-1,0,0,8),"empty before boundary")
check(not wall_box(0,0,128,128)==false,"internal tile")
camera(30,50)
check(free_box(32,40,8,8),"world coordinates")
camera()
fset(17,64)
check(not free_box(32,40,8,8),"chosen bit only")
check(six_box(32,40,8,8),"flag six")
fset(0,128)
check(free_box(0,0,1,1),"tile zero")
check(not free_box(-8,0,8,8),"empty clip")
fset(0,0)
fset(255,128) mset(127,63,255)
check(large_box(1016,504,8,8),"shared last tile")
check(not large_box(1024,504,8,8),"last boundary")
function _draw()
 cls(1)
 print("area checks: "..checks.."/36",8,40,11)
 print("edges / overlap / shared map",8,56,7)
end
