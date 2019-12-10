package zeusees.tracking;


import android.graphics.Rect;

import java.util.Arrays;

public class Face {
    public int id;

    // 人脸位置矩形框
    public int left;
    public int top;
    public int right;
    public int bottom;
    public int height;
    public int width;

    // 标记，长度212，即106个点
    public int[] landmarks;

    public Face(int id, int[] faceRect, int[] landmarks, int[] attributes, float[] eulerAngles) {
        this.id = id;
        this.left = faceRect[0];
        this.top = faceRect[1];
        this.right = faceRect[2];
        this.bottom = faceRect[3];
        this.width = faceRect[2] - faceRect[0];
        this.height = faceRect[3] - faceRect[1];
        this.landmarks = landmarks;
    }

    @Override
    public String toString() {
        return "Face{" +
                "id=" + id +
                ", left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", height=" + height +
                ", width=" + width +
                ", landmarks=" + Arrays.toString(landmarks) +
                '}';
    }
}
