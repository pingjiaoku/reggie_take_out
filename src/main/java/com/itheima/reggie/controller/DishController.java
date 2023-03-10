package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${reggie.path}")
    private String filePath;

    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info("保存菜品：{}", dishDto);

        dishService.saveWithFlavor(dishDto);

        // 清理指定分类下的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("添加菜品成功");
    }

    @GetMapping("/page")
    public R<Page<DishDto>> getAll(Integer page, Integer pageSize, String name) {
        log.info("查看菜品列表：page = {}, pageSize = {}, name = {}", page, pageSize, name);

        // 创建分页构造器
        Page<Dish> dishPage = new Page<>(page, pageSize);

        // 创建条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        // 排序条件
        queryWrapper.like(name != null, Dish::getName, name)
                .orderByAsc(Dish::getCategoryId)
                .orderByAsc(Dish::getName);

        // 执行查询
        dishService.page(dishPage, queryWrapper);

        Page<DishDto> dishDtoPage = new Page<>();
        BeanUtils.copyProperties(dishPage, dishDtoPage, "records");

        List<Dish> records = dishPage.getRecords();

        List<DishDto> dishDtoList = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            LambdaQueryWrapper<Category> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.select(Category::getName)
                    .eq(Category::getId, dishDto.getCategoryId());
            Category one = categoryService.getOne(queryWrapper1);

            dishDto.setCategoryName(one.getName());

            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(dishDtoList);

        return R.success(dishDtoPage);
    }

    @GetMapping("/{id}")
    public R<DishDto> getById(@PathVariable Long id) {
        log.info("根据id查询菜品：{}", id);

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info("修改菜品：{}", dishDto);

        dishService.updateWithFlavor(dishDto);

        // 清理所有菜品的缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);

        // 清理指定分类下的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("修改菜品成功");
    }

    @DeleteMapping
    @Transactional
    public R<String> deleteById(@RequestParam("ids") List<Long> ids) {
        log.info("删除菜品：{}", ids);

        // 获取菜品的图片名称
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Dish::getImage, Dish::getCategoryId)
                .in(Dish::getId, ids);
        List<Dish> list = dishService.list(queryWrapper);

        // 删除菜品
        LambdaQueryWrapper<Dish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(Dish::getId, ids);
        dishService.remove(queryWrapper1);

        // 删除菜品对应的口味
        LambdaQueryWrapper<DishFlavor> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(queryWrapper2);

        // 删除图片
        for (Dish dish : list) {
            File file = new File(filePath + dish.getImage());
            log.info("删除图片：path = {}", file.getPath());
            if (file.exists()) {
                if (!file.delete()) {
                    log.error("删除图片失败，path = {}", file.getPath());
                }
            }
        }

        // 将菜品的分类放在set中
        Set<Long> collect = list.stream().map(Dish::getCategoryId).collect(Collectors.toSet());
        // 清除对应分类的缓存
        redisTemplate.delete(collect);

        return R.success("删除菜品成功");
    }

    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam("ids") List<Long> ids) {
        log.info("修改菜品状态");

        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Dish::getStatus, status)
                .in(Dish::getId, ids);
        dishService.update(updateWrapper);

        return R.success("修改成功");
    }

    @GetMapping("/list")
    public R<List<DishDto>> getList(Long categoryId, Integer status, String name) {
        log.info("通过id或者name查询菜品：id = {}, status = {}, name = {}", categoryId, status, name);

        List<DishDto> dishList;

        // 动态构造key
        String key = "dish_" + categoryId + "_" + status;

        // 从redis中获取缓存数据
        dishList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        // 若存在，则直接返回数据
        if (dishList != null) {
            return R.success(dishList);
        }

        dishList = dishService.listWithDishFlavor(categoryId, status, name);

        // 若不存在，则查询数据库，并缓存到redis中
        redisTemplate.opsForValue().set(key, dishList, 60, TimeUnit.MINUTES);

        return R.success(dishList);
    }

}
