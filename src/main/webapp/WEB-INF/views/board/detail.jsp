<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@ include file="/WEB-INF/views/layout/header.jsp"%>

<div class="card">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-file-text me-2"></i>게시글 상세</h5>
    </div>
    <div class="card-body">
        <h4 class="card-title mb-3">${board.title}</h4>
        <div class="d-flex justify-content-between text-muted border-bottom pb-3 mb-4">
            <div>
                <i class="bi bi-person me-1"></i>${board.author}
                <span class="mx-2">|</span>
                <i class="bi bi-calendar3 me-1"></i>
                <fmt:parseDate value="${board.createdAt}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedDate"/>
                <fmt:formatDate value="${parsedDate}" pattern="yyyy-MM-dd HH:mm"/>
            </div>
            <div>
                <i class="bi bi-eye me-1"></i>조회 ${board.viewCount}
            </div>
        </div>
        <div class="card-text mb-4" style="min-height: 200px; white-space: pre-wrap; line-height: 1.8;">${board.content}</div>
    </div>
    <div class="card-footer bg-white">
        <div class="d-flex justify-content-between">
            <a href="/board" class="btn btn-outline-secondary btn-sm">
                <i class="bi bi-list-ul me-1"></i>목록
            </a>
            <div>
                <a href="/board/${board.id}/edit" class="btn btn-outline-dark btn-sm me-1">
                    <i class="bi bi-pencil me-1"></i>수정
                </a>
                <button type="button" class="btn btn-outline-danger btn-sm" data-bs-toggle="modal" data-bs-target="#deleteModal">
                    <i class="bi bi-trash me-1"></i>삭제
                </button>
            </div>
        </div>
    </div>
</div>

<!-- 삭제 확인 모달 -->
<div class="modal fade" id="deleteModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h6 class="modal-title"><i class="bi bi-exclamation-triangle me-2"></i>삭제 확인</h6>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                정말로 이 게시글을 삭제하시겠습니까?<br>
                <small class="text-muted">삭제된 게시글은 복구할 수 없습니다.</small>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">취소</button>
                <form action="/board/${board.id}/delete" method="post" class="d-inline">
                    <button type="submit" class="btn btn-sm btn-danger">
                        <i class="bi bi-trash me-1"></i>삭제
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jsp"%>
