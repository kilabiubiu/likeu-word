package com.likeu.word.modules.word.vo;

import com.likeu.word.modules.word.entity.WordEntity;
import com.likeu.word.modules.word.entity.WordRootEntity;
import lombok.Data;

import java.util.List;

/**
 * 单词详情VO（含词根拆解信息）
 */
@Data
public class WordDetailVO {

    private Long id;
    private String word;
    private String phoneticUk;
    private String phoneticUs;
    private String meaningCn;
    private String exampleEn;
    private String exampleCn;

    /** 词根词缀拆解列表 */
    private List<RootChip> roots;

    @Data
    public static class RootChip {
        private Long id;
        private String root;
        private String meaning;
        private Integer type; // 1-前缀 2-词根 3-后缀
    }

    public static WordDetailVO from(WordEntity entity, List<RootChip> roots) {
        WordDetailVO vo = new WordDetailVO();
        vo.setId(entity.getId());
        vo.setWord(entity.getWord());
        vo.setPhoneticUk(entity.getPhoneticUk());
        vo.setPhoneticUs(entity.getPhoneticUs());
        vo.setMeaningCn(entity.getMeaningCn());
        vo.setExampleEn(entity.getExampleEn());
        vo.setExampleCn(entity.getExampleCn());
        vo.setRoots(roots);
        return vo;
    }
}