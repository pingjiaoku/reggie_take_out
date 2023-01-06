package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;


    /**
     * 员工登录
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpSession session, @RequestBody Employee employee) {

        // 1.将页面提交的密码进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 2.根据页面提交的用户名查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        queryWrapper.eq(Employee::getPassword, password);
        Employee emp = employeeService.getOne(queryWrapper);

        if (emp == null) {
            return R.error("登陆失败");
        }

        if (emp.getStatus() == 0) {
            return R.error("账号已禁用");
        }

        // 登录成功，将员工id存入session并返回成功结果
        session.setAttribute("employee", emp.getId());

        // 删除客户端session
        session.removeAttribute("user");

        return R.success(emp);
    }

    /**
     * 员工退出
     * @param request
     * @return
     */
    @RequestMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    /**
     * 添加员工
     * @param employee
     * @return
     */
    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {
        log.info("新增员工，员工信息： {}", employee.toString());

        // 设置初始密码123456，需要进行md5加密处理
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//
//        // 获得当前登录用户的id
//        Long empId = (Long) request.getSession().getAttribute("employee");
//
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);

        employeeService.save(employee);

        return R.success("新增员工成功");

    }

    /**
     * 员工信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<Employee>> getAll(Integer page, Integer pageSize, String name){
        log.info("page = {}, pageSize = {}, name = {}", page, pageSize, name);

        // 构造分页构造器
        Page<Employee> pageInfo = new Page<>(page, pageSize);

        // 构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();

        // 添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);

        // 添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        // 执行查询
        employeeService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);

    }

    /**
     * 根据id修改员工信息
     * @param request
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee){
        log.info(employee.toString());

//        employee.setUpdateUser((Long) request.getSession().getAttribute("employee"));
//        employee.setUpdateTime(LocalDateTime.now());

        employeeService.updateById(employee);

        return R.success("员工修改成功");
    }


    @GetMapping("/{empId}")
    public R<Employee> getById(@PathVariable Long empId) {
        log.info("查询员工信息：{}", empId);

        Employee employee = employeeService.getById(empId);
        return R.success(employee);
    }
}
