package review.pr.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import review.pr.services.AIServiceHelper;

import java.util.Map;

import static review.pr.constants.AppConstants.*;

@RestController
@RequestMapping(BASE_ENDPOINT)
public class SpringAIController {
    @Autowired
    private AIServiceHelper aiServiceHelper;

    @PostMapping(REVIEW_ENDPOINT)
    public ResponseEntity<String> reviewPR(
            @RequestBody Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {
        String response = aiServiceHelper.reviewPR(payload,headers);
        return ResponseEntity.ok(response);
    }
}