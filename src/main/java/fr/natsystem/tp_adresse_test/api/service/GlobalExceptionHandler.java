package fr.natsystem.tp_adresse_test.api.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.natsystem.tp_adresse_test.api.dto.BatchLaunchResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BatchExecutionNotFoundException.class)
    public ResponseEntity<BatchLaunchResponse> handleBatchExecutionNotFound(
            BatchExecutionNotFoundException ex) {

        BatchLaunchResponse response =
                new BatchLaunchResponse(
                        ex.getJobExecutionId(),
                        "NOT_FOUND"
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
}
