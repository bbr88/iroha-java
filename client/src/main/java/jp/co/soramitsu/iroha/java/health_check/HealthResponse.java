package jp.co.soramitsu.iroha.java.health_check;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthResponse {
    @JsonProperty("memory_consumption")
    long memoryConsumption;
    @JsonProperty("last_block_round")
    long lastBlockRound;
    @JsonProperty("last_reject_round")
    long lastRejectRound;
    @JsonProperty("is_syncing")
    boolean isSyncing;
    boolean status;

    public static HealthResponse unhealthy() {
        return new HealthResponse(0L, 0L, 0L, false, false);
    }
}
