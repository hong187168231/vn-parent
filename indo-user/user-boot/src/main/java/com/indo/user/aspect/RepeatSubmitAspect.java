package com.indo.user.aspect;

import com.indo.common.annotation.NoRepeatSubmit;
import com.indo.common.constant.AuthConstants;
import com.indo.common.result.Result;
import com.indo.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author puff
 * @date 2022/1/18
 * 自定义防重复提交注解
 */
@Aspect
@Component
@Slf4j
@SuppressWarnings("all")
public class RepeatSubmitAspect {

    public static final String KEYPREX = "noRpeat:user:";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 进行接口防重复操作处理
     *
     * @param pjp
     * @param noRepeatSubmit
     * @return
     */
    @Around("execution(* com.indo.user.controller.*.*(..)) && @annotation(noRepeatSubmit)")
    public Object around(ProceedingJoinPoint pjp, NoRepeatSubmit noRepeatSubmit) throws Throwable {
        try {
            //获取request
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            //拿到token和请求路径
            StringBuilder sb = new StringBuilder();
            String token = request.getHeader(AuthConstants.AUTHORIZATION_KEY);
            sb.append(KEYPREX).append(token).append(request.getRequestURI().toString());
            //获取现在时间
            long now = System.currentTimeMillis();
            if (redisTemplate.hasKey(sb.toString())) {
                //上次请求时间
                long lastTime = Long.valueOf(redisTemplate.opsForValue().get(sb.toString()).toString());
                // 如果现在距离上次提交时间小于设置的默认时间 则 判断为重复提交  否则 正常提交 -> 进入业务处理
                if ((now - lastTime) > noRepeatSubmit.lockTime()) {
                    //重新记录时间 10分钟过期时间
                    redisTemplate.opsForValue().set(sb.toString(), String.valueOf(now), 10, TimeUnit.MINUTES);
                    //处理业务
                    Object result = pjp.proceed();
                    return result;
                } else {
                    return Result.failed(ResultCode.SUBMIT_ERROR);
                }
            } else {
                //第一次操作
                redisTemplate.opsForValue().set(sb.toString(), String.valueOf(now), 10, TimeUnit.MINUTES);
                Object result = pjp.proceed();
                return result;
            }
        } catch (Throwable e) {
            log.error("校验表单重复提交时异常: {}", e.getMessage());
            return Result.failed(ResultCode.SYSTEM_EXECUTION_ERROR);
        }
    }


}
