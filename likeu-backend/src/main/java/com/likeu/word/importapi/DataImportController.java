package com.likeu.word.importapi;

import com.likeu.word.common.Result;
import com.likeu.word.importapi.dto.RootImportDTO;
import com.likeu.word.importapi.dto.WordImportDTO;
import com.likeu.word.importapi.dto.WordRootImportDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 数据批量导入接口（骨架，后续实现具体业务逻辑）
 *
 * 使用方式：POST JSON数组，或上传CSV文件
 */
@Slf4j
@RestController
@RequestMapping("/admin/import")
public class DataImportController {

    // ========== 导入单词 ==========

    @PostMapping("/words")
    public Result<String> importWords(@Valid @RequestBody List<WordImportDTO> words) {
        log.info("收到导入单词请求，数量: {}", words.size());
        // TODO: 实现批量导入/更新逻辑，注意唯一键判断（按word+word_book_id去重）
        // 1. 先查已存在记录（按word去重）
        // 2. 存在则update，不存在则insert
        return Result.success("导入成功，共" + words.size() + "条");
    }

    // ========== 导入词根 ==========

    @PostMapping("/roots")
    public Result<String> importRoots(@Valid @RequestBody List<RootImportDTO> roots) {
        log.info("收到导入词根请求，数量: {}", roots.size());
        // TODO: 实现批量导入/更新逻辑，注意按root文本去重
        return Result.success("导入成功，共" + roots.size() + "条");
    }

    // ========== 导入单词-词根关联 ==========

    @PostMapping("/word-roots")
    public Result<String> importWordRoots(@Valid @RequestBody List<WordRootImportDTO> wordRoots) {
        log.info("收到导入单词-词根关联请求，数量: {}", wordRoots.size());
        // TODO: 实现批量导入，可按(word_id, root_id)唯一键去重
        return Result.success("导入成功，共" + wordRoots.size() + "条");
    }

    // ========== 上传CSV文件导入（后续扩展） ==========

    @PostMapping("/upload")
    public Result<Void> uploadFile() {
        // TODO: 接收MultipartFile，解析CSV/JSON文件，按类型调度到对应导入逻辑
        return Result.fail("尚未实现");
    }
}