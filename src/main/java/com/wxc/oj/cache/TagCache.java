package com.wxc.oj.cache;

import com.wxc.oj.mapper.TagMapper;
import com.wxc.oj.model.po.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TagCache {

    /**
     * tag id -> tag
     */
    private Map<Integer, Tag> tagMap = new ConcurrentHashMap<>();


    @Resource
    private TagMapper tagMapper;

    @PostConstruct
    public void init() {
        List<Tag> tags = tagMapper.selectList(null);
        for (Tag tag : tags) {
            tagMap.put(tag.getId(), tag);
        }
    }

    public Tag getById(Integer id) {
        return tagMap.get(id);
    }
}
