package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 添加菜品分类
     * @param category
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody Category category) {
        log.info("添加的分类为：{}", category);
        categoryService.save(category);

        return R.success("添加成功");
    }

    @GetMapping("/page")
    public R<Page<Category>> getAll(Integer page, Integer pageSize) {
        log.info("菜品、套餐分类分页查询：page = {}，pageSize = {}", page, pageSize);

        // 创建分页构造器
        Page<Category> categoryPage = new Page<>(page, pageSize);

        // 构造条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();

        // 添加排序条件
        queryWrapper.orderByAsc(Category::getType)
                .orderByAsc(Category::getName);

        // 执行查询
        categoryService.page(categoryPage, queryWrapper);

        return R.success(categoryPage);
    }

    @PutMapping
    public R<String> update(@RequestBody Category category) {
        log.info("修改菜品、套餐分类：{}", category );

        boolean flag = categoryService.updateById(category);

        return flag ? R.success("修改成功") : R.error("修改失败");
    }

    @DeleteMapping
    public R<String> update(Long ids) {
        log.info("删除分类: ids = {}", ids);

        categoryService.remove(ids);

        return R.success("删除成功");
    }

    /**
     * 根据条件查询分类数据
     * @param category
     * @return
     */
    @GetMapping("/list")
    public R<List<Category>> list(Category category) {
        // 条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();

        // 查询指定列
        queryWrapper.select(Category::getId, Category::getName, Category::getType);

        // 添加条件
        queryWrapper.eq(category.getType() != null, Category::getType, category.getType());

        // 添加排序条件
        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

        List<Category> list = categoryService.list(queryWrapper);

        return R.success(list);
    }

}
