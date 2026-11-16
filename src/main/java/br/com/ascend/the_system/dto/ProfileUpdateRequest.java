package br.com.ascend.the_system.dto;

import java.util.List;

public record ProfileUpdateRequest(String bio, List<String> tags, String visibility, String profileImage, String name) {
    public ProfileUpdateRequest(String bio, List<String> tags, String visibility, String profileImage) {
        this(bio, tags, visibility, profileImage, null);
    }
}
