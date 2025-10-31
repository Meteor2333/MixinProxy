package cc.meteormc.mixinproxy.service;

import cc.meteormc.mixinproxy.Bootstrap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.transformers.MixinClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class MixinService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider, IClassTracker {
    public static final AtomicReference<IMixinTransformer> TRANSFORMER = new AtomicReference<>();

    @Override
    public String getName() {
        return "MixinService";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void offer(IMixinInternal internal) {
        super.offer(internal);
        if (internal instanceof IMixinTransformerFactory) {
            TRANSFORMER.set(((IMixinTransformerFactory) internal).createTransformer());
        }
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Arrays.asList("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        try {
            URL location = this.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            return new ContainerHandleURI(location.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new ContainerHandleVirtual(this.getName());
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return Bootstrap.CLASSLOADER.getResourceAsStream(name);
    }

    @Override
    protected ILogger createLogger(String name) {
        return new LoggerAdapterConsole(name);
    }

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Bootstrap.CLASSLOADER.loadClass(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Bootstrap.CLASSLOADER);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Bootstrap.class.getClassLoader());
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, runTransformers, ClassReader.EXPAND_FRAMES);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
        byte[] bytes = Bootstrap.CLASSLOADER.getClassBytes(name, runTransformers);
        ClassReader classReader = new MixinClassReader(bytes, name);
        ClassNode node = new ClassNode();
        classReader.accept(node, readerFlags);
        return node;
    }

    @Override
    public void registerInvalidClass(String className) {

    }

    @Override
    public boolean isClassLoaded(String className) {
        return Bootstrap.CLASSLOADER.findLoadedClassFwd(className) != null;
    }

    @Override
    public String getClassRestrictions(String className) {
        return "";
    }
}
