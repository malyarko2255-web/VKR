package ru.finuniversity.advance.common.util;

import java.time.Year;

public final class RequestNoGenerator {

    private RequestNoGenerator() {}

    public static String generate(long sequence) {
        return "A-" + Year.now().getValue() + "-" + String.format("%05d", sequence);
    }
}
