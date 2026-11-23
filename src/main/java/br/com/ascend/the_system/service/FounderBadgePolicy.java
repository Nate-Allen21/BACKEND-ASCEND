package br.com.ascend.the_system.service;

public final class FounderBadgePolicy {
 private FounderBadgePolicy() {}
 public static boolean isFounder(Long userId) { return userId != null && (userId == 2L || userId == 8L); }
}
