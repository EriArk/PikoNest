pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
-- a blank canvas / pikoos
function _init()
 game_bg=1
 game_goal=1
 game_count=3
 sprite1_sx=8
 sprite1_sy=0
 sprite1_w=16
 sprite1_h=16
 sprite1_x=18
 sprite1_y=52
 sprite1_movement=1
 sprite1_speed=2
 sprite1_rule=0
 sprite1_target=1
 sprite1_on=0
 sprite1_neighbor=-1
 sprite1_visible=1
 sprite2_sx=24
 sprite2_sy=0
 sprite2_w=16
 sprite2_h=16
 sprite2_x=94
 sprite2_y=52
 sprite2_movement=0
 sprite2_speed=1
 sprite2_rule=1
 sprite2_target=0
 sprite2_on=0
 sprite2_neighbor=-1
 sprite2_visible=1
 sprite3_sx=40
 sprite3_sy=0
 sprite3_w=16
 sprite3_h=16
 sprite3_x=56
 sprite3_y=52
 sprite3_movement=0
 sprite3_speed=1
 sprite3_rule=3
 sprite3_target=0
 sprite3_on=0
 sprite3_neighbor=-1
 sprite3_visible=1
 game_score=0
 game_result=0
 game_selected=0
 game_last0=btn(0)
 game_last1=btn(1)
 game_last2=btn(2)
 game_last3=btn(3)
 game_last4=btn(4)
 game_last5=btn(5)
end

function _update()
 local pressed0=btn(0) and not game_last0
 game_last0=btn(0)
 local pressed1=btn(1) and not game_last1
 game_last1=btn(1)
 local pressed2=btn(2) and not game_last2
 game_last2=btn(2)
 local pressed3=btn(3) and not game_last3
 game_last3=btn(3)
 local pressed4=btn(4) and not game_last4
 game_last4=btn(4)
 local pressed5=btn(5) and not game_last5
 game_last5=btn(5)
 if pressed5 then
  _init()
  return
 end
 if game_result~=0 then return end
 if btn(0) then sprite1_x=mid(0,sprite1_x-sprite1_speed,112) end
 if btn(1) then sprite1_x=mid(0,sprite1_x+sprite1_speed,112) end
 if btn(2) then sprite1_y=mid(0,sprite1_y-sprite1_speed,112) end
 if btn(3) then sprite1_y=mid(0,sprite1_y+sprite1_speed,112) end
 if game_result==0 and sprite2_visible==1 and sprite1_visible==1 and sprite2_x<sprite1_x+16 and sprite1_x<sprite2_x+16 and sprite2_y<sprite1_y+16 and sprite1_y<sprite2_y+16 then
  sprite2_visible=0
  game_score+=1
 end
 if game_result==0 and sprite3_visible==1 and sprite1_visible==1 and sprite3_x<sprite1_x+16 and sprite1_x<sprite3_x+16 and sprite3_y<sprite1_y+16 and sprite1_y<sprite3_y+16 then
  game_result=2
 end
 if game_result==0 and game_score==1 then game_result=1 end
end

function _draw()
 cls(game_bg)
 if sprite1_visible==1 then
  sspr(8,0,16,16,sprite1_x,sprite1_y)
 end
 if sprite2_visible==1 then
  sspr(24,0,16,16,sprite2_x,sprite2_y)
 end
 if sprite3_visible==1 then
  sspr(40,0,16,16,sprite3_x,sprite3_y)
 end
 print("collected "..game_score.."/1",2,2,7)
 if game_result~=0 then
  rectfill(14,48,113,76,1)
  rect(14,48,113,76,7)
  print(game_result==1 and "you win!" or "try again",44,54,10)
  print("x: restart",44,66,7)
 end
end
__gfx__
00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000000000000000000000000aa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000000eee0000eee000000000aaaa0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000eeeee00eeeee0000000aaaaaa000000008000080000800000000000000000000000000000000000000000000000000000000000000000000000000
000000000eeeeeeeeeeeeeee0000aa77aaaa00000088800888008880000000000000000000000000000000000000000000000000000000000000000000000000
000000000eeeeeeeeeeeeee0000aa7777aaaa0000088800888008880000000000000000000000000000000000000000000000000000000000000000000000000
000000000ee7ee7ee7ee7ee000aa7777aaaaaa000888888888888880000000000000000000000000000000000000000000000000000000000000000000000000
0000000000eeeeeeeeeeee00000aa77aaaaaa0000888888888888880000000000000000000000000000000000000000000000000000000000000000000000000
00000000000eeeeeeeeee0000000aaaaaaaa00000888888888888880000000000000000000000000000000000000000000000000000000000000000000000000
000000000000eeeeeeee000000000aaaaaa000000888888888888880000000000000000000000000000000000000000000000000000000000000000000000000
0000000000000eeeeee00000000000aaaa0000000022222222222200000000000000000000000000000000000000000000000000000000000000000000000000
00000000000000eeee0000000000000aa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
000000000000000ee000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
