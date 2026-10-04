pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
local function move_box_solid(x,y,w,h)
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
local function move_box_axis(x,y,w,h,d,v)
 local function blocked(t)
  if v then
   return move_box_solid(
    x,min(y,y+t),w,h+abs(t))
  end
  return move_box_solid(
   min(x,x+t),y,w+abs(t),h)
 end
 if not blocked(d) then
  return d,false
 end
 local lo,hi=0,abs(d)
 local dir=sgn(d)
 for i=1,31 do
  if hi-lo<=0x0.0001 then
   break
  end
  local mid=lo+(hi-lo)/2
  if blocked(mid*dir) then
   hi=mid
  else
   lo=mid
  end
 end
 return lo*dir,true
end
function move_box(x,y,w,h,dx,dy)
 if w<=0 or h<=0 then
  return x,y,false,false
 end
 assert(dx>-32768 and dy>-32768,
  "movement step range")
 assert(abs(dx)<=0x7fff.ffff-w
  and abs(dy)<=0x7fff.ffff-h,
  "movement sweep range")
 local nx,ny=x+dx,y+dy
 assert((dx>=0 and nx>=x or
  dx<0 and nx<=x) and
  (dy>=0 and ny>=y or
  dy<0 and ny<=y),
  "movement position overflow")
 if move_box_solid(x,y,w,h) then
  return x,y,true,true
 end
 local mx,hx=move_box_axis(
  x,y,w,h,dx,false)
 x+=mx
 local my,hy=move_box_axis(
  x,y,w,h,dy,true)
 return x,y+my,hx,hy
end
local function free_move_solid(x,y,w,h)
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
local function free_move_axis(x,y,w,h,d,v)
 local function blocked(t)
  if v then
   return free_move_solid(
    x,min(y,y+t),w,h+abs(t))
  end
  return free_move_solid(
   min(x,x+t),y,w+abs(t),h)
 end
 if not blocked(d) then
  return d,false
 end
 local lo,hi=0,abs(d)
 local dir=sgn(d)
 for i=1,31 do
  if hi-lo<=0x0.0001 then
   break
  end
  local mid=lo+(hi-lo)/2
  if blocked(mid*dir) then
   hi=mid
  else
   lo=mid
  end
 end
 return lo*dir,true
end
function free_move(x,y,w,h,dx,dy)
 if w<=0 or h<=0 then
  return x,y,false,false
 end
 assert(dx>-32768 and dy>-32768,
  "movement step range")
 assert(abs(dx)<=0x7fff.ffff-w
  and abs(dy)<=0x7fff.ffff-h,
  "movement sweep range")
 local nx,ny=x+dx,y+dy
 assert((dx>=0 and nx>=x or
  dx<0 and nx<=x) and
  (dy>=0 and ny>=y or
  dy<0 and ny<=y),
  "movement position overflow")
 if free_move_solid(x,y,w,h) then
  return x,y,true,true
 end
 local mx,hx=free_move_axis(
  x,y,w,h,dx,false)
 x+=mx
 local my,hy=free_move_axis(
  x,y,w,h,dy,true)
 return x,y+my,hx,hy
end
checks=0
function check(f,x,y,w,h,dx,dy,ex,ey,ehx,ehy)
 local a,b,c,d=f(x,y,w,h,dx,dy)
 assert(a==ex and b==ey and c==ehx and d==ehy,"move case "..checks..": "..a..","..b)
 checks+=1
end
for y=0,31 do for x=0,127 do mset(x,y,0) end end
for i=0,255 do fset(i,0) end
fset(17,128)
for y=2,12 do mset(4,y,17) end
for x=0,3 do mset(x,10,17) end
check(move_box,8,24,8,8,100,0,24,24,true,false)
check(move_box,48,24,8,8,-40,0,40,24,true,false)
check(move_box,16,64,8,8,0,40,16,72,false,true)
check(move_box,16,96,8,8,0,-40,16,88,false,true)
check(move_box,24,24,8,8,20,16,24,40,true,false)
check(move_box,16,64,8,8,20,20,24,72,true,true)
check(move_box,0,0,8,8,2.25,3.5,2.25,3.5,false,false)
check(move_box,23.75,24,8,8,.5,0,24,24,true,false)
check(move_box,23.75,24,8,8,.25,0,24,24,false,false)
check(move_box,24,24,8,8,0x0.0001,0,24,24,true,false)
check(move_box,31.9999847412109375,24,0x0.0001,8,100,0,31.9999847412109375,24,true,false)
check(move_box,32,24,8,8,1,0,32,24,true,true)
check(move_box,8,24,0,8,100,0,8,24,false,false)
check(move_box,0,0,8,8,-3,0,0,0,true,false)
check(move_box,120,0,8,8,16,0,120,0,true,false)
check(move_box,8,0,8,8,0,0,8,0,false,false)
check(move_box,8,0,8,8,0,-4,8,0,false,true)
check(move_box,8,0,8,8,16,0,24,0,false,false)
check(free_move,-100,24,8,8,200,0,24,24,true,false)
check(free_move,120,0,8,8,16,0,136,0,false,false)
camera(40,50)
check(move_box,8,24,8,8,100,0,24,24,true,false)
camera()
fset(17,64)
check(move_box,8,24,8,8,100,0,108,24,false,false)
fset(17,128)
function _draw()
 cls(1)
 print("movement: "..checks.."/22",8,40,11)
 print("sweep / slide / fractions",8,56,7)
end
