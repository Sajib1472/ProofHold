package com.proofhold.claim;

import com.proofhold.item.PageInfo;

import java.util.List;

public record ClaimListPage(List<ClaimResponse> content, PageInfo page) {}
