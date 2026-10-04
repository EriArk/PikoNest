pico-8 cartridge // http://www.pico-8.com
version 43
__lua__
function _init()
  score=0
  phase=0
  target=8
  
end
function _update()
  if phase==0 then
    if btnp(0) then
      score+=1
      
    end
    if btnp(1) then
      score+=2
      
    end
    if score>=target then
      if score==target then
        phase=1
        
      else
        phase=2
        
      end
      
    end
    
  else
    if btnp(4) then
      _init()
      
    end
    
  end
  
end
-- a blank canvas / pikoos
function _draw()
 cls(1)
print("make the number",16,16,14)
print(target,16,28,7)
print("your score",16,48,12)
print(score,16,60,7)
if phase==0 then
  print("left +1  right +2",16,84,10)
  
  print(score,64,104,11)
else
  if phase==1 then
    print("exact!",16,84,11)
    
  else
    print("too far",16,84,8)
    
  end
  print("o: try again",16,104,6)
  
end
end
