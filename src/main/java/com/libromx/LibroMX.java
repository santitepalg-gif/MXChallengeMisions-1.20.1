package com.libromx;

import com.libromx.net.Net;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LibroMX.MODID)
public class LibroMX {
    public static final String MODID = "libromx";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LibroMX() {
        Net.init();
    }
}
