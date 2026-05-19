<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ include file="/WEB-INF/views/layout/header.jsp"%>

<div class="card">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-pencil me-2"></i>게시글 수정</h5>
    </div>
    <form action="/board/${board.id}/edit" method="post">
        <div class="card-body">
            <div class="mb-3">
                <label for="title" class="form-label fw-semibold">제목</label>
                <input type="text" class="form-control" id="title" name="title"
                       value="${board.title}" required maxlength="200">
            </div>
            <div class="mb-3">
                <label class="form-label fw-semibold">작성자</label>
                <input type="text" class="form-control" value="${board.author}" readonly disabled>
            </div>
            <div class="mb-3">
                <label for="content" class="form-label fw-semibold">내용</label>
                <textarea class="form-control" id="content" name="content" rows="12"
                          required>${board.content}</textarea>
            </div>
        </div>
        <div class="card-footer bg-white">
            <div class="d-flex justify-content-between">
                <a href="/board/${board.id}" class="btn btn-outline-secondary btn-sm">
                    <i class="bi bi-x-lg me-1"></i>취소
                </a>
                <button type="submit" class="btn btn-dark btn-sm btn-write">
                    <i class="bi bi-check-lg me-1"></i>수정
                </button>
            </div>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jsp"%>
