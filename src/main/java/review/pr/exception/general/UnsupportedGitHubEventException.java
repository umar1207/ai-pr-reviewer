package review.pr.exception.general;

public class UnsupportedGitHubEventException extends RuntimeException{
    public UnsupportedGitHubEventException(String message) {
        super(message);
    }
}
