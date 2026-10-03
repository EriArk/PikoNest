pico-8 cartridge // http://www.pico-8.com
version 42
__lua__
function _draw()
 cls(1)
 print("petal gate",44,12,14)
 print("chapter 1: the garden",24,26,6)
 rectfill(35,49,91,88,3)
 rectfill(51,43,75,83,4)
 rectfill(55,47,71,83,9)
 circfill(65,68,2,10)
 print("o: enter the gate",32,105,7)
end
function _update()
 if btnp(4) then load("night.p8","garden","petals=3") end
end
