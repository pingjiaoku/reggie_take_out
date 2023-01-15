package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostMapping("/sendMsg")
    private R<String> sendMsg(@RequestBody User user, HttpSession session) {

        if (StringUtils.isNotEmpty(user.getPhone())) {
            // 生成随机4位数验证码
            Random random = new Random();
            String code = random.nextInt(9000) + 1000 + "";
            log.info("发送验证码：phone = {}, code = {}", user.getPhone(), code);

            // 调用阿里云提供的短信服务api完成发送短信
//            SMSUtils.sendMessage("瑞吉外卖", "SMS_267050663", user.getPhone(), code);

            // 需要将生成的验证码保存到session中
//            session.setAttribute(user.getPhone(), code);

            // 将验证码存入redis中
            redisTemplate.opsForValue().set(user.getPhone(), code, 5, TimeUnit.MINUTES);

            return R.success("短信验证码发送成功");
        }

        return R.error("发送失败");

    }

    @PostMapping("/login")
    public R<String> login(@RequestBody Map map, HttpSession session) {
        log.info("用户登录：{}", map.toString());

        String phone = map.get("phone").toString();

        String code  = map.get("code").toString();

        // 从session中获取验证码
//        String sessionCode = (String) session.getAttribute(phone);

        // 从redis中获取验证码
        String sessionCode = redisTemplate.opsForValue().get(phone);

        if (code != null && code.equals(sessionCode)) {
            // 匹配成功
            // 删除验证码session
            session.removeAttribute(phone);
            // 判断数据库中是否存在该用户，如果不存在就自动完成注册
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);

            // 当用户不存在时，自动注册
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);

                userService.save(user);
            }

            session.setAttribute("user", user.getId());

            // 删除服务端session
            session.removeAttribute("employee");

            // 登录成功，删除redis中的验证码
            redisTemplate.delete(phone);

            return R.success("登录成功");
        }


        return R.error("登录失败");
    }

    @PostMapping("/loginout")
    public R<String> logout(HttpSession session) {
        log.info("用户登出：{}", BaseContext.getCurrentId());

        // 删除用户session
        session.removeAttribute("user");

        return R.success("成功登出");

    }


}
