package reghzy.bridge;

import net.minecraft.crash.CrashReport;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.EventBus;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import universalelectricity.core.asm.ObfMapping;

import javax.management.InstanceNotFoundException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class CrashEventTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (transformedName.equals("net.minecraft.server.dedicated.DedicatedServer")) {
            System.out.println("[CarrotTools] Transforming MinecraftServer for crash event...");

            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(bytes);
            reader.accept(node, 0);

            MethodNode finalTickMethod = null;
            for (MethodNode method : ((List<MethodNode>) node.methods)) {
                ObfMapping mapping = new ObfMapping(node.name, method.name, method.desc).toRuntime();
                if (mapping.s_name.equals("func_71228_a") || mapping.s_name.equals("finalTick")) {
                    finalTickMethod = method;
                    break;
                }
            }

            if (finalTickMethod == null) {
                System.err.println("[CarrotTools] Failed to find func_71228_a/finalTick!");
                return bytes;
            }

            InsnList list = new InsnList();
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new VarInsnNode(Opcodes.ALOAD, 1));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "reghzy/bridge/REghZyForgeBridge", "onServerCrash", "(Lnet/minecraft/server/dedicated/DedicatedServer;Lnet/minecraft/crash/CrashReport;)V"));
            list.add(new InsnNode(Opcodes.RETURN));
            finalTickMethod.instructions.insert(list);
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            return writer.toByteArray();
        }
        else {
            return bytes;
        }
    }
}
