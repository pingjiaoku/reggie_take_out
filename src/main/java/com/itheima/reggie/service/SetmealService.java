package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

public interface SetmealService extends IService<Setmeal> {
    // 同菜品一起保存套餐
    void saveWithDishes(SetmealDto setmealDto);

    // 同菜品一起保存
    void updateWithDishes(SetmealDto setmealDto);
}
