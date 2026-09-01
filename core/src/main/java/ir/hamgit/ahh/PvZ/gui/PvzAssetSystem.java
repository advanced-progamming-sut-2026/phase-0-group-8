package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.io.File;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

 






public final class PvzAssetSystem implements Disposable {
    private static final String ROOT = "pvz";
    private static final String DEFAULT_RESOLUTION = "768";

    private final FileHandle root;
    private final FileHandle imagesRoot;
    private final FileHandle atlasesRoot;
    private final Map<PlantType, AnimationSpec> plantAnimations = new EnumMap<>(PlantType.class);
    private final Map<ZombieType, AnimationSpec> zombieAnimations = new EnumMap<>(ZombieType.class);
    private final Map<ChapterType, String> backgrounds = new EnumMap<>(ChapterType.class);
    private final Map<ChapterType, String> mowers = new EnumMap<>(ChapterType.class);
    private final Map<ChapterType, String> graves = new EnumMap<>(ChapterType.class);
    private final Map<String, String> hud = new HashMap<>();
    private final Map<String, TextureRegion> regionCache = new HashMap<>();
    private final Texture fallbackCircle;
    private final Texture fallbackPixel;

    
    
    
    
    private final Matrix4 savedBatchTransform = new Matrix4();
    private final Matrix4 scaledBatchTransform = new Matrix4();

    private String defaultBackground;
    private String defaultMower;
    private String defaultGrave;
    private String resolution = DEFAULT_RESOLUTION;
    private TextureBank textures;
    private PamPlayer player;
    private String initializationMessage = "Not initialized";
    private int atlasPngCount;
    private int mappedPamPresent;
    private int mappedPamTotal;

    public PvzAssetSystem() {
        fallbackCircle = makeFallbackCircle();
        fallbackPixel = makeFallbackPixel();
        root = resolveAssetRoot();
        imagesRoot = root.child("IMAGES");
        atlasesRoot = root.child("ATLASES");
        loadMappings();
        inspectAndInitialize();
    }

    private Texture makeFallbackCircle() {
        Pixmap map = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        map.setColor(0f, 0f, 0f, 0f);
        map.fill();
        map.setColor(Color.WHITE);
        map.fillCircle(32, 32, 30);
        Texture result = new Texture(map);
        result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        map.dispose();
        return result;
    }

    private Texture makeFallbackPixel() {
        Pixmap map = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        map.setColor(Color.WHITE);
        map.fill();
        Texture result = new Texture(map);
        map.dispose();
        return result;
    }

    private FileHandle resolveAssetRoot() {
        
        
        
        FileHandle projectAssets = Gdx.files.absolute(new File("assets/" + ROOT).getAbsolutePath());
        if (hasIndex(projectAssets)) return projectAssets;

        FileHandle workingAssets = Gdx.files.absolute(new File(ROOT).getAbsolutePath());
        if (hasIndex(workingAssets)) return workingAssets;

        
        
        
        return Gdx.files.internal(ROOT);
    }

    private boolean hasIndex(FileHandle candidate) {
        if (candidate == null) return false;
        return candidate.child("RESOURCES.json").exists() || candidate.child("resources.json").exists();
    }

    private void loadMappings() {
        ObjectMapper mapper = new ObjectMapper();
        FileHandle animationMap = root.child("pvz-animation-map.json");
        if (animationMap.exists()) {
            try (InputStream in = animationMap.read()) {
                JsonNode document = mapper.readTree(in);
                resolution = document.path("resolution").asText(DEFAULT_RESOLUTION);
                readAnimationGroup(document.path("plants"), true);
                readAnimationGroup(document.path("zombies"), false);
            } catch (Exception e) {
                initializationMessage = "Animation map could not be read: " + e.getMessage();
            }
        }

        FileHandle resourceMap = root.child("pvz-resource-map.json");
        if (resourceMap.exists()) {
            try (InputStream in = resourceMap.read()) {
                JsonNode document = mapper.readTree(in);
                defaultBackground = readChapterMap(document.path("backgrounds"), backgrounds);
                defaultMower = readChapterMap(document.path("mowers"), mowers);
                defaultGrave = readChapterMap(document.path("graves"), graves);
                JsonNode hudNode = document.path("hud");
                if (hudNode.isObject()) {
                    hudNode.fields().forEachRemaining(e -> hud.put(e.getKey(), e.getValue().asText()));
                }
            } catch (Exception e) {
                initializationMessage = "Resource map could not be read: " + e.getMessage();
            }
        }
    }

    private void readAnimationGroup(JsonNode group, boolean plants) {
        if (!group.isObject()) return;
        group.fields().forEachRemaining(entry -> {
            if (entry.getValue() == null || !entry.getValue().isObject()) return;
            try {
                AnimationSpec spec = AnimationSpec.from(entry.getValue());
                if (plants) {
                    plantAnimations.put(PlantType.valueOf(entry.getKey()), spec);
                } else {
                    zombieAnimations.put(ZombieType.valueOf(entry.getKey()), spec);
                }
            } catch (IllegalArgumentException ignored) {
                
                
            }
        });
    }

    private String readChapterMap(JsonNode node, Map<ChapterType, String> destination) {
        if (!node.isObject()) return null;
        final String[] fallback = {null};
        node.fields().forEachRemaining(entry -> {
            if ("DEFAULT".equals(entry.getKey())) {
                fallback[0] = entry.getValue().asText();
                return;
            }
            try {
                destination.put(ChapterType.valueOf(entry.getKey()), entry.getValue().asText());
            } catch (IllegalArgumentException ignored) {
                
            }
        });
        return fallback[0];
    }

    private void inspectAndInitialize() {
        atlasPngCount = countDirectPngs(atlasesRoot);
        mappedPamTotal = plantAnimations.size() + zombieAnimations.size();
        mappedPamPresent = 0;
        for (AnimationSpec spec : plantAnimations.values()) if (pamExists(spec)) mappedPamPresent++;
        for (AnimationSpec spec : zombieAnimations.values()) if (pamExists(spec)) mappedPamPresent++;

        if (!root.child("RESOURCES.json").exists() && !root.child("resources.json").exists()) {
            initializationMessage = "RESOURCES.json is missing from assets/pvz.";
            return;
        }
        if (!atlasesRoot.exists() || !atlasesRoot.isDirectory() || atlasPngCount == 0) {
            initializationMessage = "ATLASES is empty. Copy the extracted atlas PNG files into assets/pvz/ATLASES/.";
            return;
        }
        if (!imagesRoot.exists() || !imagesRoot.isDirectory() || mappedPamPresent == 0) {
            initializationMessage = "IMAGES is empty. Copy the extracted IMAGES tree (including 768/.../*.PAM) into assets/pvz/IMAGES/.";
            return;
        }

        try {
            textures = new TextureBank(resolution, root);
            player = new PamPlayer(textures, root);
            initializationMessage = "libPVZ active";
        } catch (RuntimeException e) {
            textures = null;
            player = null;
            initializationMessage = "libPVZ initialization failed: " + safeMessage(e);
        }
    }

    public void update() {
        if (textures != null) {
            try {
                textures.update();
            } catch (RuntimeException ignored) {
                
                
            }
        }
    }

    public boolean isLibPvzActive() {
        return textures != null && player != null;
    }

    public boolean drawPlant(Batch batch, PlantType type, String action, float stateTime,
                             float centerX, float centerY, float maxWidth, float maxHeight) {
        return drawAnimation(batch, plantAnimations.get(type), action, stateTime,
            centerX, centerY, maxWidth, maxHeight);
    }

    public boolean drawZombie(Batch batch, ZombieType type, String action, float stateTime,
                              float centerX, float centerY, float maxWidth, float maxHeight) {
        return drawAnimation(batch, zombieAnimations.get(type), action, stateTime,
            centerX, centerY, maxWidth, maxHeight);
    }

    private boolean drawAnimation(Batch batch, AnimationSpec spec, String action, float stateTime,
                                  float centerX, float centerY, float maxWidth, float maxHeight) {
        if (player == null || spec == null || !pamExists(spec)) return false;
        String clipName = spec.clip(action);
        if (clipName == null || clipName.isBlank()) return false;
        try {
            ClipRef clip = player.getClip(spec.pam, clipName);
            if (clip == null) return false; 
            float canvasWidth = Math.max(1f, spec.canvasWidth);
            float canvasHeight = Math.max(1f, spec.canvasHeight);
            float scale = Math.min(maxWidth / canvasWidth, maxHeight / canvasHeight);
            if (!Float.isFinite(scale) || scale <= 0f) return false;
            drawScaledClip(batch, clip, stateTime, centerX, centerY, scale);
            batch.setColor(Color.WHITE);
            return true;
        } catch (RuntimeException ignored) {
            
            
            batch.setColor(Color.WHITE);
            return false;
        }
    }

     








    private void drawScaledClip(Batch batch, ClipRef clip, float stateTime,
                                float centerX, float centerY, float scale) {
        savedBatchTransform.set(batch.getTransformMatrix());
        scaledBatchTransform.set(savedBatchTransform)
            .translate(centerX, centerY, 0f)
            .scale(scale, scale, 1f)
            .translate(-centerX, -centerY, 0f);

        batch.setTransformMatrix(scaledBatchTransform);
        try {
            player.draw(batch, clip, stateTime, centerX, centerY, true);
        } finally {
            batch.setTransformMatrix(savedBatchTransform);
        }
    }

    public boolean drawBackground(Batch batch, ChapterType chapter,
                                  float x, float y, float width, float height) {
        String id = backgrounds.getOrDefault(chapter, defaultBackground);
        TextureRegion region = region(id);
        if (region == null) return false;
        drawAspectFill(batch, region, x, y, width, height, Color.WHITE);
        return true;
    }

     
    public boolean drawMenuBackground(Batch batch, float x, float y, float width, float height) {
        TextureRegion region = region(defaultBackground);
        if (region == null) return false;
        drawAspectFill(batch, region, x, y, width, height, Color.WHITE);
        return true;
    }

     
    public void drawFallbackCircle(Batch batch, float centerX, float centerY, float size, Color tint) {
        batch.setColor(tint == null ? Color.WHITE : tint);
        batch.draw(fallbackCircle, centerX - size * .5f, centerY - size * .5f, size, size);
        batch.setColor(Color.WHITE);
    }

     
    public void drawProceduralPlant(Batch batch, PlantType type, String action, float time,
                                    float centerX, float centerY, float maxWidth, float maxHeight,
                                    Color tint) {
        float u = Math.max(1f, Math.min(maxWidth, maxHeight) / 100f);
        String name = type == null ? "PLANT" : type.name();
        boolean food = "plantFood".equals(action);
        boolean attacking = "attack".equals(action);
        boolean producing = "produce".equals(action);
        float pulse = 1f + (float) Math.sin(time * (food ? 10f : 3f)) * (food ? .10f : .025f);
        float lean = attacking ? (float) Math.sin(time * 16f) * 5f * u : 0f;
        Color base = tint == null ? new Color(.30f, .75f, .28f, 1f) : tint;

        if (food) {
            circle(batch, centerX, centerY + 3f * u, 88f * u * pulse,
                new Color(1f, .82f, .12f, .34f));
            for (int i = 0; i < 8; i++) {
                double angle = time * 3.2 + i * Math.PI / 4.0;
                circle(batch, centerX + (float) Math.cos(angle) * 43f * u,
                    centerY + (float) Math.sin(angle) * 43f * u, 8f * u,
                    new Color(1f, .94f, .32f, .86f));
            }
        }

        rect(batch, centerX - 4f * u, centerY - 37f * u, 8f * u, 45f * u,
            new Color(.18f, .48f, .14f, 1f));
        circle(batch, centerX - 14f * u, centerY - 23f * u, 29f * u, new Color(.20f, .62f, .20f, 1f));
        circle(batch, centerX + 14f * u, centerY - 18f * u, 28f * u, new Color(.25f, .70f, .25f, 1f));

        if (isSunPlant(name)) {
            for (int i = 0; i < 10; i++) {
                double a = i * TWO_PI / 10.0 + time * .08;
                circle(batch, centerX + (float) Math.cos(a) * 27f * u,
                    centerY + 12f * u + (float) Math.sin(a) * 27f * u,
                    24f * u, new Color(1f, .73f, .08f, 1f));
            }
            circle(batch, centerX, centerY + 12f * u, 49f * u * pulse,
                producing ? new Color(1f, .96f, .30f, 1f) : new Color(.55f, .31f, .10f, 1f));
            face(batch, centerX, centerY + 12f * u, u, Color.BLACK);
        } else if (isMushroom(name)) {
            rect(batch, centerX - 12f * u, centerY - 9f * u, 24f * u, 32f * u,
                new Color(.90f, .82f, .68f, 1f));
            circle(batch, centerX, centerY + 19f * u, 66f * u * pulse, base);
            rect(batch, centerX - 30f * u, centerY + 14f * u, 60f * u, 12f * u, base);
            face(batch, centerX, centerY + 10f * u, u, Color.BLACK);
        } else if (isDefender(name)) {
            Color shell = name.contains("PUMPKIN") ? new Color(.93f, .40f, .08f, 1f)
                : name.contains("ICE") ? new Color(.45f, .82f, .92f, 1f)
                : new Color(.62f, .39f, .16f, 1f);
            circle(batch, centerX, centerY + 2f * u, 72f * u * pulse, shell);
            rect(batch, centerX - 28f * u, centerY - 27f * u, 56f * u, 48f * u, shell);
            face(batch, centerX, centerY + 6f * u, u, Color.BLACK);
            rect(batch, centerX - 3f * u, centerY - 10f * u, 4f * u, 14f * u,
                new Color(.27f, .16f, .08f, .75f));
        } else if (isExplosive(name)) {
            Color explosive = name.contains("JALAPENO") ? new Color(.93f, .12f, .05f, 1f)
                : name.contains("ICE") ? new Color(.35f, .78f, 1f, 1f)
                : new Color(.83f, .18f, .16f, 1f);
            circle(batch, centerX, centerY + 5f * u, 64f * u * pulse, explosive);
            rectRotated(batch, centerX + 19f * u, centerY + 32f * u, 7f * u, 22f * u,
                new Color(.24f, .16f, .08f, 1f), -32f);
            circle(batch, centerX + 29f * u, centerY + 43f * u, 8f * u,
                new Color(1f, .82f, .12f, 1f));
            face(batch, centerX, centerY + 4f * u, u, Color.BLACK);
        } else {
            float headX = centerX + lean;
            circle(batch, headX, centerY + 13f * u, 56f * u * pulse, base);
            circle(batch, headX + 26f * u, centerY + 15f * u, 27f * u, base);
            circle(batch, headX + 32f * u, centerY + 15f * u, 13f * u,
                new Color(.15f, .35f, .10f, 1f));
            circle(batch, headX - 8f * u, centerY + 23f * u, 10f * u, Color.WHITE);
            circle(batch, headX - 6f * u, centerY + 23f * u, 4f * u, Color.BLACK);
            if (attacking) {
                circle(batch, headX + 45f * u, centerY + 15f * u,
                    (8f + Math.abs((float) Math.sin(time * 18f)) * 7f) * u,
                    new Color(.75f, 1f, .30f, .9f));
            }
        }
        batch.setColor(Color.WHITE);
    }

     
    public void drawProceduralZombie(Batch batch, ZombieType type, String action, float time,
                                     float centerX, float centerY, float maxWidth, float maxHeight,
                                     Color tint) {
        float u = Math.max(1f, Math.min(maxWidth, maxHeight) / 112f);
        String name = type == null ? "ZOMBIE" : type.name();
        float speciesScale = name.contains("GARGANTUAR") ? 1.28f : name.contains("IMP") ? .72f : 1f;
        u *= speciesScale;
        boolean eating = "eat".equals(action);
        boolean special = "special".equals(action);
        boolean dying = "die".equals(action);
        float stride = dying ? 0f : (float) Math.sin(time * (eating ? 11f : 7f));
        float fall = dying ? Math.min(1f, time * 1.4f) : 0f;
        float bodyY = centerY - fall * 18f * u;
        Color skin = tint == null ? new Color(.45f, .58f, .35f, 1f) : tint;
        Color cloth = zombieCloth(name);

        rectRotated(batch, centerX - 14f * u, bodyY - 42f * u, 9f * u, 35f * u,
            new Color(.20f, .16f, .14f, 1f), dying ? 72f : stride * 16f);
        rectRotated(batch, centerX + 5f * u, bodyY - 42f * u, 9f * u, 35f * u,
            new Color(.20f, .16f, .14f, 1f), dying ? 58f : -stride * 16f);
        rect(batch, centerX - 21f * u, bodyY - 16f * u, 42f * u, 46f * u, cloth);
        rect(batch, centerX - 17f * u, bodyY + 22f * u, 34f * u, 9f * u,
            new Color(.72f, .73f, .62f, 1f));

        float armReach = eating || special ? 31f : 18f + stride * 4f;
        rectRotated(batch, centerX + 14f * u, bodyY + 8f * u, 9f * u, armReach * u,
            skin, eating || special ? -76f : -28f);
        rectRotated(batch, centerX - 20f * u, bodyY + 5f * u, 9f * u, 27f * u,
            skin, dying ? 100f : 24f);
        float headX = centerX + (special ? 3f * (float) Math.sin(time * 18f) * u : 0f);
        float headY = bodyY + 46f * u;
        circle(batch, headX, headY, 46f * u, skin);
        rect(batch, headX - 18f * u, headY - 14f * u, 36f * u, 22f * u, skin);
        circle(batch, headX - 9f * u, headY + 7f * u, 10f * u, Color.WHITE);
        circle(batch, headX + 10f * u, headY + 8f * u, 10f * u, Color.WHITE);
        circle(batch, headX - 8f * u, headY + 6f * u, 4f * u, Color.BLACK);
        circle(batch, headX + 9f * u, headY + 7f * u, 4f * u, Color.BLACK);
        rect(batch, headX - 8f * u, headY - 11f * u, 20f * u,
            (eating ? 8f + Math.abs(stride) * 5f : 5f) * u, Color.BLACK);
        drawZombieAccessory(batch, name, headX, headY, bodyY, u, special, time);
        batch.setColor(Color.WHITE);
    }

     
    public void drawProceduralIcon(Batch batch, String kind, float centerX, float centerY,
                                   float maxWidth, float maxHeight, float time) {
        float u = Math.max(1f, Math.min(maxWidth, maxHeight) / 72f);
        String key = kind == null ? "SEED" : kind.toUpperCase(Locale.ROOT);
        if (key.contains("POT")) {
            rect(batch, centerX - 24f * u, centerY - 22f * u, 48f * u, 35f * u,
                new Color(.62f, .28f, .10f, 1f));
            rect(batch, centerX - 29f * u, centerY + 7f * u, 58f * u, 10f * u,
                new Color(.82f, .40f, .14f, 1f));
            rect(batch, centerX - 18f * u, centerY + 16f * u, 36f * u, 7f * u,
                new Color(.20f, .12f, .06f, 1f));
        } else if (key.contains("FOOD")) {
            circle(batch, centerX - 9f * u, centerY + 5f * u, 40f * u,
                new Color(.25f, .82f, .24f, 1f));
            circle(batch, centerX + 12f * u, centerY + 10f * u, 36f * u,
                new Color(.38f, .94f, .30f, 1f));
            rectRotated(batch, centerX, centerY - 18f * u, 6f * u, 40f * u,
                new Color(.12f, .45f, .12f, 1f), -28f);
        } else if (key.contains("COIN") || key.contains("CURRENCY")) {
            circle(batch, centerX, centerY, 54f * u, new Color(1f, .72f, .06f, 1f));
            circle(batch, centerX, centerY, 35f * u, new Color(.92f, .48f, .04f, 1f));
            rect(batch, centerX - 4f * u, centerY - 15f * u, 8f * u, 30f * u,
                new Color(1f, .90f, .25f, 1f));
        } else {
            Color packet = key.contains("DAILY") ? new Color(.92f, .55f, .08f, 1f)
                : key.contains("RANDOM") ? new Color(.58f, .32f, .75f, 1f)
                : new Color(.25f, .66f, .28f, 1f);
            rect(batch, centerX - 25f * u, centerY - 29f * u, 50f * u, 58f * u, packet);
            rect(batch, centerX - 20f * u, centerY - 23f * u, 40f * u, 12f * u,
                new Color(.13f, .22f, .10f, 1f));
            circle(batch, centerX, centerY + 7f * u, 25f * u,
                new Color(.65f, 1f, .35f, 1f));
            if (key.contains("RANDOM")) {
                circle(batch, centerX, centerY + 7f * u, 9f * u, Color.WHITE);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private static final double TWO_PI = Math.PI * 2.0;

    private boolean isSunPlant(String name) {
        return name.contains("SUNFLOWER") || name.contains("SUN_SHROOM")
            || name.contains("MARIGOLD") || name.contains("GOLD_BLOOM");
    }

    private boolean isMushroom(String name) {
        return name.contains("SHROOM") || name.contains("MUSHROOM");
    }

    private boolean isDefender(String name) {
        return name.contains("NUT") || name.contains("PUMPKIN") || name.contains("DURIAN")
            || name.contains("ENDURIAN") || name.contains("GUACODILE");
    }

    private boolean isExplosive(String name) {
        return name.contains("CHERRY") || name.contains("JALAPENO") || name.contains("MINE")
            || name.contains("BOMB") || name.contains("DOOM") || name.contains("GRAPE")
            || name.contains("STRAWBURST");
    }

    private Color zombieCloth(String name) {
        if (name.contains("ALL_STAR")) return new Color(.78f, .08f, .08f, 1f);
        if (name.contains("HUNTER") || name.contains("FROST")) return new Color(.22f, .46f, .65f, 1f);
        if (name.contains("RA_") || name.contains("TOMB")) return new Color(.72f, .57f, .20f, 1f);
        if (name.contains("OCTOPUS") || name.contains("WIZARD")) return new Color(.47f, .20f, .58f, 1f);
        if (name.contains("SNORKEL") || name.contains("SURFER")) return new Color(.16f, .48f, .62f, 1f);
        return new Color(.34f, .25f, .20f, 1f);
    }

    private void drawZombieAccessory(Batch batch, String name, float x, float headY,
                                     float bodyY, float u, boolean special, float time) {
        if (name.contains("RA_")) {
            rect(batch, x - 24f * u, headY + 18f * u, 48f * u, 12f * u,
                new Color(.92f, .70f, .12f, 1f));
            rect(batch, x - 8f * u, headY + 27f * u, 16f * u, 18f * u,
                new Color(.25f, .58f, .78f, 1f));
        }
        if (name.contains("SNORKEL")) {
            rect(batch, x + 20f * u, headY + 7f * u, 5f * u, 34f * u,
                new Color(.88f, .72f, .18f, 1f));
            rect(batch, x + 20f * u, headY + 37f * u, 16f * u, 5f * u,
                new Color(.88f, .72f, .18f, 1f));
        }
        if (name.contains("HUNTER")) {
            circle(batch, x, headY + 11f * u, 56f * u, new Color(.30f, .65f, .82f, .72f));
        }
        if (name.contains("OCTOPUS") && special) {
            circle(batch, x + 43f * u, bodyY + 20f * u + (float) Math.sin(time * 8f) * 5f * u,
                25f * u, new Color(.68f, .25f, .77f, 1f));
        }
        if (name.contains("TOMBRAISER") && special) {
            rectRotated(batch, x + 24f * u, bodyY + 18f * u, 6f * u, 42f * u,
                new Color(.92f, .88f, .70f, 1f), -58f);
        }
    }

    private void face(Batch batch, float x, float y, float u, Color color) {
        circle(batch, x - 10f * u, y + 6f * u, 8f * u, Color.WHITE);
        circle(batch, x + 10f * u, y + 6f * u, 8f * u, Color.WHITE);
        circle(batch, x - 9f * u, y + 5f * u, 3f * u, color);
        circle(batch, x + 9f * u, y + 5f * u, 3f * u, color);
        rect(batch, x - 8f * u, y - 10f * u, 16f * u, 4f * u, color);
    }

    private void circle(Batch batch, float x, float y, float size, Color color) {
        batch.setColor(color);
        batch.draw(fallbackCircle, x - size * .5f, y - size * .5f, size, size);
    }

    private void rect(Batch batch, float x, float y, float width, float height, Color color) {
        batch.setColor(color);
        batch.draw(fallbackPixel, x, y, width, height);
    }

    private void rectRotated(Batch batch, float x, float y, float width, float height,
                             Color color, float rotation) {
        batch.setColor(color);
        batch.draw(fallbackPixel, x, y, width * .5f, 0f, width, height,
            1f, 1f, rotation, 0, 0, 1, 1, false, false);
    }

    public boolean drawMower(Batch batch, ChapterType chapter,
                             float centerX, float centerY, float maxWidth, float maxHeight) {
        String id = mowers.getOrDefault(chapter, defaultMower);
        return drawRegionCentered(batch, id, centerX, centerY, maxWidth, maxHeight, Color.WHITE);
    }

    public boolean drawGrave(Batch batch, ChapterType chapter,
                             float centerX, float centerY, float maxWidth, float maxHeight, Color tint) {
        String id = graves.getOrDefault(chapter, defaultGrave);
        return drawRegionCentered(batch, id, centerX, centerY, maxWidth, maxHeight,
            tint == null ? Color.WHITE : tint);
    }

    public boolean drawHud(Batch batch, String name,
                           float centerX, float centerY, float maxWidth, float maxHeight, Color tint) {
        return drawRegionCentered(batch, hud.get(name), centerX, centerY, maxWidth, maxHeight,
            tint == null ? Color.WHITE : tint);
    }

    private boolean drawRegionCentered(Batch batch, String id, float centerX, float centerY,
                                       float maxWidth, float maxHeight, Color tint) {
        TextureRegion region = region(id);
        if (region == null) return false;
        float rw = Math.max(1f, region.getRegionWidth());
        float rh = Math.max(1f, region.getRegionHeight());
        float scale = Math.min(maxWidth / rw, maxHeight / rh);
        float width = rw * scale;
        float height = rh * scale;
        batch.setColor(tint);
        batch.draw(region, centerX - width * .5f, centerY - height * .5f, width, height);
        batch.setColor(Color.WHITE);
        return true;
    }

    private void drawAspectFill(Batch batch, TextureRegion region, float x, float y,
                                float maxWidth, float maxHeight, Color tint) {
        float rw = Math.max(1f, region.getRegionWidth());
        float rh = Math.max(1f, region.getRegionHeight());
        float scale = Math.max(maxWidth / rw, maxHeight / rh);
        float width = rw * scale;
        float height = rh * scale;
        float dx = x + (maxWidth - width) * .5f;
        float dy = y + (maxHeight - height) * .5f;
        batch.setColor(tint);
        batch.draw(region, dx, dy, width, height);
        batch.setColor(Color.WHITE);
    }

    private TextureRegion region(String id) {
        if (textures == null || id == null || id.isBlank()) return null;
        if (regionCache.containsKey(id)) return regionCache.get(id);
        try {
            TextureRegion region = textures.region(id);
            if (region != null) regionCache.put(id, region);
            return region;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean pamExists(AnimationSpec spec) {
        return spec != null && imagesRoot.child(spec.pam).exists();
    }

    private int countDirectPngs(FileHandle directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) return 0;
        int count = 0;
        for (FileHandle file : directory.list()) {
            if (!file.isDirectory() && "png".equalsIgnoreCase(file.extension())) count++;
        }
        return count;
    }

    public String diagnostics() {
        StringBuilder sb = new StringBuilder();
        sb.append("PvZ graphics integration\n\n");
        sb.append("Status: ").append(initializationMessage).append('\n');
        sb.append("Asset root: ").append(root.path()).append("\n");
        sb.append("RESOURCES.json: ").append(root.child("RESOURCES.json").exists() || root.child("resources.json").exists() ? "found" : "MISSING").append('\n');
        sb.append("animations.json: ").append(root.child("animations.json").exists() ? "found" : "MISSING").append('\n');
        sb.append("Direct atlas PNG files: ").append(atlasPngCount).append('\n');
        sb.append("Mapped PAM files found: ").append(mappedPamPresent).append('/').append(mappedPamTotal).append('\n');
        sb.append("Plant mappings: ").append(plantAnimations.size()).append('/').append(PlantType.values().length).append('\n');
        sb.append("Zombie mappings: ").append(zombieAnimations.size()).append('/').append(ZombieType.values().length).append('\n');

        Map<String, String> missing = unmappedTypes();
        if (!missing.isEmpty()) {
            sb.append("\nNo matching PAM was found in the supplied animation manifest for:\n");
            missing.forEach((type, reason) -> sb.append(" - ").append(type).append(": ").append(reason).append('\n'));
        }
        if (!isLibPvzActive()) {
            sb.append("\nThe game intentionally falls back to generated graphics until the large extracted folders are installed.\n");
            sb.append("Copy ATLASES and IMAGES exactly as described in PVZ_ASSET_SETUP.md, then restart the game.");
        }
        return sb.toString();
    }

    private Map<String, String> unmappedTypes() {
        Map<String, String> missing = new LinkedHashMap<>();
        for (PlantType type : PlantType.values()) {
            if (!plantAnimations.containsKey(type)) missing.put("Plant " + type.name(), "no unambiguous manifest match");
        }
        for (ZombieType type : ZombieType.values()) {
            if (!zombieAnimations.containsKey(type)) missing.put("Zombie " + type.name(), "no unambiguous manifest match");
        }
        return missing;
    }

    private String safeMessage(Throwable t) {
        if (t == null || t.getMessage() == null || t.getMessage().isBlank()) return t == null ? "unknown error" : t.getClass().getSimpleName();
        return t.getMessage().replace('\n', ' ').replace('\r', ' ');
    }

    @Override
    public void dispose() {
        regionCache.clear();
        fallbackCircle.dispose();
        fallbackPixel.dispose();
        if (textures != null) {
            textures.dispose();
            textures = null;
        }
        player = null;
    }

    private static final class AnimationSpec {
        private final String pam;
        private final int canvasWidth;
        private final int canvasHeight;
        private final Map<String, String> clips;

        private AnimationSpec(String pam, int canvasWidth, int canvasHeight, Map<String, String> clips) {
            this.pam = pam;
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
            this.clips = clips;
        }

        static AnimationSpec from(JsonNode node) {
            String pam = node.path("pam").asText();
            JsonNode canvas = node.path("canvas");
            int width = canvas.isArray() && canvas.size() > 0 ? canvas.get(0).asInt(390) : 390;
            int height = canvas.isArray() && canvas.size() > 1 ? canvas.get(1).asInt(390) : 390;
            Map<String, String> clips = new HashMap<>();
            JsonNode clipNode = node.path("clips");
            if (clipNode.isObject()) {
                clipNode.fields().forEachRemaining(entry -> clips.put(entry.getKey(), entry.getValue().asText()));
            }
            return new AnimationSpec(pam.replace('\\', '/'), width, height, clips);
        }

        String clip(String action) {
            String normalized = action == null ? "idle" : action.trim();
            String result = clips.get(normalized);
            if (result == null && !"idle".equals(normalized)) result = clips.get("idle");
            return result;
        }
    }
}
