package xyz.panyi.simpleplayer;

public class NativeBridge {
    static {
        System.loadLibrary("native-lib");
    }

    public static native String stringFromJNI();
}
