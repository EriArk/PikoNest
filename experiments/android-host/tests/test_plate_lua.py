"""Execute generated pressure-plate Lua in desktop Lua 5.4 with a PICO-8 API shim.

This checks the emitted program, not official PICO-8 execution, graphics, timing,
fixed-point limits or device input. Integer collision values stay in PICO-8 range.
Run PlateWorkflowTest first, then supply one of its generated .p8 paths.
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
function mset(x,y,v) cells[y*128+x]=v end
function sfx(n,c) sounds[#sounds+1]=n end function music(n,...) music_events[#music_events+1]=n end
function cls(...) end function camera(...) end function map(...) end
function sspr(...) end function rect(...) end function rectfill(...) end
function print(...) end
function tick(key,n)
 keys={} if key then keys[key]=true end
 for i=1,n or 1 do _update() end
end
'''
    assertions = """
_init()
local start_x=sprite1_x local start_plate_x=sprite2_x
local initial_on=sprite2_on local wall=passage1_tile
check(mget(4,1)==(initial_on==1 and 0 or wall),"initial overlap sets passage")
check(mget(4,0)==wall,"other wall cell preserved")
sprite1_x=8 sprite2_x=24 keys={} _update()
tick(1,2) check(sprite1_x==16 and sprite2_on==0,"edge contact not overlap")
local sound_count=#sounds
tick(1) check(sprite1_x==20 and sprite2_on==1 and mget(4,1)==0,"step onto plate opens passage")
if sprite2_sfx then check(#sounds==sound_count+1 and sounds[#sounds]==sprite2_sfx,"press sound once") end
if game_goal==2 then
 check(game_result==1,"plate counts toward all-on goal")
else
 tick(nil,3) check(sprite2_on==1,"plate remains held")
 if sprite2_sfx then check(#sounds==sound_count+1,"held plate does not repeat sound") end
 tick(4) check(sprite2_on==1,"O does not toggle a plate")
 tick(1,3) check(sprite1_x==32 and sprite2_on==0,"leaving plate switches it off")
 check(mget(4,1)==0,"occupied passage stays open")
 tick(1,2) check(sprite1_x==40 and mget(4,1)==wall,"passage closes after crossing")
 tick(0,4) check(sprite1_x==40,"closed return blocked")
 tick(1,8) check(game_result==1,"reach win contact")
end
tick(5) check(sprite1_x==start_x and sprite2_x==start_plate_x and sprite2_on==initial_on,"restart restores geometry and state")
check(mget(4,1)==(initial_on==1 and 0 or wall),"restart restores map")
-- Independent overlap checks exercise edges, both rectangles and invisible targets.
game_goal=0 game_result=0 sprite2_x=24 sprite2_y=8 sprite2_w=8 sprite2_h=8
for _,w in ipairs({1,8,16,32,64}) do
 for _,h in ipairs({1,8,16,32,64}) do
  sprite1_w=w sprite1_h=h
  for x=math.max(0,24-w-1),33 do
   for y=math.max(0,8-h-1),17 do
    sprite1_x=x sprite1_y=y game_result=0 keys={} _update()
    local overlap=x<32 and 24<x+w and y<16 and 8<y+h
    check((sprite2_on==1)==overlap,"plate rectangle overlap")
   end
  end
 end
end
sprite1_x=24 sprite1_y=8 sprite1_visible=0 game_result=0 keys={} _update()
check(sprite2_on==0,"invisible activator releases plate")
sprite1_visible=1 sprite2_visible=0 game_result=0 _update()
check(sprite2_on==0,"invisible plate remains off")
_draw()
"""
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
        code = (api + '\n'.join(cells) + '\n' + source + '\n' + assertions).encode()
        if lua.luaL_loadstring(state, code) or lua.lua_pcallk(state, 0, 0, 0, 0, None):
            raise AssertionError(lua.lua_tolstring(state, -1, None).decode())
        lua.lua_getglobal(state, b'checks')
        print(f'Generated plate Lua: {lua.lua_tointegerx(state, -1, None)} checks passed (desktop Lua, not official PICO-8)')
    finally:
        lua.lua_close(state)


if __name__ == '__main__':
    main()
