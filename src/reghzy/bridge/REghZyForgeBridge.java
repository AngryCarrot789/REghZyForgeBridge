package reghzy.bridge;

import com.google.common.reflect.TypeToken;
import com.sun.xml.internal.ws.client.sei.MethodHandler;
import cpw.mods.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.EventBus;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.IEventListener;
import net.minecraftforge.event.ListenerList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Mod(modid = "reghzybridge", name = "REghZyForgeBridge", version = "1.1.0")
public class REghZyForgeBridge {
    public REghZyForgeBridge() {
        Logger.getLogger("Minecraft").info("----------------------------------------");
        Logger.getLogger("Minecraft").info("REghZyForgeBridge - Class CTOR :)))))))))");
        Logger.getLogger("Minecraft").info("----------------------------------------");
    }

    public static void register(EventBus bus, Object target) {
        Map<Object, ArrayList<IEventListener>> listeners = Reflect.getBusListeners(bus);
        if (listeners.containsKey(target)) {
            return;
        }

        int busId = Reflect.getBusID(bus);
        for (Method method : target.getClass().getMethods()) {
            for (Class<?> clazz : TypeToken.of(target.getClass()).getTypes().rawTypes()) {
                Class<?>[] parameters = method.getParameterTypes();
                Method real;
                try {
                    if (parameters.length == 0) {
                        real = clazz.getDeclaredMethod(method.getName());
                    }
                    else {
                        real = clazz.getDeclaredMethod(method.getName(), parameters);
                    }
                }
                catch (NoSuchMethodException e) {
                    continue;
                }

                if (real.isAnnotationPresent(ForgeSubscribe.class)) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length != 1) {
                        throw new IllegalArgumentException("Method '" + method + "' has @ForgeSubscribe annotation, but has " + parameterTypes.length + " arguments.  Event handler methods must require a single argument.");
                    }

                    Class<?> eventType = parameterTypes[0];
                    if (!Event.class.isAssignableFrom(eventType)) {
                        throw new IllegalArgumentException("Method '" + method + "' has @ForgeSubscribe annotation, but takes a argument that is not a Event " + eventType);
                    }

                    try {
                        Constructor<?> ctor = eventType.getConstructor();
                        if (!ctor.isAccessible()) {
                            ctor.setAccessible(true);
                        }

                        Event event = (Event) ctor.newInstance();
                        MethodHandler listener = new MethodHandler(target, method);
                        event.getListenerList().register(busId, listener.getPriority(), listener);
                        ArrayList<IEventListener> handlers = listeners.get(target);
                        if (handlers == null) {
                            listeners.put(target, handlers = new ArrayList<IEventListener>());
                        }

                        handlers.add(listener);
                    }
                    catch (Throwable e) {
                        throw new RuntimeException("Failed to register event handler", e);
                    }

                    break;
                }
            }
        }
    }

    /**
     * Registers all methods, in the given instance, that are annotated with {@link ForgeSubscribe}, with the normal {@link MinecraftForge#EVENT_BUS}
     * @param target The target object, containing event handler methods
     */
    public static void register(Object target) {
        register(MinecraftForge.EVENT_BUS, target);
    }

    /**
     * Registers all methods, in the given instance, that are annotated with {@link ForgeSubscribe}, with the {@link MinecraftForge#TERRAIN_GEN_BUS}
     * @param target The target object, containing event handler methods
     */
    public static void registerTerrain(Object target) {
        register(MinecraftForge.TERRAIN_GEN_BUS, target);
    }

    /**
     * Registers all methods, in the given instance, that are annotated with {@link ForgeSubscribe}, with the {@link MinecraftForge#ORE_GEN_BUS}
     * @param target The target object, containing event handler methods
     */
    public static void registerOreGen(Object target) {
        register(MinecraftForge.ORE_GEN_BUS, target);
    }

    /**
     * Unregisters all methods, in the given target instance, that are annotated with {@link ForgeSubscribe}, for the given bus
     * @param bus The bus to unregister from
     * @param target The target to unregister the methods of
     */
    public static void unregister(EventBus bus, Object target) {
        Map<Object, ArrayList<IEventListener>> listeners = Reflect.getBusListeners(bus);
        if (listeners.containsKey(target)) {
            return;
        }

        int id = Reflect.getBusID(bus);
        for (IEventListener listener : listeners.remove(target)) {
            ListenerList.unregiterAll(id, listener);
        }
    }

    /**
     * Unregisters all methods, in the given target instance, that are annotated with {@link ForgeSubscribe}, for the normal {@link MinecraftForge#EVENT_BUS}
     * @param target The target to unregister the methods of
     */
    public static void unregister(Object target) {
        unregister(MinecraftForge.EVENT_BUS, target);
    }

    /**
     * Unregisters all methods, in the given target instance, that are annotated with {@link ForgeSubscribe}, for the {@link MinecraftForge#TERRAIN_GEN_BUS}
     * @param target The target to unregister the methods of
     */
    public static void unregisterTerrain(Object target) {
        unregister(MinecraftForge.TERRAIN_GEN_BUS, target);
    }

    /**
     * Unregisters all methods, in the given target instance, that are annotated with {@link ForgeSubscribe}, for the {@link MinecraftForge#ORE_GEN_BUS}
     * @param target The target to unregister the methods of
     */
    public static void unregisterOreGen(Object target) {
        unregister(MinecraftForge.ORE_GEN_BUS, target);
    }

    /**
     * Normal forge uses an ASMEventHandler, but the ASM handler doesn't work when the handler's
     * class wasn't loaded by the class that loaded the ASM handler. E.g, if a plugin wants to listen
     * to forge events, it would throw a "NoClassDefError: my.plugin.ClassWithForgeSubscribedMethods"
     * <p>
     *     I assume that's something to do with ASM, considering it's literally creating a class, so
     *     there's probably some dodgy security protocols or class loader settings that wont work right
     * </p>
     */
    private static class MethodHandler implements IEventListener {
        public final Object target;
        public final Method method;
        public final ForgeSubscribe subInfo;

        public MethodHandler(Object target, Class<?> clazz, String methodName) {
            this.target = target;

            try {
                this.method = clazz.getDeclaredMethod(methodName, Event.class);
            }
            catch (NoSuchMethodException e) {
                throw new RuntimeException("The method doesn't take an event instance as a parameter", e);
            }

            this.subInfo = this.method.getAnnotation(ForgeSubscribe.class);
        }

        public MethodHandler(Object target, Method method) {
            this.target = target;
            this.method = method;
            this.subInfo = method.getAnnotation(ForgeSubscribe.class);
        }

        public EventPriority getPriority() {
            return this.subInfo.priority();
        }

        @Override
        public void invoke(Event event) {
            try {
                this.method.invoke(this.target, event);
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException("Illegal access while invoking event handler method", e);
            }
            catch (InvocationTargetException e) {
                throw new RuntimeException("Unhandled exception while invoking event handler of type: " + event.getClass().getName(), e);
            }
        }
    }

    private static class Reflect {
        private static final Field BUS_ID_FIELD;
        private static final Field BUS_LISTENERS_FIELD;

        public static int getBusID(EventBus bus) {
            try {
                return BUS_ID_FIELD.getInt(bus);
            }
            catch (IllegalAccessException e) {
                throw new Error("IllegalAccessException while getting event bus ID", e);
            }
        }

        public static ConcurrentHashMap<Object, ArrayList<IEventListener>> getBusListeners(EventBus bus) {
            try {
                return (ConcurrentHashMap<Object, ArrayList<IEventListener>>) BUS_LISTENERS_FIELD.get(bus);
            }
            catch (IllegalAccessException e) {
                throw new Error("IllegalAccessException while getting event bus listeners", e);
            }
        }

        static {
            try {
                BUS_ID_FIELD = EventBus.class.getDeclaredField("busID");
                BUS_ID_FIELD.setAccessible(true);
            }
            catch (NoSuchFieldException e) {
                throw new RuntimeException("No such event bus field: busID", e);
            }

            try {
                BUS_LISTENERS_FIELD = EventBus.class.getDeclaredField("listeners");
                BUS_LISTENERS_FIELD.setAccessible(true);
            }
            catch (NoSuchFieldException e) {
                throw new RuntimeException("No such event bus field: listeners", e);
            }
        }
    }
}
