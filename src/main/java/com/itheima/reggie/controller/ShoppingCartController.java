package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        log.info("查询购物车列表");

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId())
                .orderByDesc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("添加购物车: {}", shoppingCart);

        // 获取当前用户id
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        // 查询当前菜品或者套餐是否在购物车中
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(currentId != null, ShoppingCart::getUserId, currentId);

        if (dishId != null) {
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        } else  {
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
        }
        ShoppingCart one = shoppingCartService.getOne(queryWrapper);

        if (one == null) {
            // 购物车中不存在该菜品或者套餐
            // 保存
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
            return R.success(shoppingCart);
        } else {
            // 存在，加1
            one.setNumber(one.getNumber() + 1);
            shoppingCartService.updateById(one);
            return R.success(one);
        }
    }

    @PostMapping("/sub")
    public R sub(@RequestBody SetmealDish setmealDish) {
        log.info("菜品数量减1: {}", setmealDish);

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        if (setmealDish.getDishId() != null) {
            queryWrapper.eq(ShoppingCart::getDishId, setmealDish.getDishId());
        } else {
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealDish.getSetmealId());
        }

        // 查询当前菜品或者套餐的数量
        ShoppingCart one = shoppingCartService.getOne(queryWrapper);
        if (one.getNumber() == 1) {
            // 若只有一个则删除
            shoppingCartService.removeById(one.getId());
            return R.success("删除成功");
        } else {
            // 否则减一
            one.setNumber(one.getNumber() - 1);
            shoppingCartService.updateById(one);
        }

        return R.success(one);

    }

    @DeleteMapping("/clean")
    public R<String> clearCart() {
        log.info("清空购物车");

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        shoppingCartService.remove(queryWrapper);

        return R.success("清空成功");
    }
}
