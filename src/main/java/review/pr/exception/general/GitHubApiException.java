package review.pr.exception.general;

public class GitHubApiException extends RuntimeException{
    public GitHubApiException(String message){
        super(message);
    }
}
