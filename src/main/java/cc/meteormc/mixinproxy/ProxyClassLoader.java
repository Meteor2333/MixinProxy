package cc.meteormc.mixinproxy;

import cc.meteormc.mixinproxy.service.MixinService;
import com.google.common.io.ByteStreams;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ProxyClassLoader extends URLClassLoader {
    private static final List<String> DISALLOWED_CLASSES = Arrays.asList(
            "java.",
            "javax.",
            "jdk.",
            "com.sun.",
            "sun.",
            "javafx.",
            "org.spongepowered.",
            "org.objectweb.asm."
    );
    private static final Logger LOGGER = Logger.getLogger("PrimaryClassLoader");

    public ProxyClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (String prefix : DISALLOWED_CLASSES) {
            if (!name.startsWith(prefix)) continue;
            return super.loadClass(name, resolve);
        }

        synchronized (this.getClassLoadingLock(name)) {
            Class<?> clazz = this.findLoadedClass(name);

            if (clazz == null) {
                clazz = this.findClass(name);
            }

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
            throw new UncheckedIOException(e);
        }
    }

    public Class<?> findLoadedClassFwd(String name) {
        return this.findLoadedClass(name);
    }

    public byte[] getClassBytes(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        String fileName = name.replace('.', '/').concat(".class");

        byte[] bytes;
        URL url = this.getResource(fileName);
        if (url == null) {
            try (InputStream is = this.getResourceAsStream(fileName)) {
                bytes = is != null ? ByteStreams.toByteArray(is) : null;
            }
        } else {
            try (InputStream is = url.openStream()) {
                bytes = ByteStreams.toByteArray(is);
            }
        }

        if (bytes == null) {
            throw new ClassNotFoundException("The specified class '" + name + "' was not found");
        }

        if (runTransformers) {
            byte[] before = bytes;
            IMixinTransformer transformer = MixinService.TRANSFORMER.get();
            if (transformer != null) {
                bytes = transformer.transformClassBytes(name, name, bytes);
            }

            if (bytes != before) {
                LOGGER.info(
                        "Mixin target class transformed successfully! " +
                                "ClassName: " + name + ", " +
                                "Before: " + before.length + " bytes, " +
                                "After: " + bytes.length + " bytes"
                );
            }
        }

        return bytes;
    }

    public CodeSource getCodeSource(String name) throws IOException {
        String fileName = name.replace('.', '/').concat(".class");
        URL url = this.getResource(fileName);
        String protocol = url != null ? url.getProtocol() : null;
        if (!"file".equals(protocol) && !"jar".equals(protocol)) {
            return null;
        }

        Path cs;
        try {
            URLConnection connection = url.openConnection();
            if (connection instanceof JarURLConnection) {
                cs = Paths.get(((JarURLConnection) connection).getJarFileURL().toURI());
            } else {
                String path = url.getPath();
                if (!path.endsWith(fileName)) {
                    return null;
                }

                cs = Paths.get(new URL(
                        url.getProtocol(),
                        url.getHost(),
                        url.getPort(),
                        path.substring(0, path.length() - fileName.length())
                ).toURI());
            }
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }

        URI uri = cs.toRealPath().toUri();
        URLConnection connection = new URL("jar:" + uri + "!/").openConnection();
        if (connection instanceof JarURLConnection) {
            return new CodeSource(uri.toURL(), ((JarURLConnection) connection).getCertificates());
        } else {
            return null;
        }
    }
}
