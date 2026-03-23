package com.example.githubaccessreport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Repository {
    private String name;
    
    @JsonProperty("full_name")
    private String fullName;
    
    // owner login can be extracted or we can just rely on the organization name
}
