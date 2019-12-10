package pr.platerecognization;

public class PlateRecognition {

    static {
        System.loadLibrary("hyperlpr");
    }

    public static native long InitPlateRecognizer(String paramString1, String paramString2, String paramString3);

    public static native long ReleasePlateRecognizer(long paramLong);

    public static native String RunRecognition(long paramLong1, long paramLong2, float paramFloat, int paramInt1, int paramInt2, int paramInt3);

    public static native String RunRecognitionAsRaw(byte[] paramArrayOfByte, int paramInt1, int paramInt2, long paramLong, float paramFloat, int paramInt3, int paramInt4, int paramInt5);

}
