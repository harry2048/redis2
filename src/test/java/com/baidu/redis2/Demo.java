package com.baidu.redis2;

import java.util.HashMap;
import java.util.Map;

public class Demo {
    public static void main1(String[] args) {
        Map<String, Object> map = new HashMap<>();
        String name = (String) map.get("name");
        if (null == name || "".equals(name)) {
            name = "小花";
        }
        System.out.println(name);
    }

    public static void main2(String[] args) {
        String s1 = "abc";
        String s2 = "abc";
        String s3 = new String("abc");
        String s4 = s3.intern();

        System.out.println(s1 == s2);// t
        System.out.println(s1 == s3);// f
        System.out.println(s1 == s4);// t
    }

    public static void main(String[] args) {
        System.out.println("this is stash 1");
    }
}
