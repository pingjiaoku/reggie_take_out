package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;


    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        log.info("用户下单：{}", orders);

        ordersService.submit(orders);

        return R.success("下单成功");

    }

    @GetMapping("/userPage")
    public R<Page<OrdersDto>> getOrders(Integer page, Integer pageSize) {
        log.info("用户订单查询 page = {}, pageSize = {}", page, pageSize);

        Page<OrdersDto> ordersDtoPage = ordersService.listWithOrderDetail(page, pageSize);

        return R.success(ordersDtoPage);

    }

    @GetMapping("/page")
    public R<Page<Orders>> getBackendOrders(OrdersDto ordersDto) {
        log.info("服务端查询订单列表：page = {}， pagesize = {}, number = {}, beginTime = {}, endTime = {}",
                ordersDto.getPage(), ordersDto.getPageSize(), ordersDto.getNumber(), ordersDto.getBeginTime(), ordersDto.getEndTime());

        Page<Orders> page = new Page<>(ordersDto.getPage(), ordersDto.getPageSize());

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ordersDto.getNumber() != null, Orders::getNumber, ordersDto.getNumber())
                .ge(ordersDto.getBeginTime() != null, Orders::getOrderTime, ordersDto.getBeginTime())
                .le(ordersDto.getEndTime() != null, Orders::getOrderTime, ordersDto.getEndTime())
                .orderByDesc(Orders::getOrderTime);

        ordersService.page(page, queryWrapper);

        return R.success(page);
    }

    @PutMapping
    public R<String> updateStatus(@RequestBody Orders orders) {
        log.info("修改订单状态：{}", orders);

        ordersService.updateById(orders);

        return R.success("修改成功");
    }

    @PostMapping("/again")
    @Transactional
    public R<String> again(@RequestBody Orders orders) {
        log.info("再来一单：{}", orders.getId());

        // 获取订单信息
        Orders order = ordersService.getById(orders.getId());

        // 获取详细订单
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orders.getId());
        List<OrderDetail> list = orderDetailService.list(queryWrapper);

        long id1 = IdWorker.getId();
        order.setId(id1);
        order.setNumber(String.valueOf(id1));
        order.setStatus(2);
        order.setOrderTime(LocalDateTime.now());
        order.setCheckoutTime(LocalDateTime.now());

        list = list.stream().peek((item) -> {
            item.setOrderId(id1);
            item.setId(null);
        }).collect(Collectors.toList());

        // 保存订单
        ordersService.save(order);

        // 保存详细订单
        orderDetailService.saveBatch(list);

        return R.success("再来一单成功");

    }

}
