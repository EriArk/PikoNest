package art.pikoos.runtimelab;
import art.pikoos.lab.core.*;
import java.util.*;
final class ParameterPresetStore {
    private final LibraryRepository repo;
    ParameterPresetStore(LibraryRepository repo){this.repo=repo;}
    List<ParameterPreset> list()throws Exception{return repo.presets();}
    void create(ParameterPreset p)throws Exception{repo.create(p);}
    void update(ParameterPreset a,ParameterPreset b)throws Exception{repo.update(a,b);}
}
