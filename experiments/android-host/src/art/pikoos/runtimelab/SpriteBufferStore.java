package art.pikoos.runtimelab;
import art.pikoos.lab.core.*;
final class SpriteBufferStore {
    private final LibraryRepository repo;
    SpriteBufferStore(LibraryRepository repo){this.repo=repo;}
    SpriteAsset read()throws Exception{return repo.buffer();}
    void write(SpriteAsset a)throws Exception{repo.buffer(a);}
}
