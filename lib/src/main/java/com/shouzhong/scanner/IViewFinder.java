package com.shouzhong.scanner;

import android.graphics.Rect;

/**
 * 识别框
 *
 */
public interface IViewFinder {

    /**
     * 获得扫码区域(识别区域)
     */
    Rect getFramingRect();
}