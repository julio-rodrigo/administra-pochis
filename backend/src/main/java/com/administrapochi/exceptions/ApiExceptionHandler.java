package com.administrapochi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PochiNotFoundException.class)
    public ProblemDetail handlePochiNotFound(PochiNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({SettlementRuleNotFoundException.class, NoApplicableSettlementRuleException.class})
    public ProblemDetail handleSettlementRuleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({DuplicateEconomicNumberException.class, DuplicateLicensePlateException.class})
    public ProblemDetail handlePochiConflict(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ProblemDetail handleUsernameAlreadyExists(UsernameAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DriverNotFoundException.class)
    public ProblemDetail handleDriverNotFound(DriverNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PochiAssignmentNotFoundException.class)
    public ProblemDetail handlePochiAssignmentNotFound(PochiAssignmentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(SettlementNotFoundException.class)
    public ProblemDetail handleSettlementNotFound(SettlementNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DriverLicenseNotFoundException.class)
    public ProblemDetail handleDriverLicenseNotFound(DriverLicenseNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DriverAbsenceNotFoundException.class)
    public ProblemDetail handleDriverAbsenceNotFound(DriverAbsenceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({
            DuplicateCurpException.class,
            DriverDeletedException.class,
            DuplicateDriverLicenseNumberException.class,
            DriverMustBeActiveException.class,
            SettlementRuleOverlapException.class,
            PochiAssignmentConflictException.class,
            PochiAssignmentPeriodClosedException.class,
            PochiAssignmentReferencedException.class,
            PochiAssignmentDriverInactiveException.class,
            PochiAssignmentPochiInactiveException.class,
            PochiAssignmentAlreadySettledException.class
    })
    public ProblemDetail handleDriverConflict(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({
            InvalidDriverLicenseDateRangeException.class,
            InvalidDriverAbsenceDateRangeException.class,
            InvalidSettlementRuleDateRangeException.class,
            InvalidPochiAssignmentFilterException.class,
            InvalidSettlementFilterException.class,
            InvalidSettlementException.class
    })
    public ProblemDetail handleDriverDateValidation(RuntimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed");
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        problem.setProperty("errors", errors);
        return problem;
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
