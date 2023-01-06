package com.itheima;


import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.entity.ShoppingCart;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Unit test for simple App.
 */

//@SpringBootTest
public class AppTest {


    @Test
    public void method() {
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setNumber(213);
        BigDecimal bigDecimal = new BigDecimal(shoppingCart.getNumber());
    }

    public String nullToString(String str) {
        return str == null ? "" : str;
    }

}
