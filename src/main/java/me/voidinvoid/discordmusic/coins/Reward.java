package me.voidinvoid.discordmusic.coins;

import me.voidinvoid.discordmusic.utils.cache.CachedMember;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DiscordMusic - 28/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class Reward {

    private final List<CachedMember> eligibleMembers;
    private final List<String> claimants = new ArrayList<>();
    private final double baseRewardMultiplier;

    public Reward(List<CachedMember> eligibleMembers, double baseRewardMultiplier) {

        this.eligibleMembers = eligibleMembers;
        this.baseRewardMultiplier = baseRewardMultiplier;
    }

    public boolean isEligible(Member member) {
        return eligibleMembers.contains(new CachedMember(member.getUser()));
    }

    public boolean hasAlreadyClaimed(Member member) {
        return claimants.contains(member.getUser().getId());
    }

    public void markClaimed(Member member) {
        claimants.add(member.getUser().getId());
    }

    public double getBaseRewardMultiplier() {
        return baseRewardMultiplier;
    }
}
