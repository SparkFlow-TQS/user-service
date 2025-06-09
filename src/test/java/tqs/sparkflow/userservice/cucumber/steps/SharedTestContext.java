package tqs.sparkflow.userservice.cucumber.steps;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Shared context for Cucumber step definitions to share state between different step classes.
 */
@Component
public class SharedTestContext {
    
    private ResponseEntity<String> lastResponse;
    
    public void setLastResponse(ResponseEntity<String> response) {
        this.lastResponse = response;
    }
    
    public ResponseEntity<String> getLastResponse() {
        return this.lastResponse;
    }
    
    public void clear() {
        this.lastResponse = null;
    }
}