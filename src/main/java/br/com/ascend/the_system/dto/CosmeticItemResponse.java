package br.com.ascend.the_system.dto;
public record CosmeticItemResponse(String id,String category,String rarity,String name,String description,int cost,String previewUrl,boolean featured,boolean owned,boolean equipped,String slot) {}
