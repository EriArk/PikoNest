pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
function _init()
  score=0
  
end
function _update()
  if btnp(4) then
    score+=1
    
  end
  if btnp(5) then
    _init()
    
  end
  
end
-- a blank canvas / pikoos
function _draw()
 cls(1)
  print(score,16,60,7)
  circfill(64,64,score,14)
  print(time(),16,80,7)
  if score>=5 then
    print("win!",16,16,14)
    
  end
  
end
