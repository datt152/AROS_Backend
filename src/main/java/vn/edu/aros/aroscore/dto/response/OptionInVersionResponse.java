package vn.edu.aros.aroscore.dto.response;

import lombok.Data;

@Data
public class OptionInVersionResponse {
    private String label; // A, B, C, D...
    private String content;
}