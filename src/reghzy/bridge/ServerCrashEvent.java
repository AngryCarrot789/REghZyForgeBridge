package reghzy.bridge;

import net.minecraft.crash.CrashReport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.event.Event;

public class ServerCrashEvent extends Event {
    public final MinecraftServer server;
    public final CrashReport report;

    public ServerCrashEvent() {
        this.report = null;
        this.server = MinecraftServer.func_71276_C();
    }

    public ServerCrashEvent(MinecraftServer server, CrashReport report) {
        this.server = server;
        this.report = report;
    }

    public DedicatedServer getServer() {
        return (DedicatedServer) this.server;
    }
}
