package cc.meteormc.mixinproxy.bootstrap;

import cc.meteormc.mixinproxy.MixinProxy;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

// ⚠️ WARNING:
// This class (Bootstrap) serves as the system entry point and must be loaded
// exclusively by the application’s default ClassLoader (AppClassLoader).
//
// Explanation:
// 1. ClassLoader hierarchy and boundaries:
//    - All classes in this project, except for Bootstrap, are loaded by a custom
//      ProxyClassLoader, while Bootstrap itself is loaded by the system AppClassLoader
//      as the root entry point.
//    - If a class loaded by ProxyClassLoader directly references Bootstrap,
//      the JVM treats it as a cross-loader type reference, which can lead to
//      LinkageError or ClassCastException.
//
// 2. Type isolation:
//    - Even if two classes share the same name and package, they are considered
//      distinct types if loaded by different class loaders.
//    - If ProxyClassLoader attempts to reload or access symbols defined by Bootstrap,
//      it will result in loader constraint violations.
//
// 3. Load order and stability:
//    - Bootstrap is designed to be the only class initialized under AppClassLoader.
//    - Its Class object instance must remain globally unique; duplicating or
//      reloading it will break the system’s initialization process.
//
// Usage Guidelines:
// - Never reference Bootstrap directly from classes loaded by ProxyClassLoader
//   or any custom ClassLoader.
// - External modules that need to interact with Bootstrap should use controlled
//   entry points (e.g., reflection, bridge methods, or messaging interfaces).
// - Do not redefine or reload the Bootstrap class under any custom ClassLoader.
//
// In short:
// Bootstrap acts as the “entry anchor” of the entire system.
// It must exist only within the AppClassLoader namespace, and any cross-loader
// access is considered unsafe.
class Bootstrap {
    private static final LocalClassLoader CLASSLOADER = new LocalClassLoader();

    public static void main(String[] args) throws Throwable {
        String fileName = "none";
        List<String> parsedArgs = new ArrayList<>(Arrays.asList(args));
        Iterator<String> iterator = parsedArgs.iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            if (arg.startsWith("-s") || arg.startsWith("--srv") || arg.startsWith("--server-file")) {
                iterator.remove();
                if (arg.contains("=")) {
                    fileName = arg.substring(arg.indexOf('=') + 1);
                } else if (iterator.hasNext()) {
                    String next = iterator.next();
                    iterator.remove();
                    fileName = next;
                }
            }
        }

        File srv = new File(fileName);
        if (!srv.exists()) {
            throw new IllegalArgumentException("Server file is either not provided or does not exist!");
        }

        Bootstrap.CLASSLOADER.addURL(srv.toURI().toURL());
        String launcherClass = MixinProxy.Launcher.class.getName();
        System.setProperty("bundlerMainClass", launcherClass);
        Class<?> mainClass = Class.forName("org.bukkit.craftbukkit.bootstrap.Main", true, Bootstrap.CLASSLOADER);
        mainClass.getMethod("main", String[].class).invoke(null, (Object) parsedArgs.toArray(new String[0]));

        // Use reflection to isolate classes from two different ClassLoader environments
        Class.forName(launcherClass).getMethod(
                "launch",
                URLClassLoader.class,
                Consumer.class
        ).invoke(null, Bootstrap.CLASSLOADER, (Consumer<URL>) Bootstrap.CLASSLOADER::addURL);
    }
}
