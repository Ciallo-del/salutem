package com.lframework.xingyun.core.handler;

import com.lframework.starter.common.exceptions.impl.AuthExpiredException;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;

/**
 * 统一处理登录失效异常，避免落入 starter 默认异常页分支。
 */
@RestControllerAdvice(basePackages = "com.lframework.xingyun")
public class AuthExpiredExceptionHandler {

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthExpiredException.class)
    public InvokeResult<Void> handleAuthExpiredException(AuthExpiredException ex, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        return InvokeResultBuilder.fail("请重新登录！");
    }
}
