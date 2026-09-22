package com.likeu.word.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一返回状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),

    // 业务错误码 1xxx
    USER_NOT_EXIST(1001, "用户不存在"),
    WX_LOGIN_FAIL(1002, "微信登录失败"),
    TOKEN_INVALID(1003, "Token无效"),
    WORD_NOT_FOUND(1004, "单词不存在"),
    ROOT_NOT_FOUND(1005, "词根不存在"),
    BOOK_NOT_FOUND(1006, "词书不存在"),

    // 导入错误 2xxx
    IMPORT_EMPTY(2001, "导入数据为空"),
    IMPORT_FORMAT_ERROR(2002, "导入文件格式错误"),

    // 数据与中间件错误 3xxx
    DATA_CONFLICT(3001, "数据已存在或违反唯一性约束"),
    DATA_ERROR(3002, "数据保存失败，请稍后重试"),
    SERVICE_UNAVAILABLE(3003, "服务暂时不可用，请稍后重试"),
    ;

    private final Integer code;
    private final String message;
}