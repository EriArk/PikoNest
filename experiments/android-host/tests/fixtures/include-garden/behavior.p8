pico-8 cartridge // http://www.pico-8.com
version 42
__lua__
moves=0
-->8
function _update()
 if btnp(4) then
  moves+=1
  petal=8+moves%8
 end
end
