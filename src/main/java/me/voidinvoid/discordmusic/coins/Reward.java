package me.voidinvoid.discordmusic.coins;

import net.dv8tion.jda.api.entities.Member;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DiscordMusic - 28/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class Reward {

    private final List<String> eligibleMembers;
    private final double baseRewardMultiplier;

    public Reward(List<Member> eligibleMembers, double baseRewardMultiplier) {

        this.eligibleMembers = eligibleMembers.stream().map(Member::getId).collect(Collectors.toList());
        this.baseRewardMultiplier = baseRewardMultiplier;
    }

    public boolean isEligible(Member member) {
        return eligibleMembers.contains(member.getUser().getId());
    }

    public double getBaseRewardMultiplier() {
        return baseRewardMultiplier;
    }
}