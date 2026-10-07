pico-8 cartridge // http://www.pico-8.com
version 42
__lua__
function _init()
 cartdata("pikonest_runtime_save_check_71")
 runs=dget(0)+1
 dset(0,runs)
end
function _draw()
 cls(1)
 print("PIKONEST SAVE CHECK",24,28,14)
 print("SAVED LAUNCHES: "..runs,28,58,10)
 print("OFFICIAL CARTDATA / DSET",18,82,7)
 print("SELECT: EXIT MENU",30,108,6)
end
