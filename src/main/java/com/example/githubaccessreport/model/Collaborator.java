package com.example.githubaccessreport.model;

import lombok.Data;

@Data
public class Collaborator {
    private String login;
    private Permissions permissions;

    @Data
    public static class Permissions {
        private boolean admin;
        private boolean push;
        private boolean pull;
    }
    
    public String getRole() {
        if (permissions == null) return "none";
        if (permissions.isAdmin()) return "admin";
        if (permissions.isPush()) return "write";
        if (permissions.isPull()) return "read";
        return "none";
    }
}
