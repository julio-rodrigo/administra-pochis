package com.administrapochi.mappers;

import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentCreateRequest;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentResponse;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentUpdateRequest;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.AssignmentShiftOption;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.models.mappers.PochiAssignmentMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PochiAssignmentMapperTest {

    private final PochiAssignmentMapper mapper = Mappers.getMapper(PochiAssignmentMapper.class);

    @Test
    void createMappingUsesResolvedEntitiesAndDoesNotMapAuditFields() {
        PochiAssignmentCreateRequest request = new PochiAssignmentCreateRequest();
        request.setAssignmentDate(LocalDate.of(2026, 11, 10));
        request.setPochiId(82);
        request.setDriverId(3);
        request.setShift(AssignmentShiftOption.MORNING);
        request.setNotes("Shift note");
        Pochi pochi = Pochi.builder().id(82).build();
        Driver driver = Driver.builder().id(3).build();

        PochiAssignment assignment = mapper.toEntity(request, pochi, driver, Shift.MORNING);

        assertAll(
                () -> assertEquals(request.getAssignmentDate(), assignment.getAssignmentDate()),
                () -> assertEquals(pochi, assignment.getPochi()),
                () -> assertEquals(driver, assignment.getDriver()),
                () -> assertEquals(Shift.MORNING, assignment.getShift()),
                () -> assertEquals("Shift note", assignment.getNotes()),
                () -> assertNull(assignment.getId()),
                () -> assertNull(assignment.getCreatedAt()),
                () -> assertNull(assignment.getUpdatedAt()),
                () -> assertNull(assignment.getCreatedBy()),
                () -> assertNull(assignment.getUpdatedBy()));
    }

    @Test
    void responseMappingFlattensPochiDriverAndAuditUserDetails() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 11, 10, 8, 30);
        PochiAssignment assignment = PochiAssignment.builder()
                .id(1)
                .assignmentDate(LocalDate.of(2026, 11, 10))
                .shift(Shift.AFTERNOON)
                .pochi(Pochi.builder().id(82).economicNumber("P-82").build())
                .driver(Driver.builder()
                        .id(3).firstName("Ana").firstLastName("Lopez").secondLastName("Diaz").build())
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .createdBy(User.builder().id(4).name("Admin").build())
                .updatedBy(User.builder().id(5).name("Owner").build())
                .build();

        PochiAssignmentResponse response = mapper.toResponse(assignment);

        assertAll(
                () -> assertEquals("AFTERNOON", response.getShift()),
                () -> assertEquals(82, response.getPochiId()),
                () -> assertEquals("P-82", response.getPochiIdentifier()),
                () -> assertEquals(3, response.getDriverId()),
                () -> assertEquals("Ana Lopez Diaz", response.getDriverName()),
                () -> assertEquals(4, response.getCreatedById()),
                () -> assertEquals("Admin", response.getCreatedByName()),
                () -> assertEquals(5, response.getUpdatedById()),
                () -> assertEquals("Owner", response.getUpdatedByName()));
    }

    @Test
    void updateMappingPreservesIdRelationshipsAndAuditValues() {
        User creator = User.builder().id(4).name("Admin").build();
        PochiAssignment assignment = PochiAssignment.builder()
                .id(1)
                .assignmentDate(LocalDate.of(2026, 11, 10))
                .pochi(Pochi.builder().id(82).build())
                .driver(Driver.builder().id(3).build())
                .shift(Shift.MORNING)
                .createdBy(creator)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        PochiAssignmentUpdateRequest request = new PochiAssignmentUpdateRequest();
        request.setAssignmentDate(LocalDate.of(2026, 11, 11));
        request.setPochiId(99);
        request.setDriverId(100);
        request.setShift(Shift.AFTERNOON);
        request.setNotes("Corrected");

        mapper.updateEntity(request, assignment);

        assertAll(
                () -> assertEquals(1, assignment.getId()),
                () -> assertEquals(request.getAssignmentDate(), assignment.getAssignmentDate()),
                () -> assertEquals(Shift.AFTERNOON, assignment.getShift()),
                () -> assertEquals("Corrected", assignment.getNotes()),
                () -> assertEquals(82, assignment.getPochi().getId()),
                () -> assertEquals(3, assignment.getDriver().getId()),
                () -> assertEquals(creator, assignment.getCreatedBy()),
                () -> assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), assignment.getCreatedAt()));
    }
}
