package zeusees.tracking;

public class FaceTracking {
    static {
        System.loadLibrary("zeuseesTracking-lib");
    }

    public static native long createSession(String paramString);

    public static native int[] getAttributeByIndex(int paramInt, long paramLong);

    public static native float[] getEulerAngleByIndex(int paramInt, long paramLong);

    public static native int getTrackingIDByIndex(int paramInt, long paramLong);

    public static native int[] getTrackingLandmarkByIndex(int paramInt, long paramLong);

    public static native int[] getTrackingLocationByIndex(int paramInt, long paramLong);

    public static native int getTrackingNum(long paramLong);

    public static native void initTracking(byte[] paramArrayOfByte, int paramInt1, int paramInt2, long paramLong);

    public static native void releaseSession(long paramLong);

    public static native void update(byte[] paramArrayOfByte, int paramInt1, int paramInt2, long paramLong);
}
