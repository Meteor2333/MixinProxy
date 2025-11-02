package cc.meteormc.mixinproxy;

import cc.meteormc.mixinproxy.service.MixinService;
import com.google.common.io.ByteStreams;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

// Bootstrap -> CraftBukkit Bootstrap -> MixinProxy Launcher -> MixinProxy -> Main
public class MixinProxy {
    public static final boolean DEBUG = false;

    private static final Logger LOGGER = Logger.getLogger(MixinProxy.class.getName());
    public static final ProxyClassLoader CLASSLOADER = new ProxyClassLoader(MixinProxy.class.getClassLoader());

    public static void main(String[] args) throws RuntimeException {
        // Initialize Mixin
        MixinBootstrap.init();
        for (Map.Entry<String, byte[]> mixin : MixinProxy.getMixins().entrySet()) {
            String name = mixin.getKey();
            MixinService.TEMP_RESOURCES.put(name, mixin.getValue());
            Mixins.addConfiguration(name, null);
            MixinService.TEMP_RESOURCES.remove(name);
        }

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
                String mainClass;
                try (InputStream is = CLASSLOADER.getResourceAsStream("META-INF/main-class")) {
                    if (is == null) throw new FileNotFoundException("Failed to find the main class!");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        mainClass = reader.readLine();
                    }
                }

                MixinProxy.invokeMain(mainClass, CLASSLOADER, args);
                if (DEBUG) {
                    MixinProxy.LOGGER.info("Starting server... | Main class: " + mainClass + ".");
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }, "ProxyMain");
        thread.setContextClassLoader(CLASSLOADER);
        thread.start();
    }

    private static Map<String, byte[]> getMixins() throws UncheckedIOException {
        Map<String, byte[]> mixins = new HashMap<>();
        File[] plugins = new File("plugins").listFiles();
        if (plugins == null) return mixins;

        for (File plugin : plugins) {
            if (plugin.isDirectory()) continue;
            if (!plugin.getName().endsWith(".jar")) continue;

            try (JarFile file = new JarFile(plugin)) {
                Enumeration<JarEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.endsWith("mixins.json")) continue;

                    // Prevent duplicate file names
                    mixins.put(
                            name.concat('(' + UUID.randomUUID().toString().substring(0, 8) + ')'),
                            ByteStreams.toByteArray(file.getInputStream(entry))
                    );
                    MixinProxy.CLASSLOADER.addURL(plugin.toURI().toURL());
                    if (MixinProxy.DEBUG) {
                        MixinProxy.LOGGER.info("Found a Mixin configuration file named " + name + " from " + file.getName() + ".");
                    }
                    break;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return mixins;
    }

    private static Class<?> invokeMain(String className, ClassLoader loader, String[] args) throws Throwable {
        Class<?> clazz = Class.forName(className, false, loader);
        MethodHandles.lookup().findStatic(
                clazz,
                "main",
                MethodType.methodType(Void.TYPE, String[].class)
        ).asFixedArity().invoke((Object) args);
        return clazz;
    }

    // Use a Launcher class to replace the ClassLoader used for calls
    public static class Launcher {
        private static URL[] URLS;
        private static String[] ARGS;

        public static void main(String[] args) {
            Launcher.ARGS = args;
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader instanceof URLClassLoader) {
                Launcher.URLS = ((URLClassLoader) loader).getURLs();
            }
        }

        public static void launch(URLClassLoader loader, Consumer<URL> urlCallback) throws Throwable {
            for (URL url : Launcher.URLS) {
                urlCallback.accept(url);
                if (MixinProxy.DEBUG) {
                    MixinProxy.LOGGER.info("Added a URL: " + url + ".");
                }
            }

            MixinProxy.invokeMain(MixinProxy.class.getName(), loader, Launcher.ARGS);
        }
    }
}
