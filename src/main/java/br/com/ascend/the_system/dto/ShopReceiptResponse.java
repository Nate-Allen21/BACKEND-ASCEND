package br.com.ascend.the_system.dto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record ShopReceiptResponse(Long id, String itemId, String dailyId, String name, int cost, LocalDate day, OffsetDateTime createdAt) {}
