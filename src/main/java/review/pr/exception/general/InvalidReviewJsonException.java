package review.pr.exception.general;

public class InvalidReviewJsonException extends RuntimeException{
    public InvalidReviewJsonException(String message) {
        super(message);
    }
}
