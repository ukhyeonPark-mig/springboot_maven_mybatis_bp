<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ include file="/WEB-INF/views/layout/header.jsp"%>

<div class="card">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-pencil-square me-2"></i>글쓰기</h5>
    </div>
    <form action="/board/write" method="post">
        <div class="card-body">
            <div class="mb-3">
                <label for="title" class="form-label fw-semibold">제목</label>
                <input type="text" class="form-control" id="title" name="title"
                       placeholder="제목을 입력하세요" required maxlength="200">
            </div>
            <div class="mb-3">
                <label for="author" class="form-label fw-semibold">작성자</label>
                <input type="text" class="form-control" id="author" name="author"
                       placeholder="작성자를 입력하세요" required maxlength="50">
            </div>
            <div class="mb-3">
                <label for="content" class="form-label fw-semibold">내용</label>
                <textarea class="form-control" id="content" name="content" rows="12"
                          placeholder="내용을 입력하세요" required></textarea>
            </div>
        </div>
        <div class="card-footer bg-white">
            <div class="d-flex justify-content-between">
                <a href="/board" class="btn btn-outline-secondary btn-sm">
                    <i class="bi bi-x-lg me-1"></i>취소
                </a>
                <button type="submit" class="btn btn-dark btn-sm btn-write">
                    <i class="bi bi-check-lg me-1"></i>등록
                </button>
            </div>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jsp"%>
