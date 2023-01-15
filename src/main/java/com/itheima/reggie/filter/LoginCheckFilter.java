package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录
 */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // 获取本次请求的uri
        String requestURI = request.getRequestURI();

        // 定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
//                "/backend/page/login/login.html",
//                "/front/page/login.html",
                "/user/sendMsg", // 移动端发送短信
                "/user/login" // 移动端登录
        };

        // 定义客户端登录后可访问的请求
        String[] userAllows = new String[]{
                "/front/**"
        };

        // 定义服务端登陆后可访问的请求
        String[] empAllows = new String[]{
                "/backend/**"
        };

        // 判断本次请求是否需要处理
        boolean check = check(urls, requestURI);

        // 如果不需要处理，则直接放行
        if (check) {
            filterChain.doFilter(request, response);
            return;
        }

        // 判断服务端登录状态，如果已登录，则直接放行，
        if(request.getSession().getAttribute("employee") != null) {
//            if (check(userAllows, requestURI)) {
//                return;
//            }

            // 获取用户id，将id存入线程域中，作用于元数据对象处理器
            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request, response);
            return;
        }

        // 判断客服端登录状态，如果已登录，则直接放行，
        if(request.getSession().getAttribute("user") != null) {
            // 若登陆的是用户想要访问服务端页面，拒绝
//            if (check(empAllows, requestURI)) {
//                return;
//            }

            // 获取用户id，将id存入线程域中，作用于元数据对象处理器
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request, response);
            return;
        }

        // 如果未登录则返回未登录结果
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        log.info("拦截到请求：{}", requestURI);

    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match) {
                return true;
            }
        }
        return false;
    }
}
