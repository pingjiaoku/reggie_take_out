package com.itheima;

public class Main {
    public static void main(String[] args) {
        AppTest appTest = new AppTest();

        appTest.setId(1234);
        appTest.setName("东啊東");
        appTest.setAge(43);

        Test<AppTest> testTest = new Test<>(appTest);
        System.out.println(testTest.toString());
    }
}
