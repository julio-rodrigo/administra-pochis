package com.administrapochi.services;

import com.administrapochi.models.entities.PochiAssignment;

public interface PochiAssignmentReferencePolicy {

    boolean isReferenced(PochiAssignment assignment);
}
