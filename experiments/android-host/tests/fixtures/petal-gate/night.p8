pico-8 cartridge // http://www.pico-8.com
version 42
__lua__
function _init()
 reload(0,0,1,"colors.p8")
 petals=peek(0)
 assert(petals==14,"linked data not loaded")
 assert(stat(6)=="petals=3","parameter not passed")
end
function _draw()
 cls(1)
 print("petal gate",44,12,petals)
 print("chapter 2: the stars",26,26,6)
 circfill(64,62,15,10)
 circfill(70,56,13,1)
 pset(33,48,7) pset(94,73,7)
 print("linked data: "..petals,36,93,11)
 print("x: back to garden",30,108,7)
end
function _update()
 if btnp(5) then load("Petal-Gate.p8") end
end
