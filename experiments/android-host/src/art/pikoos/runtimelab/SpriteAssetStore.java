package art.pikoos.runtimelab;
import art.pikoos.lab.core.*;
import java.util.*;
/** Shared typed library workflow; location belongs to its repository. */
final class SpriteAssetStore {
    private final LibraryRepository repo;
    SpriteAssetStore(LibraryRepository repo){this.repo=repo;}
    List<SpriteAsset> list()throws Exception{return repo.sprites();}
    void create(SpriteAsset a)throws Exception{repo.create(a);}
    void rename(SpriteAsset a,String title)throws Exception{repo.update(a,a.withTitle(title));}
    void categorize(SpriteAsset a,SpriteAsset.Category c)throws Exception{repo.update(a,a.withCategory(c));}
    void favorite(SpriteAsset a,boolean favorite)throws Exception{repo.update(a,a.withFavorite(favorite));}
}
