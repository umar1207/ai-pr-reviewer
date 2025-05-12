package review.pr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import review.pr.exception.general.*;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DiffFetchException.class)
    public ResponseEntity<String> handleDiffFetchException(DiffFetchException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<String> handleGitHubApiException(GitHubApiException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(InvalidReviewJsonException.class)
    public ResponseEntity<String> handleInvalidReviewJsonException(InvalidReviewJsonException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(MissingPullRequestDataException.class)
    public ResponseEntity<String> handleMissingPullRequestDataException(MissingPullRequestDataException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedGitHubEventException.class)
    public ResponseEntity<String> handleUnsupportedGitHubEventException(UnsupportedGitHubEventException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
