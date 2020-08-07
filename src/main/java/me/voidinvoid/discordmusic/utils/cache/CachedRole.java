package me.voidinvoid.discordmusic.utils.cache;

import me.voidinvoid.discordmusic.Radio;
import net.dv8tion.jda.api.entities.Role;

import java.util.Objects;

public class CachedRole implements ICached<Role> {

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
        return Radio.getInstance().getJda().getRoleById(roleId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CachedRole && Objects.equals(roleId, ((CachedRole) obj).roleId);
    }
}
