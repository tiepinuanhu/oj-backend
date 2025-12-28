package com.wxc.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxc.oj.cache.TagCache;
import com.wxc.oj.mapper.ProblemTagMapper;
import com.wxc.oj.model.po.Tag;
import com.wxc.oj.model.req.problem.ProblemTag;
import com.wxc.oj.service.ProblemTagService;
import com.wxc.oj.service.TagService;
import com.wxc.oj.mapper.TagMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author 王新超
* @description 针对表【tag】的数据库操作Service实现
* @createDate 2024-03-08 20:51:25
*/
@Service
public class
TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService {

    @Resource
    private ProblemTagMapper problemTagMapper;

    /**
     * 使用本地缓存
     */
    @Resource
    private TagCache tagCache;

    public List<Tag> listTagsByProblemId(Long problemId) {
        LambdaQueryWrapper<ProblemTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProblemTag::getProblemId, problemId)
                .select(ProblemTag::getTagId);
        // 在problem_tag表中查到tag_ids
        List<ProblemTag> problemTags = problemTagMapper.selectList(queryWrapper);
        // 通过tag_ids去本地缓存TagCache中查tag
        List<Tag> tags = problemTags.stream()
                .map(problemTag -> tagCache.getById(problemTag.getTagId()))
                .collect(Collectors.toList());
        return tags;
    }
}




