package com.example.chat.service;

import com.example.chat.dto.TreeHoleMessageDTO;
import com.example.chat.model.TreeHoleMessage;
import com.example.chat.repository.TreeHoleRepository;
import com.example.chat.util.IdGenerator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 树洞功能：生成匿名代号、保存与读取消息。
 */
public class TreeHoleService {
    private static final List<String> DEFAULT_ALIASES = List.of(
            "匿名流星", "树洞路人", "夜行人", "匿名小纸条", "无名之辈", "远方回声", "匿名叶子", "路过的风", "树洞旅人", "匿名鸟", "无名旅客"
    );

    private final TreeHoleRepository repository;
    private final int defaultLimit;

    public TreeHoleService(TreeHoleRepository repository, int defaultLimit) {
        this.repository = repository;
        this.defaultLimit = defaultLimit;
    }

    public TreeHoleMessageDTO post(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("树洞内容不能为空");
        }
        String alias = pickAlias();
        TreeHoleMessage message = new TreeHoleMessage(IdGenerator.newId(), content.trim(), alias, System.currentTimeMillis()
        );
        repository.save(message);
        return TreeHoleMessageDTO.from(message);
    }

    public List<TreeHoleMessageDTO> list(Integer limit) {
        int realLimit = limit == null || limit <= 0 ? defaultLimit : Math.min(limit, defaultLimit);
        return repository.findRecent(realLimit).stream()
                .map(TreeHoleMessageDTO::from)
                .collect(Collectors.toList());
    }

    private String pickAlias() {
        List<String> aliases = DEFAULT_ALIASES;
        int idx = ThreadLocalRandom.current().nextInt(aliases.size());
        return aliases.get(idx);
    }
}
