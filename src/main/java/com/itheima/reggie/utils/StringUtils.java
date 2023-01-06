package com.itheima.reggie.utils;

public class StringUtils extends org.apache.commons.lang.StringUtils {
    /**
     * 将null转为空字符串
     * @param str
     * @return
     */
    public static String nullToString(String str) {
        return str == null ? "" : str;
    }
}
