package review.pr.constants;

public class AppConstants {
    public static final String BASE_ENDPOINT = "/api/v1";
    public static final String REVIEW_ENDPOINT = "/review/pr";
    public static final String INITIAL_PROMPT = "You are a PR Code Reviewer. Respond with a JSON object like {\"filename\": \"review comment\"}. Here's the git diff:\n";
    public static final String X_GITHUB_EVENT = "x-github_event";
    public static final String PULL_REQUEST = "pull_request";
    public static final String OPENED = "opened";
    public static final String HEAD = "head";
    public static final String ACTION = "action";
    public static final String DIFF_URL = "diff_url";
    public static final String PR_OPENED_MESSAGE = "Pull request opened. Reviews Posted";
    public static final String FAILURE_MESSAGE = "Pull request opened but no diff_url found.";
    public static final String PR_CLOSED_MESSAGE = "Pull Request Closed";
    public static final String UNKNOWN_EVENT = "Unknown Event";
    public static final String DIFF_FETCH_FAIL_MESSAGE = "Failed to fetch diff: ";
    public static final String JSON_CHECK = "```json";
    public static final String JSON_REGEX = "(?s)```json\\s*(.*?)\\s*```";
    public static final String JSON_REPLACEMENT = "$1";
    public static final String INVALID_JSON = "Invalid JSON received: ";
    public static final String SHA = "sha";
    public static final String URL = "url";
    public static final String COMMENTS_ENDPOINT = "/comments";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_VALUE = "application/vnd.github+json";
    public static final String BODY = "body";
    public static final String COMMIT_ID = "commit_id";
    public static final String PATH = "path";
    public static final String POSITION = "position";
    public static final String GITHUB_API_FAILURE = "Failed to post review comment: ";
}
