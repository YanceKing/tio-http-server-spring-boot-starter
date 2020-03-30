package com.yance.utils;

import com.alibaba.fastjson.JSONObject;

/**
 * @author yance
 */
public class StringTioUtil {

    /**
     * 判断是否为JSON字符串
     *
     * @param value 需要验证字符串
     * @return 返回真或假  bool类型
     */
    public static boolean isJsonString(String value) {
        try {
            JSONObject.parseObject(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
