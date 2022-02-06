package reghzy.bridge.asm;

import com.google.common.collect.HashBiMap;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.IEventListener;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

public class ASMHandlerWrapper implements IEventListener {
    /**
     * keep track of how many ASMHandlers have been created, just in case the same method gets registered... for some reason...
     */
    private static int IDs = 0;

    /**
     * net/minecraftforge/event/IEventHandler
     */
    private static final String HANDLER_DESC = Type.getInternalName(IEventListener.class);

    /**
     * Lnet/minecraftforge/event/Event;)V
     * <p>
     *     parameter is Event, return type is void
     * </p>
     */
    private static final String HANDLER_FUNC_DESC = Type.getMethodDescriptor(IEventListener.class.getDeclaredMethods()[0]);

    private static final HashMap<Class<?>, ASMClassLoader> LOADER_MAP = new HashMap<Class<?>, ASMClassLoader>();

    private final IEventListener handler;
    private final ForgeSubscribe subInfo;

    public ASMHandlerWrapper(Object target, Method method) throws Exception {
        Object instance = createWrapper(method).getConstructor(Object.class).newInstance(target);
        this.handler = (IEventListener) instance;
        this.subInfo = method.getAnnotation(ForgeSubscribe.class);
    }

    @Override
    public void invoke(Event event) {
        if (event.isCancelable() && event.isCanceled() && !subInfo.receiveCanceled()) {
            return;
        }

        handler.invoke(event);
    }

    public EventPriority getPriority() {
        return subInfo.priority();
    }

    private static Class<?> createWrapper(Method method) {
        String name = getUniqueClassNameFor(method);
        String desc = name.replace('.', '/');
        String instType = Type.getInternalName(method.getDeclaringClass());
        String eventType = Type.getInternalName(method.getParameterTypes()[0]);

        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, "java/lang/Object", new String[]{HANDLER_DESC});

        // file name
        cw.visitSource(".dynamic", null);

        // create field
        cw.visitField(ACC_PUBLIC, "instance", "Ljava/lang/Object;", null, null).visitEnd();

        // constructor, taking in the class instance
        MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 1);
        ctor.visitFieldInsn(PUTFIELD, desc, "instance", "Ljava/lang/Object;");
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(2, 2);
        ctor.visitEnd();

        // invoke method
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, desc, "instance", "Ljava/lang/Object;");
        mv.visitTypeInsn(CHECKCAST, instType);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, eventType);
        mv.visitMethodInsn(INVOKEVIRTUAL, instType, method.getName(), Type.getMethodDescriptor(method));
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        cw.visitEnd();

        ASMClassLoader loader = LOADER_MAP.get(method.getDeclaringClass());
        if (loader == null) {
            LOADER_MAP.put(method.getDeclaringClass(), loader = new ASMClassLoader(method.getDeclaringClass().getClassLoader()));
        }

        return loader.defineClass(name, cw.toByteArray());
    }

    private static String getUniqueClassNameFor(Method callback) {
        StringBuilder sb = new StringBuilder();
        sb.append("RZASMHandler_");
        sb.append(callback.getDeclaringClass().getSimpleName()).append('_');
        sb.append(callback.getName()).append('_');
        sb.append(IDs++);
        return sb.toString();
    }

    private static class ASMClassLoader extends ClassLoader {
        public ASMClassLoader(ClassLoader classLoader) {
            super(classLoader);
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
