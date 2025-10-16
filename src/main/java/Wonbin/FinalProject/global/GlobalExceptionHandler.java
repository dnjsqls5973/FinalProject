package Wonbin.FinalProject.global;

import Wonbin.FinalProject.ai.diary.exception.DiaryNotFoundException;
import Wonbin.FinalProject.ai.diary.exception.InvalidMoodException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation Failed", "details", errors));
    }

    /**
     * Diary 관련 예외 처리
     */
    @ExceptionHandler(DiaryNotFoundException.class)
    public ResponseEntity<?> handleDiaryNotFoundException(DiaryNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "DiaryNotFound", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidMoodException.class)
    public ResponseEntity<?> handleInvalidMoodException(InvalidMoodException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "InvalidMood", "message", e.getMessage()));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BadRequest", "message", e.getMessage()));
    }

    /**
     * 전역 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
    }
}
