package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBucketResponse {
    private String label;
    private Double minInclusive;
    private Double maxExclusive;
    private Integer count;
}
