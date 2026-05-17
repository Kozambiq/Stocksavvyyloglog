package com.savvy.stocksavvyyloglog.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.savvy.stocksavvyyloglog.model.ProductionDAO;
import com.savvy.stocksavvyyloglog.model.SaleDAO;
import com.savvy.stocksavvyyloglog.model.StockDAO;
import com.savvy.stocksavvyyloglog.model.Stock;
import com.savvy.stocksavvyyloglog.model.Production;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class ChatbotService {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final String apiKey;
    private final List<Map<String, String>> chatHistory = new ArrayList<>();
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final SaleDAO saleDAO = new SaleDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final ProductionDAO productionDAO = new ProductionDAO();

    public ChatbotService() {
        String key = null;
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(".env"));
            key = props.getProperty("GROQ_API_KEY");
        } catch (Exception e) {
            System.err.println("[ChatbotService] Error loading API key: " + e.getMessage());
        }
        this.apiKey = key;
    }

    public void clearHistory() {
        chatHistory.clear();
    }

    public String askChatbot(String userMessage) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Error: Groq API key not found in .env file.";
        }

        // Always use a fresh system prompt with live data as the first message
        Map<String, String> systemPrompt = Map.of("role", "system", "content", getBusinessContextPrompt());
        
        if (chatHistory.isEmpty()) {
            chatHistory.add(systemPrompt);
        } else {
            // Update the system prompt (first message) with the latest data
            chatHistory.set(0, systemPrompt);
        }

        chatHistory.add(Map.of("role", "user", "content", userMessage));

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "llama-3.1-8b-instant");
        
        JsonArray messages = new JsonArray();
        for (Map<String, String> msg : chatHistory) {
            JsonObject m = new JsonObject();
            m.addProperty("role", msg.get("role"));
            m.addProperty("content", msg.get("content"));
            messages.add(m);
        }
        requestBody.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
            String botResponse = jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .get("message").getAsJsonObject()
                    .get("content").getAsString();
            
            chatHistory.add(Map.of("role", "assistant", "content", botResponse));
            return botResponse;
        } else {
            System.err.println("[ChatbotService] API Error: " + response.body());
            return "Error: Chatbot service is currently unavailable. (Status " + response.statusCode() + ")";
        }
    }

    private void addSystemMessage(String content) {
        chatHistory.add(Map.of("role", "system", "content", content));
    }

    private String getBusinessContextPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a Business Insights Assistant for 'Stock Savvy Longganisa'.\n");
        sb.append("DATE: ").append(java.time.LocalDate.now()).append("\n\n");
        sb.append("CRITICAL INSTRUCTIONS:\n");
        sb.append("1. DO NOT HALLUCINATE. You are in 'READ-ONLY' mode. Only talk about data that is explicitly listed below.\n");
        sb.append("2. If a user asks about a delivery, stock, or event NOT in this list, say: 'I don't have a record of that in the system.'\n");
        sb.append("3. Be professional, concise, and helpful. Use the 'Business Data Snapshot' below as your source of truth.\n\n");
        
        sb.append("=== BUSINESS DATA SNAPSHOT ===\n\n");

        // 1. PRODUCTS CATALOG
        sb.append("--- PRODUCTS CATALOG ---\n");
        try (java.sql.Connection conn = com.savvy.stocksavvyyloglog.util.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT name, category, price FROM products")) {
            java.sql.ResultSet rs = ps.executeQuery();
            boolean hasProducts = false;
            while (rs.next()) {
                hasProducts = true;
                sb.append("- ").append(rs.getString(1)).append(" (").append(rs.getString(2)).append("): ₱").append(rs.getDouble(3)).append("\n");
            }
            if (!hasProducts) sb.append("- No products in catalog.\n");
        } catch (Exception e) { sb.append("- Error loading products.\n"); }
        sb.append("\n");

        // 2. INVENTORY (RAW MATERIALS)
        List<Stock> allStock = stockDAO.getAllStocks();
        sb.append("--- CURRENT INVENTORY (Raw Materials) ---\n");
        if (allStock.isEmpty()) {
            sb.append("- No inventory items recorded.\n");
        } else {
            for (Stock s : allStock) {
                String alert = (s.getQuantity() <= s.getLowStockThreshold()) ? " [LOW STOCK ALERT]" : "";
                sb.append("- ").append(s.getProductName()).append(": ").append(s.getQuantity()).append(" ").append(s.getUnit()).append(alert).append("\n");
            }
        }
        sb.append("\n");

        // 3. PRODUCTION (FINISHED GOODS)
        List<Production> productions = productionDAO.getAllProductions();
        sb.append("--- PRODUCTION STATUS (Finished Goods) ---\n");
        if (productions.isEmpty()) {
            sb.append("- No production records.\n");
        } else {
            productions.stream().limit(10).forEach(p -> {
                sb.append("- ").append(p.getName()).append(": ").append(p.getQuantity()).append(" ").append(p.getUnit()).append(" (").append(p.getStatus()).append(")\n");
            });
        }
        sb.append("\n");

        // 4. SALES SUMMARY
        SaleDAO.SaleSummary saleSummary = saleDAO.getSummary();
        sb.append("--- SALES SUMMARY (Current Month) ---\n");
        sb.append("- Total Revenue: ").append(String.format("₱%.2f", saleSummary.totalRevenue)).append("\n");
        sb.append("- Total Transactions: ").append(saleSummary.totalTransactions).append("\n");
        sb.append("- Top Selling Product: ").append(saleSummary.topProduct).append("\n");
        sb.append("- Best Customer: ").append(saleSummary.topCustomer).append("\n\n");

        // 5. MASTER CALENDAR (Schedule + Deliveries + Pickups)
        sb.append("--- MASTER CALENDAR & EVENTS ---\n");
        try (java.sql.Connection conn = com.savvy.stocksavvyyloglog.util.DatabaseConnection.getConnection()) {
            String query = 
                    "SELECT event_date as date, title, event_type as type, status FROM schedule " +
                    "WHERE event_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                    "UNION ALL " +
                    "SELECT DATE(s.sale_date) as date, " +
                    "CONCAT(CASE WHEN LOWER(s.order_type) = 'deliver' THEN 'DELIVERY: ' ELSE 'PICKUP: ' END, COALESCE(c.name, 'Customer')) as title, " +
                    "CASE WHEN s.status = 'Cancelled' THEN 'Cancelled' " +
                    "     WHEN LOWER(s.order_type) = 'deliver' THEN 'Delivery' " +
                    "     ELSE 'Pickup' END as type, " +
                    "s.status " +
                    "FROM sales s " +
                    "LEFT JOIN customers c ON s.customer_id = c.id " +
                    "WHERE s.sale_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                    "ORDER BY date ASC";
            
            try (java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
                java.sql.ResultSet rs = ps.executeQuery();
                boolean hasEvents = false;
                while (rs.next()) {
                    hasEvents = true;
                    sb.append("- [").append(rs.getString("date")).append("] ")
                      .append(rs.getString("title")).append(" (").append(rs.getString("type")).append(") ")
                      .append("- Status: ").append(rs.getString("status")).append("\n");
                }
                if (!hasEvents) sb.append("- No recent or upcoming events recorded.\n");
            }
        } catch (Exception e) { sb.append("- Error loading calendar.\n"); }
        sb.append("\n");

        // 6. RECENT STOCK-IN LOGS
        sb.append("--- RECENT STOCK IN (Restocking History) ---\n");
        try (java.sql.Connection conn = com.savvy.stocksavvyyloglog.util.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT product_name, quantity, unit, created_at FROM stock_in_log ORDER BY created_at DESC LIMIT 5")) {
            java.sql.ResultSet rs = ps.executeQuery();
            boolean hasLogs = false;
            while (rs.next()) {
                hasLogs = true;
                sb.append("- ").append(rs.getString(1)).append(": +").append(rs.getDouble(2)).append(" ").append(rs.getString(3)).append(" (On ").append(rs.getString(4)).append(")\n");
            }
            if (!hasLogs) sb.append("- No recent stock additions.\n");
        } catch (Exception e) { sb.append("- Error loading stock logs.\n"); }

        return sb.toString();
    }
}