package com.itheima.reggie.service.Impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类，删除前检查分类下是否有菜品
     *
     * @param id
     * @return
     */
    @Override
    public void remove(Long id) {

        // 检查是否有菜品
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, id);

        int count1 = dishService.count(dishLambdaQueryWrapper);
        if (count1 > 0) {
            // 已经关联菜品，抛出异常
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }

        // 检查是否有套餐
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, id);

        int count2 = setmealService.count(setmealLambdaQueryWrapper);
        if (count2 > 0) {
            // 已经关联套餐，抛出异常
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }

        // 正常删除分类
        super.removeById(id);
    }
}
