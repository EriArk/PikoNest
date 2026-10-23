pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
inputs={}
function btn(n) return inputs[n] or false end
function _init()
 game_bg=1
 game_goal=0
 game_count=3
 sprite1_sx=8
 sprite1_sy=0
 sprite1_w=8
 sprite1_h=8
 sprite1_x=20
 sprite1_y=40
 sprite1_movement=1
 sprite1_speed=1
 sprite1_rule=0
 sprite1_target=1
 sprite1_on=0
 sprite1_neighbor=-1
 sprite1_visible=1
 sprite1_animation=1
 sprite1_frames=2
 sprite1_f0_sx=8
 sprite1_f0_sy=0
 sprite1_f0_w=8
 sprite1_f0_h=8
 sprite1_f0_ticks=2
 sprite1_f1_sx=16
 sprite1_f1_sy=0
 sprite1_f1_w=8
 sprite1_f1_h=8
 sprite1_f1_ticks=3
 sprite1_clock=0
 sprite1_active=0
 sprite2_sx=8
 sprite2_sy=0
 sprite2_w=8
 sprite2_h=8
 sprite2_x=60
 sprite2_y=40
 sprite2_movement=0
 sprite2_speed=1
 sprite2_rule=4
 sprite2_target=0
 sprite2_on=0
 sprite2_neighbor=2
 sprite2_visible=1
 sprite2_animation=2
 sprite2_frames=2
 sprite2_f0_sx=8
 sprite2_f0_sy=0
 sprite2_f0_w=8
 sprite2_f0_h=8
 sprite2_f0_ticks=2
 sprite2_f1_sx=16
 sprite2_f1_sy=0
 sprite2_f1_w=8
 sprite2_f1_h=8
 sprite2_f1_ticks=3
 sprite2_clock=0
 sprite2_active=0
 sprite3_sx=8
 sprite3_sy=0
 sprite3_w=8
 sprite3_h=8
 sprite3_x=90
 sprite3_y=40
 sprite3_movement=0
 sprite3_speed=1
 sprite3_rule=4
 sprite3_target=0
 sprite3_on=0
 sprite3_neighbor=-1
 sprite3_visible=1
 sprite3_animation=2
 sprite3_frames=2
 sprite3_f0_sx=8
 sprite3_f0_sy=0
 sprite3_f0_w=8
 sprite3_f0_h=8
 sprite3_f0_ticks=2
 sprite3_f1_sx=16
 sprite3_f1_sy=0
 sprite3_f1_w=8
 sprite3_f1_h=8
 sprite3_f1_ticks=3
 sprite3_clock=0
 sprite3_active=0
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
 if pressed0 then game_selected=(game_selected+1)%2 end
 if pressed1 then game_selected=(game_selected+1)%2 end
 local sprite1_oldx=sprite1_x
 local sprite1_oldy=sprite1_y
 if btn(0) then sprite1_x=mid(0,sprite1_x-sprite1_speed,120) end
 if btn(1) then sprite1_x=mid(0,sprite1_x+sprite1_speed,120) end
 if btn(2) then sprite1_y=mid(0,sprite1_y-sprite1_speed,120) end
 if btn(3) then sprite1_y=mid(0,sprite1_y+sprite1_speed,120) end
 if pressed4 and game_selected==0 then sprite2_on=1-sprite2_on sprite3_on=1-sprite3_on end
 if pressed4 and game_selected==1 then sprite3_on=1-sprite3_on end
 if sprite1_x~=sprite1_oldx or sprite1_y~=sprite1_oldy then
  if sprite1_active==1 then sprite1_clock=(sprite1_clock+1)%5 end
  sprite1_active=1
 else
  sprite1_clock=0
  sprite1_active=0
 end
 if sprite2_on==1 then
  if sprite2_active==1 then sprite2_clock=(sprite2_clock+1)%5 end
  sprite2_active=1
 else
  sprite2_clock=0
  sprite2_active=0
 end
 if sprite3_on==1 then
  if sprite3_active==1 then sprite3_clock=(sprite3_clock+1)%5 end
  sprite3_active=1
 else
  sprite3_clock=0
  sprite3_active=0
 end
end

function _draw()
 cls(game_bg)
 if sprite1_visible==1 then
  if sprite1_clock<2 then
   sspr(8,0,8,8,sprite1_x,sprite1_y)
  else
   sspr(16,0,8,8,sprite1_x,sprite1_y)
  end
 end
 if sprite2_visible==1 then
  if sprite2_clock<2 then
   sspr(8,0,8,8,sprite2_x,sprite2_y)
  else
   sspr(16,0,8,8,sprite2_x,sprite2_y)
  end
  rect(sprite2_x,sprite2_y,sprite2_x+7,sprite2_y+7,sprite2_on==1 and 11 or 5)
  if game_selected==0 then rect(sprite2_x-2,sprite2_y-2,sprite2_x+9,sprite2_y+9,10) end
 end
 if sprite3_visible==1 then
  if sprite3_clock<2 then
   sspr(8,0,8,8,sprite3_x,sprite3_y)
  else
   sspr(16,0,8,8,sprite3_x,sprite3_y)
  end
  rect(sprite3_x,sprite3_y,sprite3_x+7,sprite3_y+7,sprite3_on==1 and 11 or 5)
  if game_selected==1 then rect(sprite3_x-2,sprite3_y-2,sprite3_x+9,sprite3_y+9,10) end
 end
 print("left/right: select  o: toggle",2,118,7)
 if game_result~=0 then
  rectfill(14,48,113,76,1)
  rect(14,48,113,76,7)
  print(game_result==1 and "you win!" or "try again",44,54,10)
  print("x: restart",44,66,7)
 end
end

original_init=_init
original_draw=_draw
checks=0
function check(v,msg)
 checks+=1
 if not v then printh("failed: "..msg) stop("failed: "..msg) end
end
function _init()
 original_init()
 check(sprite1_clock==0,"initial frame")
 inputs[1]=true
 _update()
 check(sprite1_x==21 and sprite1_clock==0,"move enters frame one")
 _update()
 check(sprite1_clock==1,"duration before boundary")
 _draw()
 check(pget(sprite1_x,sprite1_y)==14,"first frame pixels")
 _update()
 _draw()
 check(sprite1_clock==2 and pget(sprite1_x,sprite1_y)==10,"next frame pixels")
 sprite1_x=120
 _update()
 check(sprite1_clock==0 and sprite1_active==0,"edge returns to still")
 inputs={}
 _update()
 inputs[4]=true
 _update()
 check(sprite2_on==1 and sprite3_on==1,"linked toggle")
 check(sprite2_clock==0 and sprite3_clock==0,"linked state entry")
 _update()
 check(sprite2_on==1 and sprite2_clock==1,"hold does not retoggle")
 inputs={}
 expected=1
 for batch=1,20 do
  for tick=1,2000 do
   expected=(expected+1)%5
   _update()
   if sprite2_clock~=expected then stop("clock overflow") end
  end
 end
 check(sprite2_clock==expected,"forty thousand updates")
 inputs[4]=true
 _update()
 check(sprite2_on==0 and sprite3_on==0,"toggle off")
 check(sprite2_clock==0 and sprite3_clock==0,"off frame reset")
 inputs={}
 _update()
 inputs[5]=true
 _update()
 check(sprite1_x==20 and sprite1_clock==0 and sprite2_on==0,"restart state")
 _update=function() end
 printh("animation checks passed: "..checks)
end
function _draw()
 original_draw()
 rectfill(0,0,127,28,1)
 print("animation checks passed",8,6,11)
 print("40000 updates / 13 checks",8,16,7)
end
__gfx__
00000000eeeeeeeeaaaaaaaa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000eeeeeeeeaaaaaaaa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000eeeeeeeeaaaaaaaa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000eeeeeeeeaaaaaaaa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000eeeeeeeeaaaaaaaa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000eeeeeeeeaaaaaaaa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000eeeeeeeeaaaaaaaa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
00000000eeeeeeeeaaaaaaaa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
