package com.itheima.reggie.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrdersMapper;
import com.itheima.reggie.service.*;
import com.itheima.reggie.utils.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     * @param orders
     */
    @Override
    @Transactional
    public void submit(Orders orders) {
        // 获取当前用户的id
        Long currentId = BaseContext.getCurrentId();

        // 查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);
        if (shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new CustomException("购物车为空，不能下单");
        }

        // 查询当前用户
        User user = userService.getById(currentId);

        long orderId = IdWorker.getId(); // 雪花算法生成id

        // 用线程安全的整数计算总金额
        AtomicInteger amount = new AtomicInteger(0);

        // 获取订单详细数据
        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(item, orderDetail, "id", "order_id");
            orderDetail.setOrderId(orderId);
            // 计算总金额
            BigDecimal amount1 = item.getAmount();
            BigDecimal bigDecimal = new BigDecimal(item.getNumber());
            BigDecimal multiply = amount1.multiply(bigDecimal);
            int i = multiply.intValue();
            amount.addAndGet(i);
            return orderDetail;
        }).collect(Collectors.toList());

        // 查询当前用户默认地址
        LambdaQueryWrapper<AddressBook> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(AddressBook::getUserId, currentId)
                        .eq(AddressBook::getIsDefault, 1);
        AddressBook addressBook = addressBookService.getOne(queryWrapper1);

        // 设置数据
        // 设置id
        orders.setId(orderId);
        // 设置订单号
        orders.setNumber(String.valueOf(orderId));
        // 设置状态为已付款（待派送）
        orders.setStatus(2);
        // 设置用户id
        orders.setUserId(currentId);
        // 设置用户名
        orders.setUserName(user.getName());
        // 下单时间
        orders.setOrderTime(LocalDateTime.now());
        // 结账时间
        orders.setCheckoutTime(LocalDateTime.now());
        // 设置总金额
        orders.setAmount(new BigDecimal(amount.get()));
        // 设置电话
        orders.setPhone(addressBook.getPhone());
        // 设置地址
        String address = StringUtils.nullToString(addressBook.getProvinceName())
                + StringUtils.nullToString(addressBook.getCityName())
                + StringUtils.nullToString(addressBook.getDistrictName())
                + StringUtils.nullToString(addressBook.getDetail());
        orders.setAddress(address);
        // 设置用户名
        orders.setUserName(user.getName());
        // 设置收货人
        orders.setConsignee(addressBook.getConsignee());

        // 向订单表插入数据，一条数据
        this.save(orders);

        // 向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);

        // 清空购物车
        shoppingCartService.remove(queryWrapper);
    }

    /**
     * 查询订单with详细信息
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public Page<OrdersDto> listWithOrderDetail(Integer page, Integer pageSize) {
        Page<Orders> ordersPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId())
                .orderByDesc(Orders::getOrderTime);

        this.page(ordersPage, queryWrapper);

        Page<OrdersDto> ordersDtoPage = new Page<>();
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");

        List<OrdersDto> ordersDtoList = ordersPage.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);

            LambdaQueryWrapper<OrderDetail> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(OrderDetail::getOrderId, item.getId())
                    .orderByDesc(OrderDetail::getAmount);
            List<OrderDetail> list = orderDetailService.list(queryWrapper1);
            ordersDto.setOrderDetails(list);

            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(ordersDtoList);

        return ordersDtoPage;
    }


}
