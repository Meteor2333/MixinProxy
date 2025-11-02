package cc.meteormc.mixinproxy.bootstrap;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

class LocalClassLoader extends URLClassLoader {
    public LocalClassLoader() {
        super(new URL[0]);
        try {
            this.addURL(
                    Bootstrap.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
                            .toURL()
            );
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalStateException("Failed to initialize LocalClassLoader!", e);
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            Class<?> clazz = this.findLoadedClass(name);
            if (clazz == null) {
                try {
                    clazz = this.findClass(name);
                } catch (ClassNotFoundException e) {
                    clazz = super.loadClass(name, resolve);
                }
            }

            if (resolve) {
                this.resolveClass(clazz);
            }

            return clazz;
        }
    }
}
