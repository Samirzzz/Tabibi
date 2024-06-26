package com.tabibi.tabibi_system.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.tabibi.tabibi_system.Models.Feedback;

@Service
public class FeedbackService {
    private RestTemplate restTemplate;
    private String baseUrl = "http://localhost:8083";
    
    FeedbackService()
    {
        this.restTemplate = new RestTemplate(); 

    }
     public List<Feedback> findAll() {
        String url = baseUrl + "/feedback";
        return this.restTemplate.exchange(
                url, // Endpoint URL
                HttpMethod.GET, // HTTP request method
                null, // Request body
                new ParameterizedTypeReference<List<Feedback>>() {
                }).getBody(); // Response body converted to List<Post>
    }

    public void save(Feedback feedback){
        String url = baseUrl + "/feedback/add";
        this.restTemplate.postForObject(url, feedback, Feedback.class);
    } 

   public void delete(Integer id)
 {
    String url = baseUrl + "/feedback/delete";
    Map<String, Integer> requestBody = new HashMap<>();
    requestBody.put("id", id);
    restTemplate.postForObject(url, requestBody, Void.class);
}

}
