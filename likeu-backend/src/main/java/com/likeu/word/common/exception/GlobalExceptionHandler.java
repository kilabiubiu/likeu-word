package com.likeu.word.common.exception;

import com.likeu.word.common.Result;
import com.likeu.word.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>返回给客户端的文案一律为固定描述，绝不拼接原始异常信息，
 * 避免把表名、唯一键名、SQL 片段或连接信息泄露给调用方；原始异常只写服务端日志。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 兜底响应的固定文案，不含任何内部细节 */
    private static final String INTERNAL_ERROR_MSG = "系统繁忙，请稍后重试";

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", msg);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", msg);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 请求体格式错误（JSON 语法错误、字段类型不匹配等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求参数格式错误");
    }

    /**
     * 缺少必填的请求参数，如未传 bookId
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配，如 ?page=abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: name={}, value={}", e.getName(), e.getValue());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "参数类型错误: " + e.getName());
    }

    /**
     * 唯一键冲突，如重复收藏同一单词
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突", e);
        return Result.fail(ResultCode.DATA_CONFLICT.getCode(), ResultCode.DATA_CONFLICT.getMessage());
    }

    /**
     * 数据完整性异常，如字段超长、非空约束
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性异常", e);
        return Result.fail(ResultCode.DATA_ERROR.getCode(), ResultCode.DATA_ERROR.getMessage());
    }

    /**
     * Redis 不可用，如连接失败、超时
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    public Result<Void> handleRedisConnectionFailure(RedisConnectionFailureException e) {
        log.error("Redis 连接失败", e);
        return Result.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), ResultCode.SERVICE_UNAVAILABLE.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.FAIL.getCode(), INTERNAL_ERROR_MSG);
    }
}
