package customskinloader.forge.transformer;

import java.util.ListIterator;

import customskinloader.forge.TransformerManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Transformer of Spectator Menu for 1.13-
 */
public class SpectatorMenuTransformer {
    @TransformerManager.TransformTarget(className = "net.minecraft.client.gui.spectator.PlayerMenuObject",
            methodNameSrg = "<init>",
            desc = "(Lcom/mojang/authlib/GameProfile;)V")
    public static class PlayerMenuObjectTransformer implements TransformerManager.IMethodTransformer {

        @Override
        public MethodNode transform(ClassNode cn, MethodNode mn) {
            InsnList il = mn.instructions;
            ListIterator<AbstractInsnNode> li = il.iterator();

            boolean flag = false;
            while (li.hasNext()) {
                AbstractInsnNode ain = li.next();
                if (ain.getOpcode() != Opcodes.INVOKESTATIC)
                    continue;

                if (!flag) {
                    //First InvokeStatic
                    il.set(ain, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "customskinloader/fake/FakeClientPlayer",
                            "getLocationSkin",
                            "(Ljava/lang/String;)Lnet/minecraft/util/ResourceLocation;", false));
                    flag = true;
                } else {
                    //Second InvokeStatic
                    il.set(ain, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "customskinloader/fake/FakeClientPlayer",
                            "getDownloadImageSkin",
                            "(Lnet/minecraft/util/ResourceLocation;Ljava/lang/String;)Lnet/minecraft/client/renderer/ThreadDownloadImageData;", false));
                    break;
                }
            }
            return mn;
        }

    }
}
