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
 * <p>尚未实现具体导入逻辑，统一返回失败，避免调用方误以为数据已写入。</p>
 */
@Slf4j
@RestController
@RequestMapping("/admin/import")
public class DataImportController {

    private static final String NOT_IMPLEMENTED = "导入功能尚未实现";

    // ========== 导入单词 ==========

    @PostMapping("/words")
    public Result<String> importWords(@Valid @RequestBody List<WordImportDTO> words) {
        // TODO: 实现批量导入/更新逻辑，按(word_book_id, word)判断已存在记录，存在则update，不存在则insert
        log.warn("导入单词接口尚未实现，本次请求 {} 条", words.size());
        return Result.fail(NOT_IMPLEMENTED);
    }

    // ========== 导入词根 ==========

    @PostMapping("/roots")
    public Result<String> importRoots(@Valid @RequestBody List<RootImportDTO> roots) {
        // TODO: 实现批量导入/更新逻辑，按 root 文本去重
        log.warn("导入词根接口尚未实现，本次请求 {} 条", roots.size());
        return Result.fail(NOT_IMPLEMENTED);
    }

    // ========== 导入单词-词根关联 ==========

    @PostMapping("/word-roots")
    public Result<String> importWordRoots(@Valid @RequestBody List<WordRootImportDTO> wordRoots) {
        // TODO: 实现批量导入，可按(word_id, root_id)唯一键去重
        log.warn("导入单词词根关联接口尚未实现，本次请求 {} 条", wordRoots.size());
        return Result.fail(NOT_IMPLEMENTED);
    }

    // ========== 上传CSV文件导入（后续扩展） ==========

    @PostMapping("/upload")
    public Result<Void> uploadFile() {
        // TODO: 接收MultipartFile，解析CSV/JSON文件，按类型调度到对应导入逻辑
        return Result.fail(NOT_IMPLEMENTED);
    }
}
