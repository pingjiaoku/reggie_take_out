package com.itheima.reggie.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    @Override
    @Transactional
    public void saveWithDishes(SetmealDto setmealDto) {
        // 保存套餐
        this.save(setmealDto);

        // 获取套餐中的菜品，并赋值套餐id
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().peek((item) -> item.setSetmealId(setmealDto.getId()))
                .collect(Collectors.toList());

        // 保存套餐菜品
        setmealDishService.saveBatch(setmealDishes);

    }

    @Override
    @Transactional
    public void updateWithDishes(SetmealDto setmealDto) {
        // 修改套餐
        this.updateById(setmealDto);

        // 删除套餐的菜品
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        List<SetmealDish> collect = setmealDto.getSetmealDishes().stream().peek((item) ->
                item.setSetmealId(setmealDto.getId())).collect(Collectors.toList());

        // 添加新的菜品
        setmealDishService.saveBatch(collect);
    }
}
