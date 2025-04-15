package com.springboot.lab03.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {
    private List<T> items; // 데이터 목록
    private int itemsPerPage; // 페이지당 항목 수
    private int page; // 현재 페이지 번호
    private long totalItems; // 전체 항목 수
    private int totalPages; // 전체 페이지 수
} 