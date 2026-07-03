package com.kyssta.casualbans.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffNote {
    private long id;
    private UUID targetUUID;
    private String targetName;
    private String note;
    private UUID authorUUID;
    private String authorName;
    private long timestamp;
}
