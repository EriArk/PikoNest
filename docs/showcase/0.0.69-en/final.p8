pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
-- a blank canvas / pikoos
function _draw()
 cls(1)
 print(score,16,16,7)
 print(phase,16,32,10)
end
function _init()
 score=0
 phase=0
end
function _update()
 if btnp(4) and (phase==0) then
  score+=1
 end
 if true and (score>=5) then
  phase=1
 end
 if btnp(5) then
  _init()
 end
end
