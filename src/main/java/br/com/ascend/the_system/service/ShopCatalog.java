package br.com.ascend.the_system.service;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/** Preços controlados pelo servidor; o cliente nunca define o custo. */
public final class ShopCatalog {
    public record Product(String id, String name, int cost) {}
    private static final Map<String, Product> PRODUCTS;
    static {
        Map<String, Product> products = new LinkedHashMap<>();
        products.put("pausa-estrategica", new Product("pausa-estrategica", "Pausa estrategica", 120));
        products.put("equipamento-treino", new Product("equipamento-treino", "Equipamento de treino", 300));
        products.put("sessao-premium", new Product("sessao-premium", "Sessao premium", 200));
        products.put("combo-hidratacao", new Product("combo-hidratacao", "Combo de hidratacao", 90));
        products.put("almoco-livre", new Product("almoco-livre", "Almoco livre", 240));
        products.put("kit-estudos", new Product("kit-estudos", "Kit de estudos", 180));
        products.put("cinema-noturno", new Product("cinema-noturno", "Cinema noturno", 260));
        products.put("playlist-foco", new Product("playlist-foco", "Playlist de foco", 110));
        products.put("micro-upgrade-setup", new Product("micro-upgrade-setup", "Micro upgrade no setup", 320));
        products.put("dia-social", new Product("dia-social", "Encontro social", 170));
        products.put("lanche-pos-treino", new Product("lanche-pos-treino", "Lanche pos-treino", 140));
        products.put("cafe-deep-work", new Product("cafe-deep-work", "Cafe deep work", 95));
        products.put("livro-novo", new Product("livro-novo", "Livro novo", 280));
        products.put("acessorio-academia", new Product("acessorio-academia", "Acessorio de academia", 260));
        products.put("sobremesa-planejada", new Product("sobremesa-planejada", "Sobremesa planejada", 130));
        products.put("bloco-criativo", new Product("bloco-criativo", "Bloco criativo", 150));
        products.put("upgrade-escrivaninha", new Product("upgrade-escrivaninha", "Upgrade na escrivaninha", 340));
        products.put("role-curto", new Product("role-curto", "Role curto", 160));
        products.put("kit-recuperacao", new Product("kit-recuperacao", "Kit recuperacao", 145));
        products.put("noite-game", new Product("noite-game", "Noite game", 220));
        products.put("mentoria-pessoal", new Product("mentoria-pessoal", "Mentoria pessoal", 175));
        products.put("camiseta-treino", new Product("camiseta-treino", "Camiseta de treino", 310));
        products.put("saida-com-amigos", new Product("saida-com-amigos", "Saida com amigos", 290));
        products.put("reliquia-ascendente", new Product("reliquia-ascendente", "Reliquia ascendente", 777));
        PRODUCTS = Collections.unmodifiableMap(products);
    }
    private ShopCatalog() {}
    public static Product get(String id) { return PRODUCTS.get(id); }
    public static java.util.Collection<Product> all() { return PRODUCTS.values(); }
    // Mesmo algoritmo de rotacao diaria utilizado pelo frontend web.
    public static List<Product> offers(LocalDate today) {
        long seed = Long.parseLong(today.toString().replace("-", "")) + 7L * 97;
        List<Product> pool = new ArrayList<>(PRODUCTS.values());
        pool.removeIf(p -> p.id().equals("reliquia-ascendente"));
        pool.sort(Comparator.comparingLong(p -> {
            int index = new ArrayList<>(PRODUCTS.keySet()).indexOf(p.id());
            long weight = (seed * 31) + (index * 17L) + (7L * 13) + ((long) jsHash(p.id()) * 19);
            return Math.abs(weight % 997);
        }));
        List<Product> offers = new ArrayList<>();
        offers.add(PRODUCTS.get("reliquia-ascendente"));
        offers.addAll(pool.subList(0, Math.min(8,pool.size())));
        return offers;
    }
    static int jsHash(String id) {
        int hash=0;
        for(int i=0;i<id.length();i++) hash=31*hash+id.charAt(i);
        return hash;
    }
}
