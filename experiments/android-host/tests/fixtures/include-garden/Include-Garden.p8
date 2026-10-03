pico-8 cartridge // http://www.pico-8.com
version 42
__lua__
#include code/palette.lua
#include shared.p8:1
#include behavior.p8
function _draw()
 cls(bg)
 print("include garden",36,12,14)
 print("lua + cart + tab",32,24,6)
 draw_flower(64,61)
 print("moves: "..moves,48,92,7)
 print("o: grow",50,108,10)
end
