"""Execute generated switch-passage Lua in desktop Lua 5.4 with a PICO-8 API shim.

This checks the emitted program, not official PICO-8 execution, graphics, timing,
fixed-point limits or device input. Integer collision values stay in PICO-8 range.
Run PassageWorkflowTest first, then supply one of its generated .p8 paths.
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
    assertions = r"""
_init()
local wall=passage1_tile
local initial=sprite2_on
local opened=initial==passage1_on
check(mget(4,1)==(opened and 0 or wall),"initial switch state applied")
check(mget(4,2)==(opened and 0 or wall),"multiple cells share switch")
check(mget(4,0)==wall and mget(4,3)==wall,"other uses of same tile stay walls")
check(mget(100,31)==200,"outside map data preserved")
-- Close from any authored initial state; place mover safely before toggling.
sprite1_x=8 sprite1_y=8
if opened then tick(4) tick(nil) end
tick(1,10) check(sprite1_x==24,"closed passage blocks at full rectangle edge")
tick(4) check(mget(4,1)==0 and mget(4,2)==0,"switch opens immediately")
tick(4) check(mget(4,1)==0,"held O does not toggle again")
tick(1,3) check(sprite1_x==36,"mover enters open passage")
tick(4) check(mget(4,1)==0 and mget(4,2)==wall,"occupied cell waits; empty linked cell closes")
check(sprite2_on~=passage1_on,"switch really changed while close waits")
tick(1) check(sprite1_x==40 and mget(4,1)==wall,"passage closes once mover leaves")
tick(0,10) check(sprite1_x==40,"closed wall blocks return")
tick(1,8) check(game_result==1,"crossing passage reaches win contact")
tick(5) check(game_result==0 and sprite2_on==initial,"restart restores switch and result")
check(mget(4,1)==(opened and 0 or wall),"restart restores original passage state")
check(mget(4,0)==wall,"restart preserves unrelated wall")
_draw()
-- Closure guard uses every moving element's full rectangle, including non-corner overlaps.
keys={} sprite1_visible=1 sprite2_on=1-passage1_on
for _,w in ipairs({1,8,16,24,64,128}) do
 for _,h in ipairs({1,8,16,24,64,128}) do
  sprite1_w=w sprite1_h=h
  for x=32-w-1,41 do
   for y=8-h-1,17 do
    sprite1_x=x sprite1_y=y
    cells[1*128+4]=0
    world_passages(false)
    local overlap=x<40 and 32<x+w and y<16 and 8<y+h
    check((mget(4,1)==0)==overlap,"rectangle-safe close")
   end
  end
 end
end
_init() sprite1_visible=0 sprite1_x=32 sprite1_y=8 sprite2_on=1-passage1_on
cells[1*128+4]=0 world_passages(false) check(mget(4,1)==wall,"invisible mover does not hold passage")
if game_count>3 then
 sprite1_visible=0 sprite4_visible=1 sprite4_x=32 sprite4_y=8
 cells[1*128+4]=0 world_passages(false) check(mget(4,1)==0,"second mover holds passage")
 sprite4_x=40 world_passages(false) check(mget(4,1)==wall,"second mover clears passage")
end
-- Reset restores closed state even if stale runtime coordinates overlap.
world_passages(true) check(mget(4,1)==wall,"reset is authoritative")
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
        code = (api + '\n'.join(cells) + '\n' + source + assertions).encode()
        if lua.luaL_loadstring(state, code) or lua.lua_pcallk(state, 0, 0, 0, 0, None):
            raise AssertionError(lua.lua_tolstring(state, -1, None).decode())
        lua.lua_getglobal(state, b'checks')
        print(f'Generated passage Lua: {lua.lua_tointegerx(state, -1, None)} checks passed (desktop Lua, not official PICO-8)')
    finally:
        lua.lua_close(state)


if __name__ == '__main__':
    main()
