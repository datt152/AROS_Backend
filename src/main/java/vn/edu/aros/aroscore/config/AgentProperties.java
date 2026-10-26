package vn.edu.aros.aroscore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.agent")
public class AgentProperties {

    /** Số vòng tool-calling tối đa trong một lần chạy agent. */
    private int maxSteps = 6;

    /** Số bài nộp tối thiểu trước khi cho chạy phân tích lớp. */
    private int minSubmitted = 1;
}
