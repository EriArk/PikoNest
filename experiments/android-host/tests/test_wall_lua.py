"""Execute generated wall Lua in desktop Lua 5.4 with a small PICO-8 API shim.

This checks the emitted program, not official PICO-8 execution, graphics, timing,
fixed-point limits or device input. Integer collision values stay in PICO-8 range.
Run WallWorkflowTest first, then this script with its generated maze.p8 path.
"""
import argparse
import ctypes
import ctypes.util
from pathlib import Path
import re


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('cart', type=Path)
    parser.add_argument('--lua-library', default=ctypes.util.find_library('lua5.4'))
    args = parser.parse_args()
    if not args.lua_library:
        parser.error('Lua 5.4 shared library is required; pass --lua-library')
    cart = args.cart.read_text()
    source = cart.split('__lua__\n', 1)[1].split('\n__', 1)[0]
    # The generated recipe uses +=, the only PICO-8 syntax expansion here.
    source = re.sub(r'\b([a-zA-Z_][a-zA-Z_0-9]*)\+=', r'\1=\1+', source)
    map_text = cart.split('__map__\n', 1)[1].split('\n__', 1)[0].splitlines()
    cells = []
    for y, row in enumerate(map_text):
        for x in range(len(row) // 2):
            value = int(row[x*2:x*2+2], 16)
            if value:
                cells.append(f'cells[{y*128+x}]={value}')
    api = '''
checks=0
function check(value,message) checks=checks+1 assert(value,message) end
keys={} cells={} sounds={} music_events={}
function btn(n) return keys[n] or false end
function mid(a,b,c) return math.max(a,math.min(b,c)) end
flr=math.floor abs=math.abs max=math.max
-- PICO-8 sgn(0) is 1, not 0.
function sgn(n) return n<0 and -1 or 1 end
function mget(x,y) return cells[y*128+x] or 0 end
function sfx(n,c) sounds[#sounds+1]=n end function music(n,...) music_events[#music_events+1]=n end
function cls(...) end function camera(...) end function map(...) end
function sspr(...) end function rect(...) end function rectfill(...) end
function print(...) end
function tick(key,n)
 keys={} if key then keys[key]=true end
 for i=1,n or 1 do _update() end
end
'''
    assertions = '''
_init()
check(sprite1_speed==4 and sprite1_x==8,"authored movement")
if game_start_sfx then check(sounds[#sounds]==game_start_sfx,"start sound retained") end
tick(1,20)
check(sprite1_x==24 and sprite1_y==8,"wall blocks; zero axis stays still")
if sprite1_animation==1 then check(sprite1_active==0,"blocked movement stops animation") end
tick(3,8)
check(sprite1_x==24 and sprite1_y==40,"slide along wall")
if sprite1_animation==1 then check(sprite1_active==1,"sliding starts movement animation") end
tick(1,12)
check(sprite1_x==72 and sprite1_y==40,"pass below wall")
tick(2,8)
check(game_result==1 and game_score==1 and sprite2_visible==0,"collect and win")
if game_win_sfx then check(sounds[#sounds]==game_win_sfx,"win sound retained") end
if game_music then check(music_events[#music_events]==-1,"win stops music") end
tick(1,3) check(sprite1_x==72,"result freezes movement")
tick(5) check(game_result==0 and game_score==0 and sprite2_visible==1,"restart restores game")
tick(1,20) tick(3,8) tick(1,12) tick(3,4)
check(game_result==2,"hazard still works with walls")
tick(5) check(sprite1_x==8 and sprite1_y==8,"restart after loss")
_draw()
-- Boundaries, all speeds, all four sides, and full leading edges (not just corners).
world_width=32 world_height=32 world_walls={[255]=true}
for speed=1,4 do
 for _,h in ipairs({1,8,16,24,64,128}) do
  cells={} cells[5*128+10]=255
  local y=math.max(0,40-math.floor(h/2))
  local x1,y1=world_move(72, y, 8,h,speed,0)
  check(x1==72 and y1==y,"right wall / full edge")
  local x2,y2=world_move(88,y,8,h,-speed,0)
  check(x2==88 and y2==y,"left wall / full edge")
 end
 for _,w in ipairs({1,8,16,24,64,128}) do
  cells={} cells[10*128+5]=255
  local x=math.max(0,40-math.floor(w/2))
  local x1,y1=world_move(x,72,w,8,0,speed)
  check(x1==x and y1==72,"bottom wall / full edge")
  local x2,y2=world_move(x,88,w,8,0,-speed)
  check(x2==x and y2==88,"top wall / full edge")
 end
 -- Stop at exact wall surface when speed exceeds remaining gap.
 cells={} cells[0*128+2]=255
 for gap=0,3 do
  local x,y=world_move(8-gap,0,8,8,speed,0)
  check(x==math.min(8,8-gap+speed) and y==0,"partial move; no tunneling")
 end
 cells={}
 for _,direction in ipairs({{-speed,0},{speed,0},{0,-speed},{0,speed}}) do
  local x,y=world_move(0,0,8,8,direction[1],direction[2])
  check(x==math.max(0,direction[1]) and y==math.max(0,direction[2]),"top/left boundary")
  x,y=world_move(248,248,8,8,direction[1],direction[2])
  check(x==math.min(248,248+direction[1]) and y==math.min(248,248+direction[2]),"bottom/right boundary")
 end
end
-- A fixed random seed, independently calculate valid positions from rectangle overlap.
math.randomseed(1102026)
for trial=1,10000 do
 cells={} for n=1,20 do cells[math.random(0,31)*128+math.random(0,31)]=255 end
 local w=math.random(1,32) local h=math.random(1,32)
 local x=math.random(0,256-w) local y=math.random(0,256-h)
 local function blocked(px,py)
  for cell,tile in pairs(cells) do
   local tx=(cell%128)*8 local ty=math.floor(cell/128)*8
   if px<tx+8 and tx<px+w and py<ty+8 and ty<py+h then return true end
  end
  return false
 end
 if not blocked(x,y) then
  local horizontal=math.random(0,1)==1
  local delta=math.random(1,4)*(math.random(0,1)==1 and 1 or -1)
  local dx=horizontal and delta or 0 local dy=horizontal and 0 or delta
  local expectedX=x local expectedY=y
  for distance=1,math.abs(delta) do
   local nx=mid(0,x+(horizontal and distance*(delta<0 and -1 or 1) or 0),256-w)
   local ny=mid(0,y+(horizontal and 0 or distance*(delta<0 and -1 or 1)),256-h)
   if blocked(nx,ny) then break end
   expectedX=nx expectedY=ny
  end
  local actualX,actualY=world_move(x,y,w,h,dx,dy)
  check(actualX==expectedX and actualY==expectedY,"random rectangle sweep")
 end
end
'''
    lua = ctypes.CDLL(args.lua_library)
    lua.luaL_newstate.restype = ctypes.c_void_p
    lua.luaL_openlibs.argtypes = [ctypes.c_void_p]
    lua.luaL_loadstring.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
    lua.lua_pcallk.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_ssize_t, ctypes.c_void_p]
    lua.lua_tolstring.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_void_p]
    lua.lua_tolstring.restype = ctypes.c_char_p
    lua.lua_getglobal.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
    lua.lua_tointegerx.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_void_p]
    lua.lua_tointegerx.restype = ctypes.c_longlong
    lua.lua_close.argtypes = [ctypes.c_void_p]
    state = lua.luaL_newstate()
    try:
        lua.luaL_openlibs(state)
        code = (api + '\n'.join(cells) + '\n' + source + assertions).encode()
        if lua.luaL_loadstring(state, code) or lua.lua_pcallk(state, 0, 0, 0, 0, None):
            raise AssertionError(lua.lua_tolstring(state, -1, None).decode())
        lua.lua_getglobal(state, b'checks')
        print(f'Generated wall Lua: {lua.lua_tointegerx(state, -1, None)} checks passed (desktop Lua, not official PICO-8)')
    finally:
        lua.lua_close(state)


if __name__ == '__main__':
    main()
