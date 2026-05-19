<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>게시판</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <style>
        body { background-color: #f8f9fa; }
        .navbar-brand { font-weight: 700; }
        .content-wrapper { min-height: calc(100vh - 120px); }
        .card { border: none; box-shadow: 0 0.125rem 0.25rem rgba(0,0,0,0.075); }
        .table th { background-color: #f1f3f5; white-space: nowrap; }
        .btn-write { min-width: 100px; }
        .pagination .page-link { color: #495057; }
        .pagination .page-item.active .page-link { background-color: #495057; border-color: #495057; }
        footer { background-color: #343a40; }
    </style>
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container">
        <a class="navbar-brand" href="/board">
            <i class="bi bi-clipboard2-data me-2"></i>게시판
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <a class="nav-link" href="/board"><i class="bi bi-list-ul me-1"></i>목록</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/board/write"><i class="bi bi-pencil-square me-1"></i>글쓰기</a>
                </li>
            </ul>
        </div>
    </div>
</nav>
<div class="content-wrapper">
    <div class="container py-4">
        <c:if test="${not empty message}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>
