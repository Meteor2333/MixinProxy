package cc.meteormc.mixinproxy;

import cc.meteormc.project.Main;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class Bootstrap {
    public static final ProxyClassLoader CLASSLOADER = new ProxyClassLoader(Bootstrap.class.getClassLoader());

    public static void main(String[] args) {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.json", null); // Replace with your actual name

        try {
            Method method = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
            method.setAccessible(true);
            method.invoke(null, MixinEnvironment.Phase.INIT);
            method.invoke(null, MixinEnvironment.Phase.DEFAULT);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set MixinEnvironment phase!", e);
        }

        Thread thread = new Thread(() -> {
            try {
                Class<?> mainClass = Class.forName(Main.class.getName(), true, CLASSLOADER);
                MethodHandle mainHandle = MethodHandles.lookup().findStatic(
                        mainClass,
                        "main",
                        MethodType.methodType(Void.TYPE, String[].class)
                ).asFixedArity();
                mainHandle.invoke((Object) args);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }, "ProxyMain");
        thread.setContextClassLoader(CLASSLOADER);
        thread.start();
    }
}
