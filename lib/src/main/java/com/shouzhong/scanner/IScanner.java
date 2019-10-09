package com.shouzhong.scanner;

public interface IScanner {

    public Result scan(byte[] data, int width, int height) throws Exception;

}
