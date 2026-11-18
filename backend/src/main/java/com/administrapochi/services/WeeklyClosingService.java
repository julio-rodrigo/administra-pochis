package com.administrapochi.services;

import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCreateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCommentUpdateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingReopenRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingResponse;
import com.administrapochi.models.enums.WeeklyClosingStatus;

import java.util.List;

public interface WeeklyClosingService {

    WeeklyClosingResponse create(WeeklyClosingCreateRequest request);

    WeeklyClosingResponse closeByAdmin(Integer id);

    WeeklyClosingResponse closeByOwner(Integer id);

    WeeklyClosingResponse reopen(Integer id, WeeklyClosingReopenRequest request);

    WeeklyClosingResponse updateComment(Integer id, WeeklyClosingCommentUpdateRequest request);

    WeeklyClosingResponse findById(Integer id);

    List<WeeklyClosingResponse> findAll(
            Integer ownerId, Integer weekYear, Integer weekNumber, WeeklyClosingStatus status);
}
