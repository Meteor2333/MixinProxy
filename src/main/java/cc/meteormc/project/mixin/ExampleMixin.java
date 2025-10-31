package cc.meteormc.project.mixin;

import cc.meteormc.project.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// An example mixin class
@Mixin(Main.class)
public class ExampleMixin {
    @Inject(
            method = {"main"},
            at = {@At("HEAD")},
            remap = false
    )
    private static void onMain(CallbackInfo info) {
        System.out.println("Main method has been called!");
    }
}
