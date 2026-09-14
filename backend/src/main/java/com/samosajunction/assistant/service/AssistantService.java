package com.samosajunction.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.samosajunction.assistant.config.AssistantProperties;
import com.samosajunction.assistant.dto.AssistantChatRequest;
import com.samosajunction.assistant.dto.AssistantChatResponse;
import com.samosajunction.assistant.dto.AssistantChatResponse.AssistantProductSuggestion;
import com.samosajunction.assistant.dto.AssistantHistoryMessage;
import com.samosajunction.assistant.openrouter.OpenRouterClient;
import com.samosajunction.cart.dto.CartItemResponse;
import com.samosajunction.cart.dto.CartResponse;
import com.samosajunction.cart.service.CartService;
import com.samosajunction.common.exception.ApiException;
import com.samosajunction.order.dto.CreateOrderRequest;
import com.samosajunction.order.dto.DeliveryAddressRequest;
import com.samosajunction.order.dto.OrderResponse;
import com.samosajunction.order.service.OrderService;
import com.samosajunction.payment.entity.PaymentMethod;
import com.samosajunction.product.dto.ProductResponse;
import com.samosajunction.product.dto.ProductSearchCriteria;
import com.samosajunction.product.service.ProductService;
import com.samosajunction.recommendation.dto.RecommendationItemResponse;
import com.samosajunction.recommendation.service.RecommendationService;
import com.samosajunction.user.dto.UserResponse;
import com.samosajunction.user.service.UserService;
import com.samosajunction.wallet.service.WalletService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// OpenRouter tool agent: menu search, cart, wallet, and checkout with Java validation.
@Service
public class AssistantService {

    private static final Pattern ORDER_QTY = Pattern.compile(
            "(?:order|add|get)\\s+(\\d+)\\s+(.+)",
            Pattern.CASE_INSENSITIVE
    );

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final WalletService walletService;
    private final RecommendationService recommendationService;
    private final UserService userService;
    private final SemanticMenuSearch semanticMenuSearch;
    private final OpenRouterClient openRouterClient;
    private final AssistantProperties properties;
    private final ObjectMapper objectMapper;

    public AssistantService(
            ProductService productService,
            CartService cartService,
            OrderService orderService,
            WalletService walletService,
            RecommendationService recommendationService,
            UserService userService,
            SemanticMenuSearch semanticMenuSearch,
            OpenRouterClient openRouterClient,
            AssistantProperties properties,
            ObjectMapper objectMapper
    ) {
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.walletService = walletService;
        this.recommendationService = recommendationService;
        this.userService = userService;
        this.semanticMenuSearch = semanticMenuSearch;
        this.openRouterClient = openRouterClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AssistantChatResponse chat(UUID userId, AssistantChatRequest request) {
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isBlank()) {
            return AssistantChatResponse.text("Tell me a samosa you want, or ask about your order or wallet.");
        }
        if (properties.openRouterEnabled()) {
            try {
                return chatWithLlm(userId, message, request.safeHistory());
            } catch (RuntimeException ex) {
                AssistantChatResponse fallback = chatHeuristic(userId, message);
                return new AssistantChatResponse(
                        fallback.reply() + " (Live AI had a hiccup; used the local menu tools instead.)",
                        fallback.products()
                );
            }
        }
        return chatHeuristic(userId, message);
    }

    /** Kept for unit tests and OpenRouter-off local mode. */
    public AssistantChatResponse chat(UUID userId, String raw) {
        return chat(userId, new AssistantChatRequest(raw, List.of()));
    }

    private AssistantChatResponse chatWithLlm(UUID userId, String message, List<AssistantHistoryMessage> history) {
        Map<UUID, ProductResponse> suggested = new LinkedHashMap<>();
        ArrayNode tools = toolDefinitions();
        ArrayNode messages = objectMapper.createArrayNode();
        messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt(userId));
        for (AssistantHistoryMessage turn : history) {
            String role = turn.role() == null ? "" : turn.role().trim().toLowerCase(Locale.ROOT);
            if (!role.equals("user") && !role.equals("assistant")) {
                continue;
            }
            messages.addObject().put("role", role).put("content", turn.content());
        }
        messages.addObject().put("role", "user").put("content", message);

        int rounds = Math.max(1, properties.maxToolRounds());
        for (int round = 0; round < rounds; round++) {
            JsonNode completion = openRouterClient.chat(messages, tools);
            JsonNode choice = completion.path("choices").path(0).path("message");
            if (choice.isMissingNode() || choice.isNull()) {
                break;
            }
            messages.add(choice.deepCopy());
            JsonNode toolCalls = choice.path("tool_calls");
            if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                String reply = choice.path("content").asText("").trim();
                if (reply.isBlank()) {
                    reply = "I checked the live menu and your account. Ask me to recommend, add to cart, or place an order.";
                }
                return new AssistantChatResponse(reply, suggested.values().stream().map(AssistantService::suggestion).toList());
            }
            for (JsonNode call : toolCalls) {
                String id = call.path("id").asText(UUID.randomUUID().toString());
                String name = call.path("function").path("name").asText("");
                String argsJson = call.path("function").path("arguments").asText("{}");
                String result = executeTool(userId, name, argsJson, suggested);
                messages.addObject()
                        .put("role", "tool")
                        .put("tool_call_id", id)
                        .put("name", name)
                        .put("content", result);
            }
        }
        return new AssistantChatResponse(
                "I hit the tool limit before finishing. Try a shorter ask, or open Cart / Orders.",
                suggested.values().stream().map(AssistantService::suggestion).toList()
        );
    }

    private String systemPrompt(UUID userId) {
        StringBuilder catalog = new StringBuilder();
        for (ProductResponse product : semanticMenuSearch.availableCatalog()) {
            catalog.append("- id=").append(product.id())
                    .append(" | ").append(product.name())
                    .append(" | ₹").append(product.price())
                    .append(" | stock=").append(product.stock())
                    .append(" | ").append(product.description())
                    .append(" | tags=").append(product.dietaryTags())
                    .append(" | protein=").append(product.protein()).append("g")
                    .append('\n');
        }
        var wallet = walletService.getWallet(userId);
        var cart = cartService.getCart(userId);
        UserResponse profile = userService.getCurrentUser(userId);
        String addressHint = profile.hasDefaultAddress()
                ? "Default delivery is the address saved on Profile: "
                + profile.recipientName() + ", " + profile.addressLine1()
                + ", " + profile.city() + " " + profile.pincode()
                + (profile.phone() == null ? "" : ". Phone " + profile.phone())
                : "No default address on Profile. Refuse place_order until they save one at /profile.";

        return """
                You are Samosa AI for Samosa Junction, a real food-ordering app.
                Use tools for facts. Never invent prices, stock, wallet balance, orders, or addresses.
                Live menu (available only):
                %s
                Wallet balance: ₹%s. Cart lines: %d, subtotal ₹%s. %s
                Rules:
                - Prefer search_menu for taste/intent questions (paneer lover, high protein, mild, etc.). It is semantic.
                - Before adding, check stock from tools. If they ask for 5 and stock is 1, do NOT add 5 and do NOT silently add 1. Tell them only 1 is in stock, they may order 1 if they confirm, and offer another item that has stock.
                - add_to_cart only puts items in the cart. Never claim an order was placed unless place_order returned placed=true.
                - When they want to buy/pay now: add_to_cart (if needed), then place_order. place_order validates stock, wallet, and Profile default address, pays WALLET, then clears the cart.
                - Address always comes from Profile default delivery. If missing, send them to Profile. Do not invent an address.
                - For damaged/missing orders, direct them to Complaints in the app.
                - Keep replies short, warm, and concrete. Mention product names, stock, and prices from tools.
                """.formatted(
                catalog.isEmpty() ? "(empty)" : catalog,
                wallet.balance().toPlainString(),
                cart.items().size(),
                cart.subtotal().toPlainString(),
                addressHint
        );
    }

    private ArrayNode toolDefinitions() {
        ArrayNode tools = objectMapper.createArrayNode();
        tools.add(tool(
                "search_menu",
                "Semantic search over the live available menu.",
                params(Map.of(
                        "query", prop("string", "Natural language craving or product cue")
                ), List.of("query"))
        ));
        tools.add(tool(
                "recommend",
                "Personalized or cold-start menu recommendations.",
                params(Map.of(
                        "preference", prop("string", "Optional preference like paneer or protein")
                ), List.of())
        ));
        tools.add(tool("get_cart", "Show the customer's current cart.", params(Map.of(), List.of())));
        tools.add(tool("get_profile", "Phone and default delivery address saved on Profile.", params(Map.of(), List.of())));
        tools.add(tool(
                "add_to_cart",
                "Add a menu item after stock check. Refuses if requested quantity exceeds stock and returns alternatives.",
                params(Map.of(
                        "product_id", prop("string", "Catalog product UUID"),
                        "name", prop("string", "Product name if id unknown"),
                        "quantity", prop("integer", "How many to add")
                ), List.of("quantity"))
        ));
        tools.add(tool("get_wallet", "Return wallet balance in INR.", params(Map.of(), List.of())));
        tools.add(tool(
                "get_orders",
                "List recent orders for tracking.",
                params(Map.of("limit", prop("integer", "Max orders to return")), List.of())
        ));
        tools.add(tool(
                "place_order",
                "Validate stock, wallet, and Profile default address, then checkout with WALLET and clear the cart.",
                params(Map.of(), List.of())
        ));
        return tools;
    }

    private ObjectNode prop(String type, String description) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", type);
        node.put("description", description);
        return node;
    }

    private ObjectNode params(Map<String, ObjectNode> properties, List<String> required) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        properties.forEach(props::set);
        schema.set("properties", props);
        if (!required.isEmpty()) {
            ArrayNode req = objectMapper.createArrayNode();
            required.forEach(req::add);
            schema.set("required", req);
        }
        return schema;
    }

    private ObjectNode tool(String name, String description, ObjectNode parameters) {
        ObjectNode fn = objectMapper.createObjectNode();
        fn.put("name", name);
        fn.put("description", description);
        fn.set("parameters", parameters);
        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.put("type", "function");
        wrapper.set("function", fn);
        return wrapper;
    }

    private String executeTool(UUID userId, String name, String argsJson, Map<UUID, ProductResponse> suggested) {
        try {
            JsonNode args = objectMapper.readTree(argsJson == null || argsJson.isBlank() ? "{}" : argsJson);
            return switch (name) {
                case "search_menu" -> {
                    List<ProductResponse> hits = semanticMenuSearch.search(args.path("query").asText(""), 5);
                    hits.forEach(product -> suggested.put(product.id(), product));
                    yield writeJson(Map.of("products", hits.stream().map(this::productMap).toList()));
                }
                case "recommend" -> {
                    String preference = args.path("preference").asText("");
                    List<ProductResponse> picks;
                    if (!preference.isBlank()) {
                        picks = semanticMenuSearch.search(preference, 4);
                    } else {
                        picks = recommendationService.recommend(userId, 4).items().stream()
                                .map(RecommendationItemResponse::product)
                                .toList();
                        if (picks.isEmpty()) {
                            picks = semanticMenuSearch.search("popular samosa", 4);
                        }
                    }
                    picks.forEach(product -> suggested.put(product.id(), product));
                    yield writeJson(Map.of("products", picks.stream().map(this::productMap).toList()));
                }
                case "get_cart" -> {
                    CartResponse cart = cartService.getCart(userId);
                    yield writeJson(Map.of(
                            "totalQuantity", cart.totalQuantity(),
                            "subtotal", cart.subtotal(),
                            "items", cart.items()
                    ));
                }
                case "get_profile" -> writeJson(profileMap(userService.getCurrentUser(userId)));
                case "add_to_cart" -> addToCartTool(userId, args, suggested);
                case "get_wallet" -> {
                    var wallet = walletService.getWallet(userId);
                    yield writeJson(Map.of("balance", wallet.balance(), "currency", wallet.currency()));
                }
                case "get_orders" -> {
                    int limit = Math.min(5, Math.max(1, args.path("limit").asInt(3)));
                    var page = orderService.list(userId, PageRequest.of(0, limit));
                    yield writeJson(Map.of(
                            "orders", page.getContent().stream().map(order -> Map.of(
                                    "id", order.id(),
                                    "status", order.status().name(),
                                    "total", order.total(),
                                    "createdAt", order.createdAt().toString()
                            )).toList()
                    ));
                }
                case "place_order" -> placeOrderTool(userId);
                default -> writeJson(Map.of("error", "Unknown tool: " + name));
            };
        } catch (ApiException ex) {
            return writeJson(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return writeJson(Map.of("error", ex.getMessage() == null ? "Tool failed" : ex.getMessage()));
        }
    }

    private String addToCartTool(UUID userId, JsonNode args, Map<UUID, ProductResponse> suggested) {
        int quantity = Math.max(1, args.path("quantity").asInt(1));
        ProductResponse product = resolveProduct(args);
        String blocked = stockBlock(product, quantity);
        if (blocked != null) {
            List<ProductResponse> alts = alternatives(product, suggested);
            return writeJson(Map.of(
                    "added", false,
                    "error", blocked,
                    "product", product.name(),
                    "requested", quantity,
                    "stock", product.stock(),
                    "canOrder", Math.max(0, product.stock()),
                    "alternatives", alts.stream().map(this::productMap).toList()
            ));
        }
        cartService.addItem(userId, product.id(), quantity);
        suggested.put(product.id(), product);
        CartResponse cart = cartService.getCart(userId);
        return writeJson(Map.of(
                "added", true,
                "name", product.name(),
                "quantity", quantity,
                "stock", product.stock(),
                "cartSubtotal", cart.subtotal(),
                "cartQuantity", cart.totalQuantity()
        ));
    }

    private String placeOrderTool(UUID userId) {
        UserResponse profile = userService.getCurrentUser(userId);
        if (!profile.hasDefaultAddress()) {
            return writeJson(Map.of(
                    "placed", false,
                    "error",
                    "No default address on Profile. Save name, street, city, state, pincode (and phone) under Profile, then ask me again."
            ));
        }
        CartResponse cart = cartService.getCart(userId);
        if (cart.items().isEmpty()) {
            return writeJson(Map.of("placed", false, "error", "Cart is empty. Add items first."));
        }
        List<Map<String, Object>> stockIssues = new ArrayList<>();
        for (CartItemResponse line : cart.items()) {
            if (line.productMissing() || !line.available()) {
                stockIssues.add(Map.of(
                        "name", line.name(),
                        "requested", line.quantity(),
                        "stock", line.stock(),
                        "error", "This item is no longer available"
                ));
            } else if (line.quantity() > line.stock()) {
                stockIssues.add(Map.of(
                        "name", line.name(),
                        "requested", line.quantity(),
                        "stock", line.stock(),
                        "error", "Only " + line.stock() + " in stock. You can order " + line.stock() + ", not " + line.quantity() + "."
                ));
            }
        }
        if (!stockIssues.isEmpty()) {
            return writeJson(Map.of(
                    "placed", false,
                    "error", "Stock check failed. Cart was not charged.",
                    "issues", stockIssues
            ));
        }
        var wallet = walletService.getWallet(userId);
        if (wallet.balance().compareTo(cart.subtotal()) < 0) {
            return writeJson(Map.of(
                    "placed", false,
                    "error",
                    "Wallet has ₹" + wallet.balance().toPlainString()
                            + " but cart is ₹" + cart.subtotal().toPlainString()
                            + ". Add money on Wallet, then retry."
            ));
        }
        DeliveryAddressRequest delivery = userService.requireDefaultAddress(userId);
        try {
            var result = orderService.create(
                    userId,
                    new CreateOrderRequest(delivery, true, PaymentMethod.WALLET),
                    UUID.randomUUID().toString()
            );
            cartService.clear(userId);
            OrderResponse order = result.order();
            return writeJson(Map.of(
                    "placed", true,
                    "replayed", result.replayed(),
                    "orderId", order.id(),
                    "status", order.status().name(),
                    "total", order.total(),
                    "paidWith", "WALLET",
                    "deliveredTo", order.recipientName() + ", " + order.addressLine1() + ", " + order.city()
                            + " " + order.pincode(),
                    "addressSource", "profile"
            ));
        } catch (ApiException ex) {
            return writeJson(Map.of("placed", false, "error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return writeJson(Map.of("placed", false, "error", ex.getMessage() == null ? "Checkout failed" : ex.getMessage()));
        }
    }

    private String stockBlock(ProductResponse product, int quantity) {
        if (!product.available() || product.stock() <= 0) {
            return product.name() + " is sold out. Pick another samosa.";
        }
        if (quantity > product.stock()) {
            return product.name() + " has only " + product.stock() + " in stock. You can order "
                    + product.stock() + ", not " + quantity + ".";
        }
        return null;
    }

    private List<ProductResponse> alternatives(ProductResponse blocked, Map<UUID, ProductResponse> suggested) {
        List<ProductResponse> alts = semanticMenuSearch.search(blocked.name(), 5).stream()
                .filter(product -> !product.id().equals(blocked.id()) && product.stock() > 0)
                .limit(3)
                .toList();
        alts.forEach(product -> suggested.put(product.id(), product));
        return alts;
    }

    private Map<String, Object> profileMap(UserResponse profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fullName", profile.fullName());
        map.put("phone", profile.phone());
        map.put("hasDefaultAddress", profile.hasDefaultAddress());
        map.put("recipientName", profile.recipientName());
        map.put("addressLine1", profile.addressLine1());
        map.put("city", profile.city());
        map.put("state", profile.state());
        map.put("pincode", profile.pincode());
        return map;
    }

    private ProductResponse resolveProduct(JsonNode args) {
        String productId = args.path("product_id").asText("").trim();
        if (!productId.isBlank()) {
            return productService.getById(UUID.fromString(productId));
        }
        String name = args.path("name").asText("").trim();
        if (name.isBlank()) {
            throw new com.samosajunction.common.exception.InvalidRequestException("product_id or name is required");
        }
        return findProduct(name).orElseThrow(() ->
                new com.samosajunction.common.exception.InvalidRequestException("No menu match for " + name));
    }

    private Map<String, Object> productMap(ProductResponse product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.id());
        map.put("name", product.name());
        map.put("price", product.price());
        map.put("stock", product.stock());
        map.put("protein", product.protein());
        map.put("description", product.description());
        map.put("dietaryTags", product.dietaryTags());
        return map;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }

    private AssistantChatResponse chatHeuristic(UUID userId, String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (mentions(lower, "damaged", "complaint", "missing", "wrong item", "refund")) {
            return AssistantChatResponse.text(
                    "Sorry about that. Open Complaints, pick the paid order, and an executive will follow up on your account email."
            );
        }
        if (mentions(lower, "wallet", "balance", "add money")) {
            var wallet = walletService.getWallet(userId);
            return AssistantChatResponse.text(
                    "Your wallet has ₹" + wallet.balance().toPlainString() + ". You can add money on the Wallet page before checkout."
            );
        }
        if (mentions(lower, "where is my order", "track", "order status", "my order")) {
            var page = orderService.list(userId, PageRequest.of(0, 1));
            if (page.isEmpty()) {
                return AssistantChatResponse.text("You don't have an order yet. Ask me to add paneer samosas and place the order when ready.");
            }
            OrderResponse order = page.getContent().getFirst();
            return AssistantChatResponse.text(
                    "Your latest order is " + readable(order.status().name())
                            + " for ₹" + order.total().toPlainString()
                            + ". Open Orders for the full timeline."
            );
        }
        if (mentions(lower, "checkout", "place order", "buy now", "pay with wallet", "order now")
                && !ORDER_QTY.matcher(message).find()) {
            String result = placeOrderTool(userId);
            return AssistantChatResponse.text(result.contains("\"placed\":true")
                    ? "Placed your cart. Paid from wallet to the default address on your Profile. Cart is empty — open Orders to track it."
                    : "Could not place the order yet: " + result);
        }

        Matcher order = ORDER_QTY.matcher(message);
        if (order.find()) {
            int quantity = Integer.parseInt(order.group(1));
            String wanted = order.group(2);
            boolean checkout = mentions(lower, "checkout", "place", "buy", "pay")
                    || lower.matches("(?s).*\\border\\b.*");
            AssistantChatResponse added = addWanted(userId, wanted, quantity);
            if (!checkout || !added.reply().startsWith("Added ")) {
                return added;
            }
            String placed = placeOrderTool(userId);
            if (placed.contains("\"placed\":true")) {
                return new AssistantChatResponse(
                        added.reply().replace("Open Cart when you’re ready to check out.", "").trim()
                                + " Paid from wallet to your Profile default address. Cart is cleared. Open Orders to track it.",
                        added.products()
                );
            }
            return new AssistantChatResponse(added.reply() + " Checkout blocked: " + placed, added.products());
        }

        if (mentions(lower, "hungry", "recommend", "what should", "try", "protein", "under", "best", "lover", "love")) {
            return suggest(userId, lower);
        }

        List<ProductResponse> semantic = semanticMenuSearch.search(message, 4);
        if (!semantic.isEmpty()) {
            return new AssistantChatResponse(
                    "Here’s what fits from the live menu:",
                    semantic.stream().map(AssistantService::suggestion).toList()
            );
        }
        return AssistantChatResponse.text("I couldn’t match that to the menu. Try a name like paneer, millet, or classic.");
    }

    private AssistantChatResponse addWanted(UUID userId, String wanted, int quantity) {
        Optional<ProductResponse> match = findProduct(wanted);
        if (match.isEmpty()) {
            List<ProductResponse> semantic = semanticMenuSearch.search(wanted, 4);
            if (semantic.isEmpty()) {
                return AssistantChatResponse.text("I couldn’t match that to the menu. Try a name like paneer, millet, or classic.");
            }
            return new AssistantChatResponse(
                    "I couldn’t add that exactly. Closest from the menu:",
                    semantic.stream().map(AssistantService::suggestion).toList()
            );
        }
        ProductResponse product = match.get();
        String blocked = stockBlock(product, quantity);
        if (blocked != null) {
            List<ProductResponse> alts = alternatives(product, new LinkedHashMap<>());
            String extra = alts.isEmpty()
                    ? ""
                    : " Other options: " + alts.stream().map(item -> item.name() + " (" + item.stock() + " in stock)").reduce((a, b) -> a + ", " + b).orElse("");
            return new AssistantChatResponse(blocked + extra, alts.stream().map(AssistantService::suggestion).toList());
        }
        try {
            cartService.addItem(userId, product.id(), quantity);
        } catch (RuntimeException ex) {
            return AssistantChatResponse.text(ex.getMessage());
        }
        return new AssistantChatResponse(
                "Added " + quantity + " × " + product.name() + " to your cart. Open Cart when you’re ready to check out.",
                List.of(suggestion(product))
        );
    }

    private AssistantChatResponse suggest(UUID userId, String lower) {
        if (lower.contains("protein")) {
            var page = productService.search(
                    new ProductSearchCriteria(null, null, null, true, "HIGH_PROTEIN"),
                    PageRequest.of(0, 4)
            );
            if (!page.isEmpty()) {
                return new AssistantChatResponse(
                        "Here are high-protein picks from the menu.",
                        page.getContent().stream().map(AssistantService::suggestion).toList()
                );
            }
        }
        List<ProductResponse> semantic = semanticMenuSearch.search(lower, 4);
        if (!semantic.isEmpty()) {
            return new AssistantChatResponse(
                    "For a paneer lover (or that craving), start here:",
                    semantic.stream().map(AssistantService::suggestion).toList()
            );
        }
        var recs = recommendationService.recommend(userId, 4);
        List<ProductResponse> products = recs.items().stream().map(RecommendationItemResponse::product).toList();
        if (!products.isEmpty()) {
            String lead = recs.coldStart()
                    ? "I don’t have paid-order taste yet, so these are live menu picks."
                    : "Based on what you’ve ordered before:";
            return new AssistantChatResponse(lead, products.stream().map(AssistantService::suggestion).toList());
        }
        return AssistantChatResponse.text("Menu is empty right now.");
    }

    private Optional<ProductResponse> findProduct(String wanted) {
        List<ProductResponse> semantic = semanticMenuSearch.search(wanted, 5);
        if (!semantic.isEmpty()) {
            String needle = wanted.toLowerCase(Locale.ROOT)
                    .replaceAll("samosas?", " ")
                    .replaceAll("[^a-z0-9 ]", " ")
                    .trim();
            return semantic.stream()
                    .max(Comparator.comparingInt(product -> tokenScore(product.name(), needle)));
        }
        String needle = wanted.toLowerCase(Locale.ROOT)
                .replaceAll("samosas?", " ")
                .replaceAll("[^a-z0-9 ]", " ")
                .trim();
        var page = productService.search(
                new ProductSearchCriteria(needle.isBlank() ? wanted : needle, null, null, true, null),
                PageRequest.of(0, 20)
        );
        return page.getContent().stream()
                .filter(product -> tokenScore(product.name(), needle) > 0)
                .max(Comparator.comparingInt(product -> tokenScore(product.name(), needle)));
    }

    private static int tokenScore(String name, String needle) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains(needle) || needle.contains(n.toLowerCase(Locale.ROOT))) {
            return 100;
        }
        int hits = 0;
        for (String token : needle.split("\\s+")) {
            if (token.length() >= 3 && n.contains(token)) {
                hits += 10;
            }
        }
        return hits;
    }

    private static AssistantProductSuggestion suggestion(ProductResponse product) {
        return new AssistantProductSuggestion(product.id(), product.name(), product.price(), product.stock());
    }

    private static boolean mentions(String lower, String... needles) {
        for (String needle : needles) {
            if (lower.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String readable(String status) {
        return status.replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
