package reghzy.bridge;

import calclavia.lib.asm.CalclaviaTransformer;
import calclavia.lib.config.ConfigTransformer;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class REghZyFMLPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getLibraryRequestClass() {
        return new String[0];
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{CrashEventTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> map) {
        ok(this, map);
        return;
    }

    public static void ok(REghZyFMLPlugin pl, Map<String, Object> map) {

    }
}
