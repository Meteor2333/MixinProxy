package cc.meteormc.project.mixin;

import cc.meteormc.project.target.TestClass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TestClass.class)
public class ExampleMixin {
    @Inject(
            method = {"resolve(II)I"},
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void onResolve(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(info.getReturnValue() * 10);
    }

    @Inject(
            method = "print()V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onPrint(CallbackInfo info) {
        info.cancel();
    }

    @Inject(
            method = "test()V",
            at = @At("HEAD"),
            remap = false
    )
    private static void onTest(CallbackInfo info) {
        System.out.println("Test method has been called!");
    }
}
