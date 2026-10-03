package art.pikoos.lab.core;

/** Pixel permutations within an unchanged selection footprint. */
public enum SpriteTransform {
    FLIP_HORIZONTAL, FLIP_VERTICAL, ROTATE_CLOCKWISE, ROTATE_HALF;

    public boolean supports(SpriteRegion region) {
        return this != ROTATE_CLOCKWISE || region.width == region.height;
    }
}
