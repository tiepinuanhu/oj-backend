package com.wxc.oj.service.impl;

import com.wxc.oj.common.ErrorCode;
import com.wxc.oj.enums.UserRoleEnum;
import com.wxc.oj.exception.BusinessException;
import com.wxc.oj.model.po.Problem;
import com.wxc.oj.model.po.Tag;
import com.wxc.oj.model.po.User;
import com.wxc.oj.model.req.problem.ProblemAddRequest;
import com.wxc.oj.model.req.problem.ProblemQueryRequest;
import com.wxc.oj.model.req.problem.ProblemTag;
import com.wxc.oj.model.vo.problem.ListProblemVO;
import com.wxc.oj.model.vo.problem.ProblemVO;
import com.wxc.oj.service.ProblemEsService;
import com.wxc.oj.service.ProblemTagService;
import com.wxc.oj.service.TagService;
import com.wxc.oj.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private TagService tagService;

    @Mock
    private ProblemTagService problemTagService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ProblemEsService problemEsService;

    private ProblemServiceImpl problemService;

    @BeforeEach
    void setUp() {
        problemService = spy(new ProblemServiceImpl());
        ReflectionTestUtils.setField(problemService, "userService", userService);
        ReflectionTestUtils.setField(problemService, "tagService", tagService);
        ReflectionTestUtils.setField(problemService, "problemTagService", problemTagService);
        ReflectionTestUtils.setField(problemService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(problemService, "problemEsService", problemEsService);
    }

    @Test
    void getProblemVOByIdRejectsNullId() {
        BusinessException exception = assertThrows(
                BusinessException.class, () -> problemService.getProblemVOById(null));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getProblemVOByIdReturnsCachedValueWithoutDatabaseQuery() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ProblemVO cached = new ProblemVO();
        cached.setId(1L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("{\"id\":1}");

        ProblemVO result = problemService.getProblemVOById(1L);

        assertEquals(cached.getId(), result.getId());
        verify(problemService, never()).getById(any());
    }

    @Test
    void getQueryWrapperUsesAllSelectedTagsAndRestrictsNormalUsers() {
        ProblemQueryRequest request = queryRequest(List.of(1, 2));
        User user = user(10L, UserRoleEnum.USER.getValue());
        when(userService.getById(10L)).thenReturn(user);
        when(problemTagService.list(any())).thenReturn(List.of(
                relation(100L, 1),
                relation(100L, 2),
                relation(200L, 1)));

        String sql = problemService.getQueryWrapper(request).getSqlSegment();

        assertTrue(sql.contains("id IN"));
        assertTrue(sql.contains("is_public"));
    }

    @Test
    void getQueryWrapperForcesEmptyResultWhenNoProblemHasEveryTag() {
        ProblemQueryRequest request = queryRequest(List.of(1, 2));
        when(userService.getById(10L)).thenReturn(user(10L, UserRoleEnum.ADMIN.getValue()));
        when(problemTagService.list(any())).thenReturn(List.of(
                relation(100L, 1),
                relation(200L, 2)));

        String sql = problemService.getQueryWrapper(request).getSqlSegment();

        assertTrue(sql.contains("id"));
        assertFalse(sql.contains("is_public"));
    }

    @Test
    void listConversionLoadsUsersAndTagsInBatches() {
        Problem first = problem(1L, 11L);
        Problem second = problem(2L, 12L);
        User firstUser = user(11L, UserRoleEnum.USER.getValue());
        firstUser.setUserAccount("first");
        User secondUser = user(12L, UserRoleEnum.USER.getValue());
        secondUser.setUserAccount("second");
        Tag tag = new Tag();
        tag.setId(3);
        tag.setName("graph");
        when(userService.listByIds(anyCollection())).thenReturn(List.of(firstUser, secondUser));
        when(problemTagService.list(any())).thenReturn(List.of(relation(1L, 3), relation(2L, 3)));
        when(tagService.listByIds(anyCollection())).thenReturn(List.of(tag));

        List<ListProblemVO> result = problemService.getProblemVOListByProblemList(List.of(first, second));

        assertEquals(2, result.size());
        assertEquals("first", result.get(0).getPublisherName());
        assertEquals(List.of(tag), result.get(1).getTags());
        verify(userService).listByIds(anyCollection());
        verify(tagService).listByIds(anyCollection());
        verify(userService, never()).getById(any());
        verify(tagService, never()).listTagsByProblemId(any());
    }

    @Test
    void addProblemAcceptsEmptyTagsAndSyncsEs() {
        ProblemAddRequest request = new ProblemAddRequest();
        request.setTitle("A+B");
        request.setContent("Calculate A+B");
        request.setLevel(1);
        request.setPublisherId(10L);
        request.setIsPublic(true);
        request.setTags(null);
        doAnswer(invocation -> {
            Problem problem = invocation.getArgument(0);
            problem.setId(99L);
            return true;
        }).when(problemService).save(any(Problem.class));

        assertTrue(problemService.addProblem(request));

        verify(problemTagService, never()).saveBatch(anyCollection());
        verify(problemEsService).saveOrUpdate(any(Problem.class));
    }

    @Test
    void addProblemRejectsUnknownTag() {
        ProblemAddRequest request = new ProblemAddRequest();
        request.setTitle("A+B");
        request.setContent("Calculate A+B");
        request.setLevel(1);
        request.setPublisherId(10L);
        request.setIsPublic(true);
        request.setTags(List.of(999));
        when(tagService.count(any())).thenReturn(0L);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> problemService.addProblem(request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        verify(problemService, never()).save(any(Problem.class));
    }

    private ProblemQueryRequest queryRequest(List<Integer> tags) {
        ProblemQueryRequest request = new ProblemQueryRequest();
        request.setUserId(10L);
        request.setTags(tags);
        return request;
    }

    private ProblemTag relation(Long problemId, Integer tagId) {
        ProblemTag relation = new ProblemTag();
        relation.setProblemId(problemId);
        relation.setTagId(tagId);
        return relation;
    }

    private Problem problem(Long id, Long userId) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setUserId(userId);
        problem.setIsPublic(1);
        return problem;
    }

    private User user(Long id, Integer role) {
        User user = new User();
        user.setId(id);
        user.setUserRole(role);
        return user;
    }
}
