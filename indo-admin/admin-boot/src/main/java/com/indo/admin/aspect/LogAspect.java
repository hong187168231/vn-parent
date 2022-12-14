
package com.indo.admin.aspect;

import com.indo.admin.modules.sys.service.ISysLogService;
import com.indo.admin.pojo.entity.SysLog;
import com.indo.common.utils.IPAddressUtil;
import com.indo.common.utils.RequestHolder;
import com.indo.common.utils.ThrowableUtil;
import com.indo.common.web.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author puff
 * @date 2021-08-24
 */
@Component
@Aspect
@Slf4j
public class LogAspect {

    @Resource
    private ISysLogService logService;

    ThreadLocal<Long> currentTime = new ThreadLocal<>();


    /**
     * 配置切入点
     */
    @Pointcut("@annotation(com.indo.common.annotation.Log)")
    public void logPointcut() {
        // 该方法无方法体,主要为了让同类中其他方法使用此切入点
    }

    /**
     * 配置环绕通知,使用在方法logPointcut()上注册的切入点
     *
     * @param joinPoint join point for advice
     */
    @Around("logPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;
        currentTime.set(System.currentTimeMillis());
        result = joinPoint.proceed();
        SysLog log = new SysLog("INFO", System.currentTimeMillis() - currentTime.get());
        currentTime.remove();
        HttpServletRequest request = RequestHolder.getHttpServletRequest();
        logService.save(getUsername(),
                IPAddressUtil.getIp(RequestHolder.getHttpServletRequest()), joinPoint,
                log, getUid());
        return result;
    }

    /**
     * 配置异常通知
     *
     * @param joinPoint join point for advice
     * @param e         exception
     */
    @AfterThrowing(pointcut = "logPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        SysLog log = new SysLog("ERROR", System.currentTimeMillis() - currentTime.get());
        currentTime.remove();
        log.setExceptionDetail(ThrowableUtil.getStackTrace(e).getBytes());
        HttpServletRequest request = RequestHolder.getHttpServletRequest();
        logService.save(getUsername(),
                IPAddressUtil.getIp(RequestHolder.getHttpServletRequest()),
                (ProceedingJoinPoint) joinPoint, log, getUid());
    }

    public String getUsername() {
        try {
          return JwtUtils.getUsername();
        } catch (Exception e) {
            return "";
        }
    }

    public Long getUid() {
        try {
            return JwtUtils.getUserId();
        } catch (Exception e) {
            return 0L;
        }
    }
}
