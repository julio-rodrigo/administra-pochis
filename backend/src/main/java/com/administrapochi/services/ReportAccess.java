package com.administrapochi.services;

import com.administrapochi.exceptions.ReportAccessDeniedException;
import com.administrapochi.exceptions.UserNotFoundException;
import com.administrapochi.models.enums.UserRole;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import com.administrapochi.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class ReportAccess {

    private static final Integer TEMPORARY_USER_ID = 2;
    private final UserRepository userRepository;

    Scope resolve(Integer requestedOwnerId) {
        var user = userRepository.findById(TEMPORARY_USER_ID)
                .orElseThrow(() -> new UserNotFoundException(TEMPORARY_USER_ID));
        if (user.getRole() == UserRole.ADMIN) {
            return new Scope(requestedOwnerId, true);
        }
        if (user.getRole() != UserRole.OWNER
                || (requestedOwnerId != null && !requestedOwnerId.equals(user.getId()))) {
            throw new ReportAccessDeniedException("Reports for another Owner are not accessible.");
        }
        return new Scope(user.getId(), false);
    }

    record Scope(Integer ownerId, boolean admin) {
        List<WeeklyClosingStatus> visibleStatuses() {
            return admin ? List.of(WeeklyClosingStatus.values())
                    : List.of(WeeklyClosingStatus.ADMIN_CLOSED, WeeklyClosingStatus.OWNER_CLOSED);
        }

        void requireVisible(WeeklyClosingStatus status) {
            if (!visibleStatuses().contains(status)) {
                throw new ReportAccessDeniedException("An OPEN Weekly Closing is not accessible to the Owner.");
            }
        }
    }
}
