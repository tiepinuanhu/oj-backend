package com.wxc.oj.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.enums.LanguageConfigEnum;
import com.wxc.oj.enums.sandbox.SandBoxResponseStatus;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.judger.SandboxCodeRunner;
import com.wxc.oj.judger.model.TestCase;
import com.wxc.oj.judger.model.TestCases;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.problem.StandardSolution;
import com.wxc.oj.model.req.problem.GenerateCaseItemRequest;
import com.wxc.oj.model.req.problem.GenerateCasesRequest;
import com.wxc.oj.model.req.problem.UploadStandardRequest;
import com.wxc.oj.model.req.sandbox.Result;
import com.wxc.oj.model.vo.problem.GenerateCaseItemVO;
import com.wxc.oj.model.vo.problem.GenerateCasesVO;
import com.wxc.oj.model.vo.problem.StandardSolutionVO;
import com.wxc.oj.model.vo.problem.UploadStandardVO;
import com.wxc.oj.service.ProblemCaseService;
import com.wxc.oj.service.ProblemService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j(topic = "ProblemCaseService")
@Service
public class ProblemCaseServiceImpl implements ProblemCaseService {

    private static final String STANDARD_FILE = "standard.json";
    private static final String CONFIG_FILE = "config.json";
    private static final int MAX_CASES = 50;
    private static final LanguageConfigEnum STANDARD_LANGUAGE = LanguageConfigEnum.CPP;
    private static final Pattern IN_INDEX_PATTERN = Pattern.compile("(\\d+)\\.in$", Pattern.CASE_INSENSITIVE);

    @Resource
    private ProblemService problemService;

    @Resource
    private SandboxCodeRunner sandboxCodeRunner;

    @Value("${oj.data.path}")
    private String dataPath;

    @Override
    public UploadStandardVO uploadStandard(UploadStandardRequest request) {
        if (request == null || request.getProblemId() == null
                || StrUtil.isBlank(request.getSourceCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "problemId/sourceCode 不能为空");
        }

        Problem problem = problemService.getById(request.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_EXIST);
        }

        LanguageConfigEnum languageEnum = STANDARD_LANGUAGE;
        String exeFileId = null;
        try {
            Result compileResult = sandboxCodeRunner.compile(languageEnum, request.getSourceCode());
            if (compileResult == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "sandbox runtime error");
            }
            if (!SandBoxResponseStatus.ACCEPTED.getValue().equals(compileResult.getStatus())) {
                String stderr = compileResult.getFiles() != null ? compileResult.getFiles().getStderr() : null;
                String msg = StrUtil.isNotBlank(stderr) ? stderr : compileResult.getError();
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "标程编译失败: " + (msg == null ? compileResult.getStatus() : msg));
            }
            exeFileId = sandboxCodeRunner.extractExeFileId(languageEnum, compileResult);

            // 持久化到数据库
            Problem update = new Problem();
            update.setId(problem.getId());
            update.setStandardCode(request.getSourceCode());
            boolean updated = problemService.updateById(update);
            if (!updated) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存标程失败");
            }

            // 兼容：同步写一份 standard.json（生成逻辑可回退读取）
            StandardSolution standard = new StandardSolution();
            standard.setLanguage(languageEnum.getValue());
            standard.setSourceCode(request.getSourceCode());
            File dir = problemAnsDir(request.getProblemId());
            FileUtil.mkdir(dir);
            FileUtil.writeString(JSONUtil.toJsonPrettyStr(standard),
                    new File(dir, STANDARD_FILE), StandardCharsets.UTF_8);

            UploadStandardVO vo = new UploadStandardVO();
            vo.setProblemId(request.getProblemId());
            vo.setLanguage(languageEnum.getValue());
            vo.setSaved(true);
            vo.setOutsRegenerated(false);

            List<GenerateCaseItemRequest> existingIns = loadExistingInCases(dir);
            if (!existingIns.isEmpty()) {
                GenerateCasesRequest regen = new GenerateCasesRequest();
                regen.setProblemId(request.getProblemId());
                regen.setOverwrite(true);
                regen.setCases(existingIns);
                GenerateCasesVO regenResult = generateCases(regen);
                vo.setRegenerateResult(regenResult);
                vo.setOutsRegenerated(Boolean.TRUE.equals(regenResult.getPersisted()));
                if (!Boolean.TRUE.equals(regenResult.getPersisted())) {
                    log.warn("标程已保存，但根据已有 .in 重生成 .out 失败, problemId={}",
                            request.getProblemId());
                }
            }
            return vo;
        } finally {
            sandboxCodeRunner.deleteFile(exeFileId);
        }
    }

    @Override
    public StandardSolutionVO getStandard(Long problemId) {
        if (problemId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "problemId 不能为空");
        }
        Problem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_EXIST);
        }
        StandardSolutionVO vo = new StandardSolutionVO();
        vo.setProblemId(problemId);
        vo.setLanguage(STANDARD_LANGUAGE.getValue());
        String code = problem.getStandardCode();
        // DB 为空时尝试从旧版 standard.json 迁移回填
        if (StrUtil.isBlank(code)) {
            StandardSolution fromFile = tryLoadStandardFromFile(problemId);
            if (fromFile != null && StrUtil.isNotBlank(fromFile.getSourceCode())) {
                code = fromFile.getSourceCode();
                Problem update = new Problem();
                update.setId(problemId);
                update.setStandardCode(code);
                problemService.updateById(update);
            }
        }
        vo.setSourceCode(code);
        return vo;
    }

    @Override
    public GenerateCasesVO generateCases(GenerateCasesRequest request) {
        if (request == null || request.getProblemId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "problemId 不能为空");
        }
        List<GenerateCaseItemRequest> caseRequests = request.getCases();
        if (caseRequests == null || caseRequests.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cases 不能为空");
        }
        if (caseRequests.size() > MAX_CASES) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cases 数量不能超过 " + MAX_CASES);
        }

        ensureProblemExists(request.getProblemId());
        boolean overwrite = request.getOverwrite() == null || request.getOverwrite();
        File dir = problemAnsDir(request.getProblemId());
        File configFile = new File(dir, CONFIG_FILE);
        if (!overwrite && configFile.exists()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "样例已存在且 overwrite=false，拒绝覆盖");
        }

        StandardSolution standard = loadStandard(request.getProblemId());
        LanguageConfigEnum languageEnum = STANDARD_LANGUAGE;
        validateCaseRequests(caseRequests);

        List<GenerateCaseItemVO> caseResults = new ArrayList<>();
        List<String> outputs = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        String exeFileId = null;
        try {
            if (sandboxCodeRunner.needCompile(languageEnum)) {
                Result compileResult = sandboxCodeRunner.compile(languageEnum, standard.getSourceCode());
                if (compileResult == null) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "sandbox runtime error");
                }
                if (!SandBoxResponseStatus.ACCEPTED.getValue().equals(compileResult.getStatus())) {
                    String stderr = compileResult.getFiles() != null ? compileResult.getFiles().getStderr() : null;
                    String msg = StrUtil.isNotBlank(stderr) ? stderr : compileResult.getError();
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,
                            "标程编译失败: " + (msg == null ? compileResult.getStatus() : msg));
                }
                exeFileId = sandboxCodeRunner.extractExeFileId(languageEnum, compileResult);
            }

            for (GenerateCaseItemRequest item : caseRequests) {
                GenerateCaseItemVO itemVO = new GenerateCaseItemVO();
                itemVO.setIndex(item.getIndex());
                Result runResult = sandboxCodeRunner.run(
                        languageEnum, standard.getSourceCode(), exeFileId, item.getInput());
                itemVO.setStatus(runResult.getStatus());
                itemVO.setTimeCost(runResult.getRunTime() / 1_000_000);
                itemVO.setMemoryUsed(runResult.getMemory());

                if (SandBoxResponseStatus.ACCEPTED.getValue().equals(runResult.getStatus())) {
                    String output = runResult.getFiles() != null ? runResult.getFiles().getStdout() : "";
                    if (output == null) {
                        output = "";
                    }
                    itemVO.setOk(true);
                    itemVO.setOutput(output);
                    outputs.add(output);
                    successCount++;
                } else {
                    itemVO.setOk(false);
                    String stderr = runResult.getFiles() != null ? runResult.getFiles().getStderr() : null;
                    itemVO.setError(StrUtil.blankToDefault(stderr, runResult.getError()));
                    outputs.add(null);
                    failCount++;
                }
                caseResults.add(itemVO);
            }
        } finally {
            sandboxCodeRunner.deleteFile(exeFileId);
        }

        GenerateCasesVO vo = new GenerateCasesVO();
        vo.setProblemId(request.getProblemId());
        vo.setSuccessCount(successCount);
        vo.setFailCount(failCount);
        vo.setCases(caseResults);

        if (failCount > 0) {
            vo.setPersisted(false);
            log.warn("generateCases 存在失败 case，整批不落盘, problemId={}, failCount={}",
                    request.getProblemId(), failCount);
            return vo;
        }

        clearOldCaseFiles(dir);
        persistCases(dir, caseRequests, outputs);
        vo.setPersisted(true);
        return vo;
    }

    @Override
    public GenerateCasesVO generateCasesByFiles(Long problemId, List<MultipartFile> files) {
        if (problemId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "problemId 不能为空");
        }
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传至少一个 .in 或 .zip 文件");
        }
        List<GenerateCaseItemRequest> cases = parseUploadedInFiles(files);
        GenerateCasesRequest request = new GenerateCasesRequest();
        request.setProblemId(problemId);
        request.setOverwrite(true);
        request.setCases(cases);
        return generateCases(request);
    }

    /**
     * 解析上传的 .in / .zip，得到 cases（按 index 排序）
     */
    private List<GenerateCaseItemRequest> parseUploadedInFiles(List<MultipartFile> files) {
        // index -> input，后上传的同名 index 覆盖先前的
        Map<Integer, String> indexed = new HashMap<>();
        List<String> unindexedContents = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String filename = file.getOriginalFilename();
            if (StrUtil.isBlank(filename)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件名不能为空");
            }
            String lower = filename.toLowerCase(Locale.ROOT);
            try {
                if (lower.endsWith(".zip")) {
                    collectFromZip(file.getInputStream(), indexed, unindexedContents);
                } else if (lower.endsWith(".in")) {
                    collectInContent(filename, new String(file.getBytes(), StandardCharsets.UTF_8),
                            indexed, unindexedContents);
                } else {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,
                            "仅支持 .in 或 .zip 文件: " + filename);
                }
            } catch (BusinessException e) {
                throw e;
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败: " + filename);
            }
        }

        // 无数字文件名的 .in，按出现顺序接到最大 index 之后
        int nextIndex = indexed.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        for (String content : unindexedContents) {
            indexed.put(nextIndex++, content);
        }

        if (indexed.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未找到任何 .in 文件");
        }
        if (indexed.size() > MAX_CASES) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cases 数量不能超过 " + MAX_CASES);
        }

        List<GenerateCaseItemRequest> cases = new ArrayList<>();
        indexed.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(e -> {
                    GenerateCaseItemRequest item = new GenerateCaseItemRequest();
                    item.setIndex(e.getKey());
                    item.setInput(e.getValue());
                    cases.add(item);
                });
        return cases;
    }

    private void collectFromZip(InputStream zipStream,
                                Map<Integer, String> indexed,
                                List<String> unindexedContents) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                // 防 zip slip：拒绝含 .. 的路径
                if (entryName.contains("..")) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法 zip 条目: " + entryName);
                }
                String baseName = FileUtil.getName(entryName);
                if (!baseName.toLowerCase(Locale.ROOT).endsWith(".in")) {
                    continue;
                }
                String content = new String(readAllBytes(zis), StandardCharsets.UTF_8);
                collectInContent(baseName, content, indexed, unindexedContents);
                zis.closeEntry();
            }
        }
    }

    private void collectInContent(String filename, String content,
                                  Map<Integer, String> indexed,
                                  List<String> unindexedContents) {
        Integer index = parseInIndex(filename);
        if (index != null) {
            indexed.put(index, content);
        } else {
            unindexedContents.add(content);
        }
    }

    private Integer parseInIndex(String filename) {
        String baseName = FileUtil.getName(filename);
        Matcher matcher = IN_INDEX_PATTERN.matcher(baseName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }

    private void clearOldCaseFiles(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = f.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".in") || name.endsWith(".out") || name.equals(CONFIG_FILE)) {
                FileUtil.del(f);
            }
        }
    }

    /**
     * 从题目目录读取已有 .in（优先 config.json；否则扫描 *.in）
     */
    private List<GenerateCaseItemRequest> loadExistingInCases(File dir) {
        List<GenerateCaseItemRequest> cases = new ArrayList<>();
        File configFile = new File(dir, CONFIG_FILE);
        if (configFile.exists()) {
            String json = FileUtil.readString(configFile, StandardCharsets.UTF_8);
            TestCases testCases = JSONUtil.toBean(json, TestCases.class);
            if (testCases != null && testCases.getCases() != null) {
                for (TestCase tc : testCases.getCases()) {
                    if (tc == null || tc.getIndex() <= 0) {
                        continue;
                    }
                    File inFile = new File(dir, tc.getIndex() + ".in");
                    if (!inFile.exists()) {
                        // config 指向的文件名
                        if (StrUtil.isNotBlank(tc.getInput())) {
                            inFile = new File(dir, FileUtil.getName(tc.getInput()));
                        }
                    }
                    if (!inFile.exists()) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                                "config.json 中缺少输入文件: " + tc.getIndex() + ".in");
                    }
                    GenerateCaseItemRequest item = new GenerateCaseItemRequest();
                    item.setIndex(tc.getIndex());
                    item.setInput(FileUtil.readString(inFile, StandardCharsets.UTF_8));
                    item.setFullScore(tc.getFullScore());
                    cases.add(item);
                }
                cases.sort(Comparator.comparingInt(GenerateCaseItemRequest::getIndex));
                return cases;
            }
        }

        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".in"));
        if (files == null || files.length == 0) {
            return cases;
        }
        Map<Integer, String> indexed = new HashMap<>();
        List<String> unindexed = new ArrayList<>();
        for (File f : files) {
            collectInContent(f.getName(), FileUtil.readString(f, StandardCharsets.UTF_8), indexed, unindexed);
        }
        int next = indexed.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        for (String content : unindexed) {
            indexed.put(next++, content);
        }
        indexed.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(e -> {
                    GenerateCaseItemRequest item = new GenerateCaseItemRequest();
                    item.setIndex(e.getKey());
                    item.setInput(e.getValue());
                    cases.add(item);
                });
        return cases;
    }

    private void persistCases(File dir, List<GenerateCaseItemRequest> caseRequests, List<String> outputs) {
        FileUtil.mkdir(dir);
        List<Integer> scores = resolveFullScores(caseRequests);
        TestCases testCases = new TestCases();
        List<TestCase> cases = new ArrayList<>();

        for (int i = 0; i < caseRequests.size(); i++) {
            GenerateCaseItemRequest item = caseRequests.get(i);
            int index = item.getIndex();
            String inName = index + ".in";
            String outName = index + ".out";
            FileUtil.writeString(item.getInput() == null ? "" : item.getInput(),
                    new File(dir, inName), StandardCharsets.UTF_8);
            FileUtil.writeString(outputs.get(i), new File(dir, outName), StandardCharsets.UTF_8);

            TestCase testCase = new TestCase();
            testCase.setIndex(index);
            testCase.setInput(inName);
            testCase.setOutput(outName);
            testCase.setFullScore(scores.get(i));
            cases.add(testCase);
        }
        testCases.setCases(cases);
        FileUtil.writeString(JSONUtil.toJsonPrettyStr(testCases),
                new File(dir, CONFIG_FILE), StandardCharsets.UTF_8);
        log.info("已写入样例到 {}", dir.getAbsolutePath());
    }

    private List<Integer> resolveFullScores(List<GenerateCaseItemRequest> caseRequests) {
        boolean allSpecified = caseRequests.stream().allMatch(c -> c.getFullScore() != null);
        List<Integer> scores = new ArrayList<>(caseRequests.size());
        if (allSpecified) {
            for (GenerateCaseItemRequest c : caseRequests) {
                if (c.getFullScore() <= 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "fullScore 必须为正整数");
                }
                scores.add(c.getFullScore());
            }
            return scores;
        }
        boolean anySpecified = caseRequests.stream().anyMatch(c -> c.getFullScore() != null);
        if (anySpecified) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "fullScore 需全部指定或全部省略");
        }
        int n = caseRequests.size();
        int base = 100 / n;
        int rem = 100 % n;
        for (int i = 0; i < n; i++) {
            scores.add(base + (i < rem ? 1 : 0));
        }
        return scores;
    }

    private void validateCaseRequests(List<GenerateCaseItemRequest> caseRequests) {
        Set<Integer> indexes = new HashSet<>();
        for (GenerateCaseItemRequest item : caseRequests) {
            if (item.getIndex() == null || item.getIndex() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "case.index 必须为正整数");
            }
            if (item.getInput() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "case.input 不能为 null");
            }
            if (!indexes.add(item.getIndex())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "case.index 不能重复: " + item.getIndex());
            }
        }
    }

    private StandardSolution loadStandard(Long problemId) {
        Problem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_EXIST);
        }
        if (StrUtil.isNotBlank(problem.getStandardCode())) {
            StandardSolution standard = new StandardSolution();
            standard.setLanguage(STANDARD_LANGUAGE.getValue());
            standard.setSourceCode(problem.getStandardCode());
            return standard;
        }
        StandardSolution fromFile = tryLoadStandardFromFile(problemId);
        if (fromFile != null && StrUtil.isNotBlank(fromFile.getSourceCode())) {
            return fromFile;
        }
        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请先上传标程");
    }

    private StandardSolution tryLoadStandardFromFile(Long problemId) {
        File file = new File(problemAnsDir(problemId), STANDARD_FILE);
        if (!file.exists()) {
            return null;
        }
        try {
            String json = FileUtil.readString(file, StandardCharsets.UTF_8);
            return JSONUtil.toBean(json, StandardSolution.class);
        } catch (Exception e) {
            log.warn("读取 standard.json 失败, problemId={}", problemId, e);
            return null;
        }
    }

    private File problemAnsDir(Long problemId) {
        return new File(dataPath + File.separator + "ans" + File.separator + problemId);
    }

    private void ensureProblemExists(Long problemId) {
        Problem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_EXIST);
        }
    }

    private LanguageConfigEnum parseLanguage(String language) {
        try {
            return LanguageConfigEnum.fromValue(language);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED, language);
        }
    }
}
