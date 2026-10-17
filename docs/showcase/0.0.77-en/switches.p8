pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
-- a blank canvas / pikoos
function _init()
 game_bg=5
 game_goal=2
 game_count=3
 sprite1_sx=8
 sprite1_sy=0
 sprite1_w=16
 sprite1_h=16
 sprite1_x=20
 sprite1_y=48
 sprite1_movement=0
 sprite1_speed=1
 sprite1_rule=4
 sprite1_target=1
 sprite1_on=0
 sprite1_neighbor=1
 sprite1_visible=1
 sprite2_sx=8
 sprite2_sy=0
 sprite2_w=16
 sprite2_h=16
 sprite2_x=56
 sprite2_y=48
 sprite2_movement=0
 sprite2_speed=1
 sprite2_rule=4
 sprite2_target=1
 sprite2_on=0
 sprite2_neighbor=-1
 sprite2_visible=1
 sprite3_sx=8
 sprite3_sy=0
 sprite3_w=16
 sprite3_h=16
 sprite3_x=92
 sprite3_y=48
 sprite3_movement=0
 sprite3_speed=1
 sprite3_rule=4
 sprite3_target=1
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
 if pressed0 then game_selected=(game_selected+2)%3 end
 if pressed1 then game_selected=(game_selected+1)%3 end
 if pressed4 and game_selected==0 then sprite1_on=1-sprite1_on sprite2_on=1-sprite2_on end
 if pressed4 and game_selected==1 then sprite2_on=1-sprite2_on end
 if pressed4 and game_selected==2 then sprite3_on=1-sprite3_on end
 if game_result==0 and sprite1_on==1 and sprite2_on==1 and sprite3_on==1 then game_result=1 end
end

function _draw()
 cls(game_bg)
 if sprite1_visible==1 then
  sspr(8,0,16,16,sprite1_x,sprite1_y)
  rect(sprite1_x,sprite1_y,sprite1_x+15,sprite1_y+15,sprite1_on==1 and 11 or 5)
  if game_selected==0 then rect(sprite1_x-2,sprite1_y-2,sprite1_x+17,sprite1_y+17,10) end
 end
 if sprite2_visible==1 then
  sspr(8,0,16,16,sprite2_x,sprite2_y)
  rect(sprite2_x,sprite2_y,sprite2_x+15,sprite2_y+15,sprite2_on==1 and 11 or 5)
  if game_selected==1 then rect(sprite2_x-2,sprite2_y-2,sprite2_x+17,sprite2_y+17,10) end
 end
 if sprite3_visible==1 then
  sspr(8,0,16,16,sprite3_x,sprite3_y)
  rect(sprite3_x,sprite3_y,sprite3_x+15,sprite3_y+15,sprite3_on==1 and 11 or 5)
  if game_selected==2 then rect(sprite3_x-2,sprite3_y-2,sprite3_x+17,sprite3_y+17,10) end
 end
 print("left/right: select  o: toggle",2,118,7)
 if game_result~=0 then
  rectfill(14,48,113,76,1)
  rect(14,48,113,76,7)
  print(game_result==1 and "you win!" or "try again",44,54,10)
  print("x: restart",44,66,7)
 end
end
__gfx__
00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000000cccccccccc00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000cccccccccccc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000cccccccccccc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000ccc7cccc7ccc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000ccc7cccc7ccc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000cccccccccccc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000cceccccceccc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000cccc777ccccc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
0000000000cccccccccccc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000000cccccccccc00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
