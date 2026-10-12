package com.example.shop.util;

import sun.misc.BASE64Encoder;

public final class LegacyEncoder {
    private LegacyEncoder() {}

    public static String encode(byte[] data) {
        return new BASE64Encoder().encode(data);
    }
}
