pico-8 cartridge // http://www.pico-8.com
version 42
__lua__
x=64
y=66
stars=0
function _update()
 if btn(0) then x=max(12,x-1) end
 if btn(1) then x=min(115,x+1) end
 if btn(2) then y=max(44,y-1) end
 if btn(3) then y=min(94,y+1) end
 if btnp(4) then stars+=1 end
end
function _draw()
 cls(1)
 print("HELLO, PICO-8!",38,14,14)
 print("YOUR RUNTIME IS RUNNING",20,28,7)
 rect(8,39,119,101,13)
 circfill(x,y,7,10)
 pset(x-2,y-1,1)
 pset(x+2,y-1,1)
 line(x-2,y+3,x+2,y+3,1)
 print("DPAD: MOVE   O: "..stars,21,108,7)
 print("SELECT: EXIT MENU",30,119,6)
end
