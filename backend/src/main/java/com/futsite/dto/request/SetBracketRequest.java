package com.futsite.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class SetBracketRequest {
    /** Map of bracket position -> team ID (manual seeding), or empty for random draw */
    private Map<Integer, Long> bracketSeeding;

    /** If true, ignore bracketSeeding and randomize */
    private boolean randomDraw;
}
