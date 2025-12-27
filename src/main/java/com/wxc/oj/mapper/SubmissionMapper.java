package com.wxc.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxc.oj.model.po.Submission;
import com.wxc.oj.model.vo.submission.SubmissionStatusCount;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 王新超
* @description 针对表【submission】的数据库操作Mapper
* @createDate 2024-02-28 10:33:17
* @Entity com.wxc.oj.pojo.Submission
*/
public interface SubmissionMapper extends BaseMapper<Submission> {
    List<SubmissionStatusCount> getStatusDistribution(@Param("problemId") Long problemId);
}




