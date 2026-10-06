package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OmrBubbleResponse {
    private String choice;
    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;
    /** green = ô tô đúng, red = ô tô sai / tô trùng */
    private String tone;
}
