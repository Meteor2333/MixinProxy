package cc.meteormc.mixinproxy;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ProxyClassLoader extends URLClassLoader {
    private IMixinTransformer transformer;

    private static final Logger LOGGER = Logger.getLogger(ProxyClassLoader.class.getName());
    private static final List<String> ALLOWED_CLASSES = Arrays.asList(
            "cc.meteormc.project."   // Provide the package(s) of the Mixin target classes here
    );

    public ProxyClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public void offerTransformer(IMixinTransformer transformer) {
        if (this.transformer == null) {
            this.transformer = transformer;
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        boolean allowed = false;
        for (String prefix : ProxyClassLoader.ALLOWED_CLASSES) {
            if (name.startsWith(prefix)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            return super.loadClass(name, resolve);
        }

        synchronized (this.getClassLoadingLock(name)) {
            // Check if the class has already been loaded
            Class<?> clazz = this.findLoadedClass(name);

            // Then try to load the class from the current ClassLoader
            if (clazz == null) {
                clazz = this.findClass(name);
            }

            // If the class is still not found use the parent ClassLoader to load it
            if (clazz == null) {
                clazz = super.findClass(name);
            }

            if (resolve) {
                this.resolveClass(clazz);
            }

            return clazz;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] bytes = this.getClassBytes(name, true);
            return this.defineClass(name, bytes, 0, bytes.length, this.getCodeSource(name));
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public byte[] getClassBytes(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        byte[] bytes;
        try (InputStream is = this.getResourceAsStream(ProxyClassLoader.toClassPath(name))) {
            bytes = is != null ? ByteStreams.toByteArray(is) : null;
        }

        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }

        if (runTransformers) {
            byte[] before = bytes;
            if (this.transformer != null) {
                bytes = this.transformer.transformClassBytes(name, name, bytes);
            }

            if (MixinProxy.DEBUG && bytes != before) {
                RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
                File dir = new File(
                        new File("mixin-dump"),
                        LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(runtime.getStartTime()),
                                ZoneId.systemDefault()
                        ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss"))
                );
                dir.mkdirs();

                String fileName = name.replace('.', '_');
                File beforeFile = new File(dir, fileName.concat(".before.class"));
                if (beforeFile.createNewFile()) {
                    Files.write(before, beforeFile);
                }
                File afterFile = new File(dir, fileName.concat(".after.class"));
                if (afterFile.createNewFile()) {
                    Files.write(bytes, afterFile);
                }

                ProxyClassLoader.LOGGER.info(
                        "Mixin target class transformed successfully! " +
                                "ClassName: " + name + ", " +
                                "Before: " + beforeFile + "(" + before.length + " bytes), " +
                                "After: " + afterFile + "(" + bytes.length + " bytes)."
                );
            }
        }

        return bytes;
    }

    // Attempts to determine the CodeSource (origin) of a class.
    // For JAR entries, this method extracts the JAR file URL and its certificates (if available).
    // For directory entries, the location points to the root directory.
    private CodeSource getCodeSource(String name) throws IOException {
        String classPath = ProxyClassLoader.toClassPath(name);
        URL url = this.getResource(classPath);
        if (url == null) return null;

        URL location;
        Certificate[] certificates;
        URLConnection connection = url.openConnection();
        if (connection instanceof JarURLConnection) {
            JarURLConnection jar = (JarURLConnection) connection;
            location = jar.getJarFileURL();
            certificates = jar.getCertificates();
        } else {
            String path = url.getPath();
            if (path.endsWith(classPath)) {
                location = new URL(
                        url.getProtocol(),
                        url.getHost(),
                        url.getPort(),
                        path.substring(0, path.length() - classPath.length())
                );
                certificates = null;
            } else {
                return null;
            }
        }

        return new CodeSource(location, certificates);
    }

    private static String toClassPath(String name) {
        return name.replace('.', '/').concat(".class");
    }
}
