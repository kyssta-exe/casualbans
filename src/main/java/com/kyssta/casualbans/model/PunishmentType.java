package com.kyssta.casualbans.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PunishmentType {
    BAN("ban", "banned", true),
    TEMPBAN("tempban", "tempbanned", true),
    IPBAN("ipban", "IP-banned", true),
    MUTE("mute", "muted", false),
    TEMPMUTE("tempmute", "tempmuted", false),
    IPMUTE("ipmute", "IP-muted", false),
    WARN("warn", "warned", false),
    KICK("kick", "kicked", false),
    UNBAN("unban", "unbanned", true),
    UNMUTE("unmute", "unmuted", false),
    UNWARN("unwarn", "unwarned", false);

    private final String commandName;
    private final String pastTense;
    private final boolean kickOnCreate;

    public boolean isBan() {
        return this == BAN || this == TEMPBAN || this == IPBAN;
    }

    public boolean isMute() {
        return this == MUTE || this == TEMPMUTE || this == IPMUTE;
    }

    public boolean isWarn() {
        return this == WARN;
    }

    public boolean isKick() {
        return this == KICK;
    }

    public boolean isIP() {
        return this == IPBAN || this == IPMUTE;
    }

    public boolean isTemp() {
        return this == TEMPBAN || this == TEMPMUTE;
    }

    public boolean isRemoval() {
        return this == UNBAN || this == UNMUTE || this == UNWARN;
    }

    public PunishmentType getBaseType() {
        if (isBan()) return BAN;
        if (isMute()) return MUTE;
        if (isWarn()) return WARN;
        if (isKick()) return KICK;
        return this;
    }
}
