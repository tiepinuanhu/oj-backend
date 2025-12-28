package com.wxc.oj.service;

import com.wxc.oj.model.po.Tag;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 王新超
* @description 针对表【tag】的数据库操作Service
* @createDate 2024-03-08 20:51:25
*/
public interface TagService extends IService<Tag> {

    List<Tag> listTagsByProblemId(Long problemId);

}
