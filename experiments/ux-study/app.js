'use strict';
const palette=['#000000','#1d2b53','#7e2553','#008751','#ab5236','#5f574f','#c2c3c7','#fff1e8','#ff004d','#ffa300','#ffec27','#00e436','#29adff','#83769c','#ff77a8','#ffccaa'];
const initialSprite=[
'0000000000000000','0007000000700000','0007700007700000','0007777777700000',
'0077777777770000','0077177771770000','0077177771770000','0077778877770000',
'0007777777700000','0000888888000000','0007888888700000','0007788887700000',
'0000777777000000','0000770077000000','0007700077000000','0000000000000000'];
const fresh=()=>({screen:'workshop',tool:'workshop',title:'Лунный сад',speed:2,jump:3,edit:null,draft:null,modal:null,empty:false,history:[],sprite:initialSprite.map(r=>[...r].map(Number)),color:7,cursor:[7,7],eraser:false,heroX:46,heroY:85,toast:'',returnTool:null,codeScroll:0,focusByTool:{workshop:'edit:speed',code:'edit:speed',sprite:'canvas'}});
let state=fresh(), toastTimer, frame=0, lastGamepad=[];
const app=document.querySelector('#app');
const icons={workshop:'M1 3h2V1h4v2h2v6H1z M3 5h4v2H3z',code:'M3 1H1v6h2v2H0V0h3z M6 0h3v9H6V7h2V2H6z',sprite:'M1 1h2v2h3V1h2v7H1z M2 4h1v1H2z M6 4h1v1H6z'};
const icon=name=>`<span class="toolicon" aria-hidden="true"><svg viewBox="0 0 9 9"><path fill-rule="evenodd" d="${icons[name]}"></path></svg></span>`;
const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const button=(action,text,cls='',extra='')=>`<button data-action="${action}" class="${cls}" ${extra}>${text}</button>`;
function snapshot(){return {speed:state.speed,jump:state.jump,sprite:state.sprite.map(r=>r.slice())};}
function remember(){state.history.push(snapshot());if(state.history.length>40)state.history.shift();}
function message(text){state.toast=text;clearTimeout(toastTimer);toastTimer=setTimeout(()=>{state.toast='';const el=app.querySelector('.save');if(el)el.innerHTML='<i></i>сохранено';},1800);}
function setValue(field,value){const n=Math.max(1,Math.min(4,value));if(state[field]!==n){remember();state[field]=n;}}
function commit(){if(state.edit){setValue(state.edit,state.draft);state.edit=null;state.draft=null;}}
function footer(){
 if(state.screen==='run')return `<div class="footer"><span>← → двигаться</span><span><b class="key confirm">○</b>прыгать</span>${button('back','<b class="key">×</b>в мастерскую','right')}</div>`;
 if(state.edit)return `<div class="footer"><span>← → значение</span>${button('commit','<b class="key confirm">○</b>готово')}${button('back','<b class="key">×</b>отменить')}${button('test','<b class="key start">START</b>тест','right')}</div>`;
 if(state.tool==='sprite'&&state.screen!=='library')return `<div class="footer"><span>✛ пиксель</span><span><b class="key confirm">○</b>рисовать</span>${button('back','<b class="key">×</b>назад')}${button('test','<b class="key start">START</b>тест','right')}</div>`;
 return `<div class="footer"><span><b class="key confirm">○</b>${state.screen==='library'?'открыть':'изменить'}</span>${button('back','<b class="key">×</b>назад')}<span class="shoulders"><b class="key">L R</b>инструмент</span>${button('test','<b class="key start">START</b>тест','right')}</div>`;
}
function tabs(){return `<nav class="tabs" aria-label="Инструменты">${[['workshop','Мастерская'],['code','Код'],['sprite','Спрайты']].map(([id,title])=>button('tool:'+id,icon(id)+title,state.tool===id?'active':'',`aria-pressed="${state.tool===id}" ${state.edit?'disabled':''}`)).join('')}</nav>`;}
function shell(content){
 return `<header class="topbar"><span class="brand">PIKOOS</span><span class="crumb">маленькая мастерская</span><span class="save" role="status"><i></i>${state.edit?'правка':state.toast||'сохранено'}</span>${button('menu','≡','menu','aria-label="Меню проекта"')}</header>`+
 (state.screen==='library'?'':`<div class="titlebar">${button('library','‹','back','aria-label="К картриджам"')}<h2>${esc(state.title)}</h2><span class="micro">game.p8</span></div>${tabs()}`)+
 `<div class="content">${content}</div>${footer()}`;
}
function workshop(){
 return `<div class="work-grid"><div class="scene-wrap"><div class="preview-label"><p class="section-label">ТВОЯ ИГРА</p><span>128 × 128</span></div><div class="scene-frame"><canvas class="scene" width="128" height="128" data-scene="0" aria-label="Комната лунного сада с героем и платформами"></canvas></div><div class="scene-note"><span>Последний кадр</span><span class="pink">START · попробовать</span></div></div><section><p class="section-label">ГЕРОЙ / ДВИЖЕНИЕ</p>${['speed','jump'].map((id,i)=>button('edit:'+id,`<span>${i?'Прыжок':'Скорость'}</span><span class="pval"><span class="ghost">${i?'высота':'шаг'}</span><strong>${state.edit===id?state.draft:state[id]}</strong></span>`,'property '+((state.edit||'speed')===id?'selected':''),`aria-label="${i?'Прыжок':'Скорость'} ${state[id]}"`)).join('')}${state.edit?`<div class="editbar"><div class="stepper">${button('decrease','−','','aria-label="Уменьшить"')}<output>${state.draft}</output>${button('increase','+','','aria-label="Увеличить"')}</div><p class="hint">○ Готово &nbsp; × Отменить</p></div>`:`<p class="hint">Как быстро герой идёт<br>и как высоко прыгает.</p>`}${button('tool:code','{ } Посмотреть код','code-link')}${button('learn','? Как это работает','code-link')}</section></div>`;
}
function code(){const speed=state.edit==='speed'?state.draft:state.speed;const jump=state.edit==='jump'?state.draft:state.jump;const lines=[
 ['<span class="comment">-- движение героя</span>'],
 [`speed=${button('edit:speed',speed,'literal',`aria-label="Изменить speed, сейчас ${speed}"`)}`,'current'],
 [`jump=<span class="num">${jump}</span>`],[''],
 ['<span class="kw">function</span> <span class="func">_update</span>()'],
 ['  <span class="kw">if</span> <span class="func">btn</span>(<span class="num">0</span>) <span class="kw">then</span>'],
 ['    x-=speed'],['  <span class="kw">end</span>'],
 ['  <span class="kw">if</span> <span class="func">btn</span>(<span class="num">1</span>) <span class="kw">then</span>'],
 ['    x+=speed'],['  <span class="kw">end</span>'],['<span class="kw">end</span>']];
 return `<div class="code-layout"><div class="code-pane"><div class="code-header"><span class="current">01 движение</span><span>02 рисование</span><span>Lua</span></div>${lines.map(([l,c],i)=>`<div class="code-line ${c||''}"><span class="ln">${i+1}</span><span>${l||' '}</span></div>`).join('')}</div><div class="code-panel"><label>Скорость героя<small>speed · число от 1 до 4</small></label>${button('code-decrease','−','','aria-label="Уменьшить speed"')}<output>${speed}</output>${button('code-increase','+','','aria-label="Увеличить speed"')}${button('learn','?','','aria-label="Что такое переменная"')}</div></div>`;
}
function sprites(){return `<div class="sprite-layout"><div><div class="preview-label"><p class="section-label">ГЕРОЙ</p><span>№ 001 · 16 × 16</span></div><canvas class="sprite-canvas" width="256" height="256" tabindex="0" role="img" aria-label="Редактор спрайта героя. Стрелки перемещают курсор, Enter рисует пиксель."></canvas></div><section class="sprite-tools"><p class="section-label">ИНСТРУМЕНТ</p><div class="tool-row">${button('pencil','Кисть',state.eraser?'':'active')}${button('eraser','Ластик',state.eraser?'active':'')}${button('undo','↶','','aria-label="Отменить штрих"')}</div><p class="section-label">ПАЛИТРА</p><div class="palette" aria-label="Цвета PICO-8">${palette.map((p,i)=>button('color:'+i,'',i===state.color?'chosen':'',`style="background:${p}" aria-label="Цвет ${i}" aria-pressed="${i===state.color}"`)).join('')}</div><div class="sprite-mini"><canvas width="16" height="16" aria-label="Спрайт в исходном масштабе"></canvas><span>Один герой.<br>В редакторе и в игре.</span></div><p class="hint">Рисуй касанием<br>или по одному пикселю.</p></section></div>`;}
function library(){if(state.empty)return `<div class="empty"><span style="font-size:48px;color:var(--pink)">{ }</span><h2>Первый картридж — твой</h2><p>Начни с маленькой игры.<br>Её можно сразу менять.</p>${button('new','+ Создать игру')}${button('import','Открыть .p8')}</div>`;
 return `<div class="library"><div class="library-heading"><h2>Мои картриджи</h2>${button('new','+ Новый','new-cart')}</div><div class="shelf">${[state.title,'Тихий лес','Звёздная почта'].map((name,i)=>button('open:'+i,`<canvas width="128" height="128" data-scene="${i}" aria-label="Обложка ${esc(name)}"></canvas><span class="cart-name">${esc(name)}</span><span class="cart-kind">${i===0?'Продолжить работу':i===1?'Мой проект':'Игра · сделать ремикс'}</span><span class="cart-slots"></span>`,'cart '+(i===0?'featured':''))).join('')}</div><p class="shelf-note">Твои игры и их исходники — рядом.</p></div>`;
}
function modal(){if(!state.modal)return '';let body='';
 if(state.modal==='learn')body=`<h3>Что делает speed</h3><code>speed=${state.speed}</code><p><span style="color:var(--yellow)">speed</span> — переменная. Она хранит число, которое задаёт скорость героя.</p><p>В строке <span style="color:var(--blue)">x+=speed</span> герой сдвигается вправо на это число.</p><p class="muted">Попробуй 1, потом 4. Что удобнее?</p><div class="actions">${button('close','Понятно')}${button('learn-code','К этой строке','secondary')}</div>`;
 if(state.modal==='menu')body=`<h3>${esc(state.title)}</h3><p class="muted">Изменения сохраняются по ходу работы.</p><div class="actions">${button('undo','↶ Отменить','',state.history.length?'':'disabled')}${button('close','Продолжить','secondary')}</div><div class="actions">${button('library','К картриджам','secondary')}</div>`;
 if(state.modal==='new')body=`<h3>С чего начнём?</h3><p>Герой, маленькая комната и движение. Всё уже можно попробовать.</p><div class="actions">${button('create','Маленькая игра')}${button('close','Назад','secondary')}</div><p class="muted">Имя можно дать позже — сейчас сделаем первый шаг.</p>`;
 if(state.modal==='remix')body=`<h3>Заглянем внутрь?</h3><p>Для изменений создадим твою копию. Исходный картридж останется на полке.</p><div class="actions">${button('remix','Сделать ремикс')}${button('close','Назад','secondary')}</div>`;
 if(state.modal==='import')body=`<h3>Открыть картридж</h3><p>В приложении здесь откроется системный выбор .p8. Импорт создаёт отдельную рабочую копию.</p><div class="actions">${button('close','Понятно')}</div><p class="muted">Этот переход обозначен в UX-макете.</p>`;
 if(state.modal==='error')body=`<h3>Не удалось открыть PICO-8</h3><p>Проект сохранён. Приложение для запуска не найдено.</p><p class="muted">Настрой PICO-8 и попробуй снова.</p><div class="actions">${button('close','В мастерскую')}${button('runtime-help','Настроить','secondary')}</div>`;
 if(state.modal==='runtime-help')body=`<h3>Твой PICO-8</h3><p>Для запуска нужен официальный PICO-8, который ты приобрёл. Мастерская поможет выбрать его файлы.</p><p class="muted">Настройка запуска — отдельный первый шаг. Здесь показан только переход.</p><div class="actions">${button('close','К проекту')}</div>`;
 return `<div class="overlay"><section class="dialog ${state.modal==='error'?'error':''}" role="dialog" aria-modal="true" aria-label="${state.modal==='learn'?'Как работает скорость':'Действие с проектом'}">${body}</section></div>`;
}
function render(focusAction){
 if(state.screen==='run')app.innerHTML=`<div class="run-top"><span>${esc(state.title)}</span>${button('back','× Вернуться')}</div><div class="content"><div class="run-stage"><canvas width="128" height="128" data-run aria-label="Условный тест игры. Можно двигать героя стрелками."></canvas></div></div>${footer()}`;
 else app.innerHTML=shell(state.screen==='library'?library():state.tool==='workshop'?workshop():state.tool==='code'?code():sprites());
 if(state.modal){app.insertAdjacentHTML('beforeend',modal());for(const e of app.children)if(!e.classList.contains('overlay'))e.inert=true;}
 drawAll();
 const codeScroller=app.querySelector('.code-pane');if(codeScroller){codeScroller.scrollTop=state.codeScroll;codeScroller.addEventListener('scroll',()=>{state.codeScroll=codeScroller.scrollTop;});}
 const names={library:['Полка','Продолжить проект, поиграть или сделать свою копию.'],workshop:['Мастерская','Герой и его поведение рядом с последним кадром игры.'],code:['Код','Та же скорость — как настоящая переменная Lua.'],sprite:['Спрайты','Один рисунок для редактора и сцены.'],run:['Тест · имитация','В продукте здесь работает официальный PICO-8. Возврат сохраняет инструмент и фокус.']};
 const copy=names[state.screen==='library'?'library':state.screen==='run'?'run':state.tool];document.querySelector('#screen-name').textContent=copy[0];document.querySelector('#screen-explanation').textContent=copy[1];
 const desired=focusAction||state.focusByTool[state.tool];
 const target=state.modal?app.querySelector('.dialog button'):desired==='canvas'?app.querySelector('.sprite-canvas'):app.querySelector(`[data-action="${desired}"]`);if(target)target.focus({preventScroll:true});
 const canvas=app.querySelector('.sprite-canvas');if(canvas)canvas.addEventListener('pointerdown',paintPointer);
}
function action(id){
 if(id==='commit'){commit();render('edit:speed');return;}
 if(id.startsWith('tool:')){commit();state.tool=id.slice(5);state.screen='workshop';state.modal=null;render(id);return;}
 if(id.startsWith('edit:')){commit();state.edit=id.slice(5);state.draft=state[state.edit];render('increase');return;}
 if(id==='increase'||id==='decrease'){state.draft=Math.max(1,Math.min(4,state.draft+(id==='increase'?1:-1)));render(id);return;}
 if(id==='code-increase'||id==='code-decrease'){if(!state.edit){state.edit='speed';state.draft=state.speed;}state.draft=Math.max(1,Math.min(4,state.draft+(id==='code-increase'?1:-1)));render(id);return;}
 if(id==='test'){commit();state.returnTool=state.tool;state.screen='run';state.modal=null;state.heroX=46;render();return;}
 if(id==='back'){if(state.modal){state.modal=null;render();}else if(state.screen==='run'){state.screen='workshop';state.tool=state.returnTool||'workshop';render();}else if(state.edit){const field=state.edit;state.edit=null;state.draft=null;render('edit:'+field);}else if(state.tool==='sprite'&&document.activeElement?.classList.contains('sprite-canvas')){render('pencil');}else if(state.tool!=='workshop'){state.tool='workshop';render('tool:workshop');}else{state.screen='library';render('open:0');}return;}
 if(id==='library'){commit();state.screen='library';state.modal=null;render('open:0');return;}
 if(id.startsWith('open:')){const i=Number(id.slice(5));if(i===2){state.modal='remix';render();return;}state.screen='workshop';state.tool='workshop';if(i===1)state.title='Тихий лес';render('edit:speed');return;}
 if(id==='new'||id==='import'||id==='learn'||id==='menu'){state.modal=id;render();return;}
 if(id==='close'){state.modal=null;render('edit:speed');return;}
 if(id==='learn-code'){state.modal=null;state.tool='code';render('edit:speed');return;}
 if(id==='create'||id==='remix'){state=fresh();state.title=id==='remix'?'Звёздная почта · моя':'Моя маленькая игра';render('edit:speed');return;}
 if(id==='undo'){const old=state.history.pop();if(old){Object.assign(state,old);state.edit=null;state.draft=null;state.modal=null;message('отменено');}render('undo');return;}
 if(id.startsWith('color:')){state.color=Number(id.slice(6));state.eraser=false;render(id);return;}
 if(id==='pencil'||id==='eraser'){state.eraser=id==='eraser';render(id);return;}
 if(id==='runtime-help'){state.modal=id;render();}
}
app.addEventListener('click',e=>{const b=e.target.closest('[data-action]');if(b&&!b.disabled)action(b.dataset.action);});
app.addEventListener('focusin',e=>{if(state.modal||state.screen!=='workshop'||!e.target.closest('.content'))return;const id=e.target.classList.contains('sprite-canvas')?'canvas':e.target.dataset.action;if(id)state.focusByTool[state.tool]=id;});
function paintPointer(e){const rect=e.currentTarget.getBoundingClientRect();state.cursor=[Math.max(0,Math.min(15,Math.floor((e.clientX-rect.left)/rect.width*16))),Math.max(0,Math.min(15,Math.floor((e.clientY-rect.top)/rect.height*16)))];paint();e.currentTarget.focus();}
function paint(){const [x,y]=state.cursor;const c=state.eraser?0:state.color;if(state.sprite[y][x]!==c){remember();state.sprite[y][x]=c;}drawAll();}
function focusables(){return [...app.querySelectorAll(state.modal?'.dialog button':'button,.sprite-canvas')].filter(e=>!e.disabled&&!e.closest('[inert]')&&e.getClientRects().length);}
function moveFocus(direction){const items=focusables(),current=document.activeElement;if(!items.includes(current)){items[0]?.focus();return;}const r=current.getBoundingClientRect(),cx=r.x+r.width/2,cy=r.y+r.height/2;const candidates=items.filter(e=>e!==current).map(e=>{const b=e.getBoundingClientRect(),dx=b.x+b.width/2-cx,dy=b.y+b.height/2-cy;const along=direction==='left'?-dx:direction==='right'?dx:direction==='up'?-dy:dy;const cross=direction==='left'||direction==='right'?Math.abs(dy):Math.abs(dx);return {e,score:along+cross*3,along};}).filter(c=>c.along>4).sort((a,b)=>a.score-b.score);candidates[0]?.e.focus();}
function semantic(a){
 if(a==='cancel'){action('back');return;}
 if(a==='test'&&!state.modal){action('test');return;}
 if(a==='undo'&&!state.modal){action('undo');return;}
 if(a==='context'){if(state.screen==='run')return;if(state.tool==='sprite'){const next=(state.color+1)%16;action('color:'+next);}else action('learn');return;}
 if(a==='next'||a==='previous'){if(state.modal||state.edit||state.screen==='run'||state.screen==='library')return;const list=['workshop','code','sprite'];action('tool:'+list[(list.indexOf(state.tool)+(a==='next'?1:2))%3]);return;}
 if(a==='confirm'){if(state.modal){document.activeElement?.click();return;}if(state.screen==='run'){state.heroY=73;drawAll();setTimeout(()=>{state.heroY=85;drawAll();},250);return;}if(state.edit){commit();render('edit:speed');return;}if(document.activeElement?.classList.contains('sprite-canvas')){paint();return;}document.activeElement?.click();return;}
 if(['left','right','up','down'].includes(a)){
  if(state.screen==='run'){if(a==='left'||a==='right')state.heroX=Math.max(5,Math.min(113,state.heroX+(a==='right'?1:-1)*state.speed*2));drawAll();return;}
  if(state.edit&&(a==='left'||a==='right')){action(a==='left'?'decrease':'increase');return;}
  if(document.activeElement?.classList.contains('sprite-canvas')&&!state.modal){let [x,y]=state.cursor;x=Math.max(0,Math.min(15,x+(a==='right'?1:a==='left'?-1:0)));y=Math.max(0,Math.min(15,y+(a==='down'?1:a==='up'?-1:0)));state.cursor=[x,y];drawAll();return;}
  moveFocus(a);
 }
}
document.addEventListener('keydown',e=>{if(e.target.closest('.study-controls,.study-help')&&e.key!=='Escape')return;const keys={ArrowLeft:'left',ArrowRight:'right',ArrowUp:'up',ArrowDown:'down',Escape:'cancel',Enter:'confirm',' ':'test',q:'previous',e:'next',z:'undo',x:'context'};if(e.key==='Tab'&&state.modal){e.preventDefault();const items=focusables();const i=items.indexOf(document.activeElement);items[(i+(e.shiftKey?items.length-1:1))%items.length]?.focus();return;}const a=keys[e.key];if(a){e.preventDefault();if(e.repeat&&!['left','right','up','down'].includes(a))return;semantic(a);}});
document.querySelectorAll('[data-size]').forEach(b=>b.addEventListener('click',()=>{document.querySelector('#device').className=b.dataset.size;document.querySelectorAll('[data-size]').forEach(x=>x.setAttribute('aria-pressed',String(x===b)));}));
document.querySelector('#reset-study').addEventListener('click',()=>{state=fresh();render('edit:speed');});
document.querySelector('#show-error').addEventListener('click',()=>{state.screen='workshop';state.modal='error';render();});
document.querySelector('#show-empty').addEventListener('click',()=>{state.empty=true;state.screen='library';render('new');});
function sprite(ctx,x,y,scale=1){for(let j=0;j<16;j++)for(let i=0;i<16;i++){const c=state.sprite[j][i];if(c){ctx.fillStyle=palette[c];ctx.fillRect(x+i*scale,y+j*scale,scale,scale);}}}
function scene(c,variant=0,running=false){const ctx=c.getContext('2d');ctx.imageSmoothingEnabled=false;ctx.fillStyle=palette[1];ctx.fillRect(0,0,128,128);const p=(x,y,w,h,col)=>{ctx.fillStyle=palette[col];ctx.fillRect(x,y,w,h);};
 for(let i=0;i<30;i++){let x=(i*37+13)%128,y=(i*17+3)%60;p(x,y,1,1,i%3?13:7);}for(let y=-8;y<=8;y++)for(let x=-8;x<=8;x++){if(x*x+y*y<60&&(x-4)*(x-4)+(y+3)*(y+3)>55)p(104+x,19+y,1,1,15);}
 for(let i=0;i<7;i++){let x=i*24-12,y=48+(i%3)*8;p(x,y,20,70,13);p(x+4,y-6,12,8,13);p(x+8,y+2,2,24,1);}p(0,88,128,40,variant===1?3:2);p(0,97,128,31,1);
 for(let i=0;i<9;i++){let x=i*16+3;p(x,91,8,3,3);p(x+3,88,2,5,11);p(x+1,86,6,2,variant===2?10:14);}
 const platform=(x,y,w)=>{p(x,y,w,3,3);p(x+2,y,w-4,1,11);p(x,y+3,w,6,2);for(let j=0;j<w;j+=8)p(x+j,y+4,3,2,4);};platform(0,101,128);platform(9,73,26);platform(57,62,26);platform(96,81,24);p(67,49,4,4,10);p(66,50,6,2,10);p(68,48,2,6,10);
 p(109,85,10,16,4);p(111,87,6,14,2);p(111,86,6,2,9);p(112,89,1,1,10);sprite(ctx,running?state.heroX:46,running?state.heroY:85);if(variant===1){p(22,45,8,7,11);p(18,50,16,8,3);p(24,56,4,17,4);}
}
function drawAll(){app.querySelectorAll('[data-scene]').forEach(c=>scene(c,Number(c.dataset.scene)));const run=app.querySelector('[data-run]');if(run)scene(run,0,true);const canvas=app.querySelector('.sprite-canvas');if(canvas){const ctx=canvas.getContext('2d');ctx.imageSmoothingEnabled=false;for(let y=0;y<16;y++)for(let x=0;x<16;x++){ctx.fillStyle=palette[state.sprite[y][x]||((x+y)%2?1:0)];ctx.fillRect(x*16,y*16,16,16);}ctx.strokeStyle=palette[13];ctx.lineWidth=1;for(let i=0;i<=16;i++){ctx.beginPath();ctx.moveTo(i*16+.5,0);ctx.lineTo(i*16+.5,256);ctx.stroke();ctx.beginPath();ctx.moveTo(0,i*16+.5);ctx.lineTo(256,i*16+.5);ctx.stroke();}ctx.strokeStyle=palette[10];ctx.lineWidth=3;ctx.strokeRect(state.cursor[0]*16+2,state.cursor[1]*16+2,12,12);}const mini=app.querySelector('.sprite-mini canvas');if(mini){const ctx=mini.getContext('2d');ctx.clearRect(0,0,16,16);sprite(ctx,0,0);}}
function pollGamepad(){const p=navigator.getGamepads?.()[0];if(p){const map={0:'confirm',1:'cancel',2:'context',4:'previous',5:'next',8:'undo',9:'test',12:'up',13:'down',14:'left',15:'right'};for(const [i,a]of Object.entries(map)){const pressed=p.buttons[i]?.pressed;if(pressed&&!lastGamepad[i])semantic(a);lastGamepad[i]=pressed;}}else lastGamepad=[];frame=requestAnimationFrame(pollGamepad);}
render('edit:speed');pollGamepad();
