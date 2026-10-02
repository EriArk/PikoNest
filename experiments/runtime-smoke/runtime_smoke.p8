pico-8 cartridge // http://www.pico-8.com
version 42
__lua__
-- pikoos runtime smoke test v1
-- ordinary pico-8; no host extensions
function _init()
 x=64 y=67 ticks=0
 counts={0,0,0,0,0,0}
 previous={false,false,false,
  false,false,false}
end

function _update60()
 ticks=(ticks+1)%3600
 for i=0,5 do
  local down=btn(i)
  if down and not previous[i+1] then
   counts[i+1]+=1
   if i==4 then sfx(0,0,0,4) end
   if i==5 then sfx(0,0,2,2) end
  end
  previous[i+1]=down
 end
 if btn(0) then x-=1 end
 if btn(1) then x+=1 end
 if btn(2) then y-=1 end
 if btn(3) then y+=1 end
 x=mid(7,x,120) y=mid(51,y,81)
end

function _draw()
 cls(1)
 rect(0,0,127,127,6)
 print("pikoos runtime check",5,5,7)
 print("v1 / standard .p8",5,13,6)
 print("d-pad: move the dot",5,24,7)
 print("o / x: separate counts",5,32,7)
 print("either button: sound",5,40,7)
 rect(4,48,123,84,5)
 circfill(x,y,3,11)
 local names={"l","r","u","d","o","x"}
 for i=0,5 do
  local px=5+(i%3)*40
  local py=90+flr(i/3)*9
  print(names[i+1]..":"..counts[i+1],
   px,py,btn(i) and 10 or 7)
 end
 print("time:"..flr(ticks/60).."s",5,109,6)
 rectfill(5,119,5+ticks%118,122,12)
end
__sfx__
00080000183501c3501f3502435000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
