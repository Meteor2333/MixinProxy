package cc.meteormc.mixinproxy.service;

import cc.meteormc.mixinproxy.MixinProxy;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.logging.LoggerAdapterDefault;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.transformers.MixinClassReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MixinService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
    public static final Map<String, byte[]> TEMP_RESOURCES = new ConcurrentHashMap<>();

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
            MixinProxy.CLASSLOADER.offerTransformer(((IMixinTransformerFactory) internal).createTransformer());
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
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Arrays.asList(MixinPlatformAgentDefault.class.getName());
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
        // Try to get temporary resources
        byte[] tempResource = MixinService.TEMP_RESOURCES.get(name);
        if (tempResource != null) {
            return new ByteArrayInputStream(tempResource);
        }

        return MixinProxy.CLASSLOADER.getResourceAsStream(name);
    }

    @Override
    protected ILogger createLogger(String name) {
        return MixinProxy.DEBUG ? new LoggerAdapterConsole(name) : new LoggerAdapterDefault(name);
    }

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return MixinProxy.CLASSLOADER.loadClass(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, MixinProxy.CLASSLOADER);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, ClassLoader.getSystemClassLoader());
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
        byte[] bytes = MixinProxy.CLASSLOADER.getClassBytes(name, runTransformers);
        ClassReader classReader = new MixinClassReader(bytes, name);
        ClassNode node = new ClassNode();
        classReader.accept(node, readerFlags);
        return node;
    }
}
