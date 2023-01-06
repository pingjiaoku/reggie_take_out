package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.Orders;

public interface OrdersService extends IService<Orders> {
    void submit(Orders orders);

    Page<OrdersDto> listWithOrderDetail(Integer page, Integer pageSize);
}
