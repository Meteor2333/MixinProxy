package cc.meteormc.mixinproxy;

import cc.meteormc.project.Main;
import jdk.nashorn.internal.runtime.linker.Bootstrap;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

// MixinProxy -> Main
public class MixinProxy {
    public static final boolean DEBUG = false;

    public static final ProxyClassLoader CLASSLOADER = new ProxyClassLoader(MixinProxy.class.getClassLoader());

    static {
        try {
            CLASSLOADER.addURL(
                    Bootstrap.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
                            .toURL()
            );
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalStateException("Failed to initialize MixinProxy!", e);
        }
    }

    public static void main(String[] args) throws RuntimeException {
        // Initialize Mixin
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.json", null);

        // Change the current Mixin phase
        try {
            Method method = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
            method.setAccessible(true);
            method.invoke(null, MixinEnvironment.Phase.INIT);
            method.invoke(null, MixinEnvironment.Phase.DEFAULT);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to change MixinEnvironment phase!", e);
        }

        // Start the server
        Thread thread = new Thread(() -> {
            try {
                Class<?> clazz = Class.forName(Main.class.getName(), false, CLASSLOADER);
                MethodHandles.lookup().findStatic(
                        clazz,
                        "main",
                        MethodType.methodType(Void.TYPE, String[].class)
                ).asFixedArity().invoke((Object) args);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }, "ProxyMain");
        thread.setContextClassLoader(CLASSLOADER);
        thread.start();
    }
}
