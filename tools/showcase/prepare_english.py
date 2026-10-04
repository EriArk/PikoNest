"""Prepare an isolated English capture build; never modify the production sources.

Run from the repo root: py -3 tools/showcase/prepare_english.py
Then run the printed PowerShell build command. This is a presentation translation
of the selected showcase surfaces, not the product's future localization system.
"""
from pathlib import Path
import shutil,re

repo=Path(__file__).resolve().parents[2]
stage=repo/'.local/showcase-en-65/build'
host=stage/'experiments/android-host'
host.mkdir(parents=True,exist_ok=True)
shutil.copytree(repo/'experiments/android-host',host,dirs_exist_ok=True)
shutil.copyfile(Path(__file__).with_name('labels-en.json'),host/'assets/showcase-en.json')
package=host/'src/art/pikoos/runtimelab'
for name in ['WorkshopView.java','PlayView.java','LibraryView.java']:
    p=package/name;s=p.read_text(encoding='utf-8')
    s=re.sub(r'(private (?:void|float) (?:text|fit|fitted|width|wrapped|button|key)\(String (\w+)[^\n{]*\)\s*\{)',lambda m:m[1]+m[2]+'=EnglishLabels.t('+m[2]+');',s)
    p.write_text(s,encoding='utf-8')
p=package/'PixelText.java';s=p.read_text(encoding='utf-8').replace('static void configure(Context context, Paint paint) {','static void configure(Context context, Paint paint) {\n        EnglishLabels.load(context);').replace('static int actionSize(Paint paint, String label, float available) {','static int actionSize(Paint paint, String label, float available) {\n        label=EnglishLabels.t(label);');p.write_text(s,encoding='utf-8')
(package/'EnglishLabels.java').write_text('''package art.pikoos.runtimelab;
import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.util.*;
import java.io.*;
/** Capture-build text only. Domain state, projects and controls are unchanged. */
final class EnglishLabels {
    private static JSONObject labels;
    private static final ArrayList<String> keys=new ArrayList<>();
    private static final HashMap<String,String> cache=new HashMap<>();
    static void load(Context context){
        if(labels!=null)return;
        try(InputStream in=context.getAssets().open("showcase-en.json")){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            labels=new JSONObject(new String(out.toByteArray(),"UTF-8"));
            Iterator<String> it=labels.keys();while(it.hasNext())keys.add(it.next());
            Collections.sort(keys,(a,b)->Integer.compare(b.length(),a.length()));
        }catch(Exception e){throw new IllegalStateException("English capture labels",e);}
    }
    static String t(String source){
        String found=cache.get(source);if(found!=null)return found;
        String value=source;for(String key:keys)value=value.replace(key,labels.optString(key));
        if(value.matches(".*[\\\\u0400-\\\\u04ff].*"))Log.i("ShowcaseEnglish",source+" -> "+value);
        cache.put(source,value);return value;
    }
}
'''.replace('\\\\u','\\u'),encoding='utf-8')
p=host/'build.ps1';s=p.read_text(encoding='utf-8-sig')
s=s.replace("$pikoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\\..')).Path","$pikoRoot = '"+str(repo).replace("'","''")+"'")
s=s.replace("$pikoApk = Join-Path $pikoArtifacts 'pikoos-runtime-lab.apk'","$pikoApk = '"+str(stage.parent/'pikonest-english-capture.apk').replace("'","''")+"'")
p.write_text(s,encoding='utf-8-sig')
print('powershell -NoProfile -ExecutionPolicy Bypass -File "'+str(p)+'"')
