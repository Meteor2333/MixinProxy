package cc.meteormc.mixinproxy.service;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Blackboard implements IGlobalPropertyService {
    private static final Map<String, Object> BLACKBOARD = new ConcurrentHashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    @Override
    public <T> T getProperty(IPropertyKey key) {
        return this.getProperty(key, null);
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        Blackboard.BLACKBOARD.put(key.toString(), value);
    }

    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        //noinspection unchecked
        return (T) Blackboard.BLACKBOARD.getOrDefault(key.toString(), defaultValue);
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        return this.getProperty(key, defaultValue);
    }

    private static class Key implements IPropertyKey {
        private final String key;

        Key(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return this.key;
        }
    }
}
