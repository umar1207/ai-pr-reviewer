package review.pr.exception.general;

public class MissingPullRequestDataException extends RuntimeException{
    public MissingPullRequestDataException(String message) {
        super(message);
    }
}
