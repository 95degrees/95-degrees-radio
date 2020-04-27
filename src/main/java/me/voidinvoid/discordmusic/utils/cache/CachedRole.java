package me.voidinvoid.discordmusic.utils.cache;

import cf.ninetyfivedegrees.guardian.GuardianBot;
import net.dv8tion.jda.api.entities.Role;

public class CachedRole implements ICached {

    private String roleId;

    public CachedRole(String roleId) {

        this.roleId = roleId;
    }

    public CachedRole(Role role) {

        this.roleId = role.getId();
    }

    public String getId() {
        return roleId;
    }

    public Role get() {
        return GuardianBot.getInstance().getJda().getRoleById(roleId);
    }
}
