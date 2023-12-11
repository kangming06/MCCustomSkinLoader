package customskinloader.fake;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import customskinloader.CustomSkinLoader;
import customskinloader.fake.itf.IFakeSkinManagerCacheKey;
import customskinloader.loader.MojangAPILoader;
import customskinloader.profile.ModelManager0;
import customskinloader.profile.UserProfile;
import customskinloader.utils.HttpTextureUtil;
import customskinloader.utils.TextureUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.SkinManager;

public class FakeSkinManager {
    /**
     * Invoked from {@link SkinManager(net.minecraft.client.renderer.texture.TextureManager, File, MinecraftSessionService)}
     */
    public static void setSkinCacheDir(File skinCacheDirectory) {
        HttpTextureUtil.defaultCacheDir = skinCacheDirectory;
    }

    /**
     * Invoked from {@link SkinManager(net.minecraft.client.renderer.texture.TextureManager, Path, MinecraftSessionService, java.util.concurrent.Executor)}
     */
    public static void setSkinCacheDir(Path skinCacheDirectory) {
        HttpTextureUtil.defaultCacheDir = skinCacheDirectory.toFile();
    }

    private static CacheLoader<Object, ?> cacheLoader;
    /**
     * Invoked from {@link SkinManager(net.minecraft.client.renderer.texture.TextureManager, Path, MinecraftSessionService, java.util.concurrent.Executor)}
     */
    public static CacheLoader<Object, ?> setCacheLoader(CacheLoader<Object, ?> loader) {
        return cacheLoader = loader;
    }

    /**
     * Invoked from {@link SkinManager#getOrLoad(GameProfile)}
     */
    public static Object loadCache(LoadingCache<?, ?> loadingCache, Object cacheKey, GameProfile profile) throws Exception {
        return cacheLoader.load(FakeCacheKey.wrapCacheKey(cacheKey, profile));
    }

    /**
     * Invoked from {@link SkinManager#loadSkin(MinecraftProfileTexture, MinecraftProfileTexture.Type, SkinManager.SkinAvailableCallback)}
     */
    public static Object[] createThreadDownloadImageData(ImmutableList<Object> list, MinecraftProfileTexture profileTexture, MinecraftProfileTexture.Type textureType) {
        Object[] params = list.toArray();
        if (profileTexture instanceof FakeMinecraftProfileTexture && params.length > 1) {
            FakeMinecraftProfileTexture fakeProfileTexture = (FakeMinecraftProfileTexture) profileTexture;
            params[0] = fakeProfileTexture.getCacheFile();
            if (params[params.length - 2] instanceof Boolean) {
                params[params.length - 2] = true;
            }
            params[params.length - 1] = new BaseBuffer((Runnable) params[params.length - 1], textureType, fakeProfileTexture);
        }
        return params;
    }


    private final static String KEY = "CustomSkinLoaderInfo";
    /**
     * Invoked from {@link SkinManager#loadProfileTextures(GameProfile, SkinManager.SkinAvailableCallback, boolean)}
     */
    public static void loadProfileTextures(Runnable runnable, GameProfile profile) {
        CustomSkinLoader.loadProfileTextures(() -> CustomSkinLoader.loadProfileLazily(profile, p -> {
            profile.getProperties().putAll(KEY, p.toProperties());
            runnable.run();
            return null;
        }));
    }

    /**
     * 23w31a+
     * Invoked from net.minecraft.client.resources.SkinManager$1#load(net.minecraft.client.resources.SkinManager$CacheKey)
     */
    public static Object[] loadProfileTextures(ImmutableList<Object> list, Object cacheKey) {
        Object[] params = list.toArray();
        GameProfile profile = FakeCacheKey.unwrapCacheKey(cacheKey);
        if (!profile.getProperties().containsKey(SKULL_KEY)) {
            final Supplier<?> supplier = (Supplier<?>) params[0];
            params[0] = (Supplier<?>) () -> CustomSkinLoader.loadProfileLazily(profile, p -> {
                profile.getProperties().putAll(KEY, p.toProperties());
                FakeCacheKey.wrapCacheKey(cacheKey, profile);
                return supplier.get();
            });
            params[1] = CustomSkinLoader.THREAD_POOL;
        } else {
            params[1] = Minecraft.getMinecraft();
        }
        return params;
    }

    /**
     * Invoked from {@link SkinManager#lambda$loadProfileTextures$1(GameProfile, boolean, SkinAvailableCallback)}
     */
    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getUserProfile(MinecraftSessionService sessionService, GameProfile profile, boolean requireSecure) {
        return ModelManager0.fromUserProfile(UserProfile.fromProperties(profile.getProperties().values()));
    }

    /**
     * Invoked from {@link SkinManager#lambda$null$0(Map, SkinAvailableCallback)}
     */
    public static void loadElytraTexture(SkinManager skinManager, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map, SkinManager.SkinAvailableCallback skinAvailableCallback) {
        for (int i = 2; i < MinecraftProfileTexture.Type.values().length; i++) {
            MinecraftProfileTexture.Type type = MinecraftProfileTexture.Type.values()[i];
            if (map.containsKey(type)) {
                skinManager.loadSkin(map.get(type), type, skinAvailableCallback);
            }
        }
    }

    private final static String SKULL_KEY = "CSL$IsSkull";
    /**
     * 23w31a+
     * Invoked from {@link SkinManager#getInsecureSkin(GameProfile)}
     */
    public static void setSkullType(GameProfile profile) {
        profile.getProperties().removeAll(SKULL_KEY);
        profile.getProperties().put(SKULL_KEY, new Property(SKULL_KEY, "true"));
    }

    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> loadSkinFromCache(GameProfile profile) {
        return CustomSkinLoader.loadProfileFromCache(profile);
    }

    /**
     * 23w31a ~ 23w41a
     * Invoked from net.minecraft.client.resources.SkinManager$1#lambda$load$0(GameProfile)
     */
    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> loadSkinFromCache(MinecraftSessionService sessionService, GameProfile profile, boolean requireSecure) {
        if (profile.getProperties().containsKey(SKULL_KEY)) {
            profile.getProperties().removeAll(SKULL_KEY);
            return CustomSkinLoader.loadProfileFromCache(profile);
        } else {
            return getUserProfile(sessionService, profile, requireSecure);
        }
    }

    /**
     * 23w42a+
     * Invoked from net.minecraft.client.resources.SkinManager$1#lambda$load$0(SkinManager.CacheKey, MinecraftSessionService)
     */
    public static Object loadSkinFromCache(MinecraftSessionService sessionService, Property property) {
        return FakeCacheKey.createMinecraftProfileTextures(loadSkinFromCache(sessionService, FakeCacheKey.unwrapProperty(property), false));
    }

    private static boolean shouldJudgeType(MinecraftProfileTexture texture) {
        return texture != null && "auto".equals(texture.getMetadata("model"));
    }

    private static class BaseBuffer implements IImageBuffer {
        private IImageBuffer buffer;

        private final Runnable callback;
        private final FakeMinecraftProfileTexture texture;

        public BaseBuffer(Runnable callback, MinecraftProfileTexture.Type type, FakeMinecraftProfileTexture texture) {
            this.callback = callback;
            this.texture = texture;

            switch (type) {
                case SKIN: this.buffer = new FakeSkinBuffer(); break;
                case CAPE: this.buffer = new FakeCapeBuffer(); break;
            }
        }

        @Override
        public NativeImage func_195786_a(NativeImage image) {
            return buffer instanceof FakeSkinBuffer ? ((FakeSkinBuffer) buffer).func_195786_a(image) : image;
        }

        @Override
        public BufferedImage parseUserSkin(BufferedImage image) {
            return buffer instanceof FakeSkinBuffer ? ((FakeSkinBuffer) buffer).parseUserSkin(image) : image;
        }

        @Override
        public void skinAvailable() {
            if (buffer != null) {
                buffer.skinAvailable();
                if (shouldJudgeType(texture) && buffer instanceof FakeSkinBuffer) {
                    //Auto judge skin type
                    String type = ((FakeSkinBuffer) buffer).judgeType();
                    texture.setModel(type);
                }
            }

            if (this.callback != null) {
                this.callback.run();
            }
        }
    }

    public static class FakeCacheKey {
        public static Object wrapCacheKey(Object cacheKey, GameProfile profile) {
            IFakeSkinManagerCacheKey fakeCacheKey = (IFakeSkinManagerCacheKey) cacheKey;
            if (fakeCacheKey.profile() != null) {
                return cacheKey; // 23w31a ~ 23w41a
            } else {
                TextureUtil.AuthlibField.PROPERTY_SIGNATURE.set(fakeCacheKey.packedTextures(), MojangAPILoader.GSON.toJson(profile, GameProfile.class));
                return fakeCacheKey; // 23w42a+
            }
        }

        public static GameProfile unwrapCacheKey(Object cacheKey) {
            IFakeSkinManagerCacheKey fakeCacheKey = (IFakeSkinManagerCacheKey) cacheKey;
            if (fakeCacheKey.profile() != null) {
                return fakeCacheKey.profile(); // 23w31a ~ 23w41a
            } else {
                return unwrapProperty(fakeCacheKey.packedTextures()); // 23w42a+
            }
        }

        public static GameProfile unwrapProperty(Property property) {
            return MojangAPILoader.GSON.fromJson((String) TextureUtil.AuthlibField.PROPERTY_SIGNATURE.get(property), GameProfile.class); // 23w42a+
        }

        public static Object createMinecraftProfileTextures(Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures) {
            return new MinecraftProfileTextures(textures.get(MinecraftProfileTexture.Type.SKIN), textures.get(MinecraftProfileTexture.Type.CAPE), textures.get(MinecraftProfileTexture.Type.ELYTRA), SignatureState.SIGNED);
        }
    }
}
