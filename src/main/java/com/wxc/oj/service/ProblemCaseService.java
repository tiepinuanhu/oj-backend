package com.wxc.oj.service;

import com.wxc.oj.model.req.problem.GenerateCasesRequest;
import com.wxc.oj.model.req.problem.UploadStandardRequest;
import com.wxc.oj.model.vo.problem.GenerateCasesVO;
import com.wxc.oj.model.vo.problem.StandardSolutionVO;
import com.wxc.oj.model.vo.problem.UploadStandardVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProblemCaseService {

    /**
     * 上传并编译校验 C++ 标程，保存到 problem.standard_code
     */
    UploadStandardVO uploadStandard(UploadStandardRequest request);

    /**
     * 查询题目标程（管理员回填编辑器）
     */
    StandardSolutionVO getStandard(Long problemId);

    /**
     * 根据已保存标程与请求体输入生成样例；全部成功才落盘
     */
    GenerateCasesVO generateCases(GenerateCasesRequest request);

    /**
     * 上传多个 .in 或包含 .in 的 zip，解析后按标程生成并覆盖样例
     */
    GenerateCasesVO generateCasesByFiles(Long problemId, List<MultipartFile> files);
}
