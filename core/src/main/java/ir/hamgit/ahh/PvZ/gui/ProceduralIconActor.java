package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

 
final class ProceduralIconActor extends Actor {
    private final PvzAssetSystem art;
    private final String kind;
    private float elapsed;

    ProceduralIconActor(PvzAssetSystem art, String kind) {
        this.art = art;
        this.kind = kind;
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        elapsed += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (art == null) {
            return;
        }
        float bob = (float) Math.sin(elapsed * 2.7f) * getHeight() * .035f;
        art.drawProceduralIcon(batch, kind, getX() + getWidth() * .5f,
            getY() + getHeight() * .5f + bob, getWidth() * .90f, getHeight() * .90f, elapsed);
    }
}
