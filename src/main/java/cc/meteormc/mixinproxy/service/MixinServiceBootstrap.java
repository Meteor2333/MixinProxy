package cc.meteormc.mixinproxy.service;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class MixinServiceBootstrap implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return "MixinServiceBootstrap";
    }

    @Override
    public String getServiceClassName() {
        return MixinService.class.getName();
    }

    @Override
    public void bootstrap() {

    }
}
