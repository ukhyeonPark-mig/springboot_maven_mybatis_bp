<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@ include file="/WEB-INF/views/layout/header.jsp"%>

<div class="card">
    <div class="card-header bg-white">
        <div class="row align-items-center">
            <div class="col">
                <h5 class="mb-0"><i class="bi bi-list-ul me-2"></i>게시글 목록</h5>
            </div>
            <div class="col-auto">
                <span class="badge bg-secondary">총 ${totalCount}건</span>
            </div>
        </div>
    </div>
    <div class="card-body">
        <!-- 검색 -->
        <form action="/board" method="get" class="mb-3">
            <div class="row g-2 justify-content-center">
                <div class="col-auto">
                    <select name="searchType" class="form-select form-select-sm">
                        <option value="all" ${search.searchType == 'all' ? 'selected' : ''}>전체</option>
                        <option value="title" ${search.searchType == 'title' ? 'selected' : ''}>제목</option>
                        <option value="content" ${search.searchType == 'content' ? 'selected' : ''}>내용</option>
                        <option value="author" ${search.searchType == 'author' ? 'selected' : ''}>작성자</option>
                    </select>
                </div>
                <div class="col-auto">
                    <input type="text" name="keyword" class="form-control form-control-sm"
                           placeholder="검색어를 입력하세요" value="${search.keyword}">
                </div>
                <div class="col-auto">
                    <button type="submit" class="btn btn-sm btn-dark">
                        <i class="bi bi-search me-1"></i>검색
                    </button>
                </div>
            </div>
        </form>

        <!-- 테이블 -->
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead>
                    <tr class="text-center">
                        <th style="width:70px">번호</th>
                        <th>제목</th>
                        <th style="width:100px">작성자</th>
                        <th style="width:140px">작성일</th>
                        <th style="width:80px">조회</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="board" items="${boardList}">
                        <tr>
                            <td class="text-center">${board.id}</td>
                            <td>
                                <a href="/board/${board.id}" class="text-decoration-none text-dark fw-semibold">
                                    ${board.title}
                                </a>
                            </td>
                            <td class="text-center">${board.author}</td>
                            <td class="text-center">
                                <fmt:parseDate value="${board.createdAt}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedDate"/>
                                <fmt:formatDate value="${parsedDate}" pattern="yyyy-MM-dd HH:mm"/>
                            </td>
                            <td class="text-center">
                                <span class="badge bg-light text-dark">${board.viewCount}</span>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty boardList}">
                        <tr>
                            <td colspan="5" class="text-center py-5 text-muted">
                                <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                                게시글이 없습니다.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
    <div class="card-footer bg-white">
        <div class="row align-items-center">
            <div class="col">
                <!-- 페이징 -->
                <c:if test="${totalPages > 1}">
                    <nav>
                        <ul class="pagination pagination-sm mb-0">
                            <li class="page-item ${search.page <= 1 ? 'disabled' : ''}">
                                <a class="page-link" href="/board?page=${search.page - 1}&size=${search.size}&searchType=${search.searchType}&keyword=${search.keyword}">
                                    <i class="bi bi-chevron-left"></i>
                                </a>
                            </li>
                            <c:forEach begin="1" end="${totalPages}" var="i">
                                <li class="page-item ${search.page == i ? 'active' : ''}">
                                    <a class="page-link" href="/board?page=${i}&size=${search.size}&searchType=${search.searchType}&keyword=${search.keyword}">${i}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item ${search.page >= totalPages ? 'disabled' : ''}">
                                <a class="page-link" href="/board?page=${search.page + 1}&size=${search.size}&searchType=${search.searchType}&keyword=${search.keyword}">
                                    <i class="bi bi-chevron-right"></i>
                                </a>
                            </li>
                        </ul>
                    </nav>
                </c:if>
            </div>
            <div class="col-auto">
                <a href="/board/write" class="btn btn-dark btn-sm btn-write">
                    <i class="bi bi-pencil-square me-1"></i>글쓰기
                </a>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jsp"%>
