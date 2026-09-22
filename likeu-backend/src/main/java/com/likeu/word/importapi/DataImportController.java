package com.likeu.word.importapi;

import com.likeu.word.common.Result;
import com.likeu.word.importapi.dto.RootImportRequestDTO;
import com.likeu.word.importapi.dto.WordImportRequestDTO;
import com.likeu.word.importapi.dto.WordRootImportRequestDTO;
import com.likeu.word.importapi.service.DataImportService;
import com.likeu.word.importapi.vo.ImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 数据批量导入接口（仅管理员可用，由 AdminInterceptor 拦 /admin/**）
 *
 * <p>统一按业务自然键判存：存在则更新、不存在则插入，返回 {@code {inserted, updated, skipped}}。</p>
 */
@Tag(name = "数据导入", description = "基础数据批量导入（仅管理员，openid 白名单校验）")
@Slf4j
@RestController
@RequestMapping("/admin/import")
public class DataImportController {

    /** 上传文件导入尚未实现：改文件解析的工程量远超本期范围，先用明确文案引导调用方走 JSON 接口 */
    private static final String UPLOAD_NOT_IMPLEMENTED =
            "文件导入暂未实现，请改用 JSON 批量导入接口（/admin/import/words、/roots、/word-roots）";

    @Resource
    private DataImportService dataImportService;

    // ========== 导入单词 ==========

    @Operation(summary = "批量导入单词", description = "按 (词书ID, 单词) 判存：存在则更新、不存在则插入")
    @PostMapping("/words")
    public Result<ImportResultVO> importWords(@Valid @RequestBody WordImportRequestDTO request) {
        return Result.success(dataImportService.importWords(request.getItems()));
    }

    // ========== 导入词根 ==========

    @Operation(summary = "批量导入词根词缀", description = "按词根文本判存，已存在时保留其热度")
    @PostMapping("/roots")
    public Result<ImportResultVO> importRoots(@Valid @RequestBody RootImportRequestDTO request) {
        return Result.success(dataImportService.importRoots(request.getItems()));
    }

    // ========== 导入单词-词根关联 ==========

    @Operation(summary = "批量导入单词词根关联", description = "按 (单词ID, 词根ID) 判存")
    @PostMapping("/word-roots")
    public Result<ImportResultVO> importWordRoots(@Valid @RequestBody WordRootImportRequestDTO request) {
        return Result.success(dataImportService.importWordRoots(request.getItems()));
    }

    // ========== 上传CSV文件导入（未实现） ==========

    @Operation(summary = "上传文件导入（未实现）")
    @PostMapping("/upload")
    public Result<Void> uploadFile() {
        return Result.fail(UPLOAD_NOT_IMPLEMENTED);
    }
}
