package br.com.ascend.the_system.dto;
import java.util.List;
public record ShopStateResponse(int coins, List<ShopReceiptResponse> purchases, List<ShopOfferResponse> offers) {}
