package com.example.floatoverlay;

public final class MemoryReader {
    static {
        System.loadLibrary("memoryreader");
    }

    private MemoryReader() {
    }

    public static native float readFloat(long address);

    public static native long createSample(float value);
}
