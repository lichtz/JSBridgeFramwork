package com.xiaoying.h5core.api;

import java.util.List;

public interface H5PluginManager extends H5IntentTarget {

    public boolean register(H5PluginConfig pluginConfig);

    public boolean register(H5Plugin plugin);

    public boolean register(List<H5Plugin> plugins);

    public boolean unregister(H5Plugin plugin);

    public boolean unregister(List<H5Plugin> plugins);

    public boolean canHandle(String action);

}
