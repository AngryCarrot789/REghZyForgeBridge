# REghZyForgeBridge
A simple mod that allows bukkit plugins to listen to forge events

It simply replicates the event bus' register() method, using a custom IEventHandler, instead of the ASMHandler. the ASM one won't be able to find classes 
that weren't loaded by a specific classloader, which i image to be the LauncClassLoader or some sort of mod classloader.

However, my one simply creates a wrapper around Method#invoke(), which works fine
