package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.*;
import br.com.ascend.the_system.dto.*;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.exception.ConflictException;
import br.com.ascend.the_system.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ShopService {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private final ShopPurchaseRepository purchases;
    private final UserRepository users;
    private final VaultTransactionRepository transactions;

    public ShopService(ShopPurchaseRepository purchases, UserRepository users, VaultTransactionRepository transactions) {
        this.purchases=purchases; this.users=users; this.transactions=transactions;
    }

    @Transactional(readOnly=true)
    public ShopStateResponse getState(User user) {
        return state(users.findById(user.getId()).orElseThrow());
    }

    @Transactional
    public ShopStateResponse purchase(User authenticatedUser, ShopPurchaseRequest request) {
        if (request == null || request.itemId() == null) throw new BadRequestException("Selecione um item da loja.");
        ShopCatalog.Product product = ShopCatalog.get(request.itemId());
        if (product == null) throw new BadRequestException("Item da loja inexistente.");
        LocalDate today = LocalDate.now(ZONE);
        if (ShopCatalog.offers(today).stream().noneMatch(p -> p.id().equals(product.id()))) {
            throw new BadRequestException("Esse item nao esta disponivel na loja de hoje.");
        }
        // Lock avoids two simultaneous requests spending the same balance.
        User user = users.findByIdForUpdate(authenticatedUser.getId()).orElseThrow();
        if (purchases.existsByUser_IdAndItemIdAndDay(user.getId(), product.id(), today)) {
            throw new ConflictException("Este item ja foi resgatado hoje.");
        }
        if (user.getCoins() < product.cost()) throw new BadRequestException("Moedas insuficientes.");
        user.spendCoins(product.cost());
        ShopPurchase purchase = new ShopPurchase(user, product.id(), product.name(), product.cost(), today);
        purchases.saveAndFlush(purchase);
        transactions.save(new VaultTransaction(user, VaultTransaction.VaultTransactionType.EXPENSE, "Loja: " + product.name(), product.cost()));
        return state(user);
    }

    private ShopStateResponse state(User user) {
        LocalDate today=LocalDate.now(ZONE);
        return new ShopStateResponse(user.getCoins(), history(user.getId()), ShopCatalog.offers(today).stream()
            .map(p -> new ShopOfferResponse(p.id(), p.name(), p.cost(), p.id()+"-"+today)).toList());
    }

    private List<ShopReceiptResponse> history(Long userId) {
        return purchases.findTop200ByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(p -> new ShopReceiptResponse(p.getId(), p.getItemId(), p.getItemId()+"-"+p.getDay(), p.getName(), p.getCost(), p.getDay(), p.getCreatedAt()))
                .toList();
    }
}
