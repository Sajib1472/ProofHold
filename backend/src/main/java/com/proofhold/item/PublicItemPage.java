package com.proofhold.item;

import java.util.List;

public record PublicItemPage(List<PublicItemResponse> content, PageInfo page) {}
