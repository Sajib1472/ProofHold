package com.proofhold.audit;

import com.proofhold.item.PageInfo;

import java.util.List;

public record AuditPage(List<AuditEventResponse> content, PageInfo page) {}
