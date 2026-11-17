package com.administrapochi.mappers;

import com.administrapochi.models.entities.Expense;
import com.administrapochi.models.entities.ExpenseType;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.ExpenseStatus;
import com.administrapochi.models.mappers.ExpenseMapperImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseMapperTest {

    private final ExpenseMapperImpl mapper = new ExpenseMapperImpl();

    @Test
    void mapsNullablePochiAndCancellationFieldsSafely() {
        Expense expense = Expense.builder()
                .expenseType(ExpenseType.builder().id(4).code("REPAIR").name("Repair").build())
                .paidByUser(User.builder().id(2).name("Temporary").build())
                .registeredByUser(User.builder().id(2).name("Temporary").build())
                .status(ExpenseStatus.ACTIVE)
                .build();

        var response = mapper.toResponse(expense);

        assertThat(response.getExpenseTypeId()).isEqualTo(4);
        assertThat(response.getExpenseTypeCode()).isEqualTo("REPAIR");
        assertThat(response.getPochiId()).isNull();
        assertThat(response.getPochiIdentifier()).isNull();
        assertThat(response.getCancelledByUserId()).isNull();
        assertThat(response.getCancelledByUserName()).isNull();
        assertThat(response.getCancellationReason()).isNull();
        assertThat(response.getCancelledAt()).isNull();
    }
}
