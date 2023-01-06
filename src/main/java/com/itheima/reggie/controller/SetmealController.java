package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.events.Event;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class    SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    @Value("${reggie.path}")
    private String filePath;

    @GetMapping("/page")
    public R<Page<SetmealDto>> getAll(Integer page, Integer pageSize, String name) {
        log.info("套餐分页查询，page = {}, pageSize = {}, name = {}", page, pageSize, name);

        // 构建分页构造器
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);

        // 构建条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();

        // 条件：：like '%name%'
        queryWrapper.like(name != null, Setmeal::getName, name);
        // 查询
        setmealService.page(setmealPage, queryWrapper);

        // 使用Dto，查询套餐分类名称
        Page<SetmealDto> setmealDtoPage = new Page<>();
        // 复制属性，排除数据
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");

        List<SetmealDto> collect = setmealPage.getRecords().stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);

            Long categoryId = setmealDto.getCategoryId();

            LambdaQueryWrapper<Category> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.select(Category::getName)
                    .eq(Category::getId, categoryId);

            Category one = categoryService.getOne(queryWrapper1);
            setmealDto.setCategoryName(one.getName());
            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(collect);

        return R.success(setmealDtoPage);
    }

    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("添加套餐：{}", setmealDto);

        setmealService.saveWithDishes(setmealDto);

        return R.success("添加成功");
    }

    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id) {
        log.info("通过id查询套餐：id = {}", id);

        // 查询套餐
        Setmeal setmeal = setmealService.getById(id);
        // 查询套餐中的菜品
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        setmealDto.setSetmealDishes(list);

        return R.success(setmealDto);
    }

    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info("修改套餐：{}", setmealDto);

        setmealService.updateWithDishes(setmealDto);

        return R.success("修改成功");
    }

    @PostMapping("/status/{status}")
    public R<String> updateStatus(@RequestParam("ids") List<Long> ids, @PathVariable Integer status) {
        log.info("批量起售停售：status = {}, ids = {}", status, ids.toString());

        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Setmeal::getStatus, status)
                .in(Setmeal::getId, ids);

        setmealService.update(updateWrapper);

        return R.success("修改成功");
    }

    @DeleteMapping
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        log.info("批量删除套餐：{}", ids.toString());

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Setmeal::getImage, Setmeal::getStatus).in(Setmeal::getId, ids);
        List<Setmeal> list = setmealService.list(queryWrapper);

        // 判断是不是停售中，是则可以删除，否则不能
        for (Setmeal setmeal : list) {
            if (setmeal.getStatus() == 1) {
                return R.error("删除失败，存在商品起售中");
            }
        }

        // 删除图片
        for (Setmeal setmeal : list) {
            File file = new File(filePath + setmeal.getImage());
            if (file.exists()) {
                file.delete();
            }
        }

        // 删除套餐
        setmealService.removeByIds(ids);

        // 删除菜品
        LambdaQueryWrapper<SetmealDish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(queryWrapper1);

        return R.success("删除成功");
    }

    @GetMapping("/list")
    public R<List<Setmeal>> list(Long categoryId, Integer status) {
        log.info("查询套餐：categoryId = {}， status = {}", categoryId, status);

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(categoryId != null, Setmeal::getCategoryId, categoryId)
                .eq(status != null, Setmeal::getStatus, status)
                .orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }

    @GetMapping("/dish/{id}")
    public R<List<DishDto>> getDishes(@PathVariable Long id) {
        log.info("获取套餐中的菜品");

        Setmeal setmeal = setmealService.getById(id);

        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        // 获取菜品中的基本信息
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id)
                .orderByDesc(SetmealDish::getSort);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        List<DishDto> collect = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            // 获取菜品图片、描述
            Long dishId = item.getDishId();
            Dish dish = dishService.getById(dishId);

            BeanUtils.copyProperties(dish, dishDto);

            return dishDto;
        }).collect(Collectors.toList());

        return R.success(collect);
    }

}
