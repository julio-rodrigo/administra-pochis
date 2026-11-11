package com.administrapochi.services;

import com.administrapochi.models.entities.PochiAssignment;
import org.springframework.stereotype.Component;

/**
 * Temporary policy until Settlements or other assignment-referencing records are introduced.
 */
@Component
public class NoPochiAssignmentReferencesPolicy implements PochiAssignmentReferencePolicy {

    @Override
    public boolean isReferenced(PochiAssignment assignment) {
        return false;
    }
}
