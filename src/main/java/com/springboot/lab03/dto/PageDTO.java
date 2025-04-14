package com.springboot.lab03.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {
    private List<T> items; // 현재 페이지의 데이터 목록
    private int itemsPerPage; // 페이지당 항목 수
    private int page; // 현재 페이지 번호 (API 응답 기준, 0 또는 1부터 시작할 수 있음)
    private long totalItems; // 전체 항목 수
    private int totalPages; // 전체 페이지 수
} 