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

        if (chatHistory.isEmpty()) {
            addSystemMessage(getBusinessContextPrompt());
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
        sb.append("You are a Business Insights Assistant for 'Stock Savvy Longganisa', a meat processing business. ");
        sb.append("You have access to current business data. Use this information to answer user questions concisely and professionally. ");
        sb.append("Current Business Data:\n\n");

        // Sales Summary
        SaleDAO.SaleSummary saleSummary = saleDAO.getSummary();
        sb.append("--- SALES (This Month) ---\n");
        sb.append("- Total Revenue: ").append(String.format("₱%.2f", saleSummary.totalRevenue)).append("\n");
        sb.append("- Total Transactions: ").append(saleSummary.totalTransactions).append("\n");
        sb.append("- Average Order Value: ").append(String.format("₱%.2f", saleSummary.avgOrderValue)).append("\n");
        sb.append("- Top Product: ").append(saleSummary.topProduct).append("\n");
        sb.append("- Top Customer: ").append(saleSummary.topCustomer).append("\n\n");

        // Inventory Summary (Low Stock)
        List<Stock> lowStock = stockDAO.getAllStocks().stream()
                .filter(s -> s.getQuantity() <= s.getLowStockThreshold())
                .collect(Collectors.toList());
        sb.append("--- INVENTORY ALERTS (Low Stock) ---\n");
        if (lowStock.isEmpty()) {
            sb.append("- No items are currently low on stock.\n");
        } else {
            for (Stock s : lowStock) {
                sb.append("- ").append(s.getProductName()).append(": ").append(s.getQuantity()).append(" ").append(s.getUnit()).append(" left\n");
            }
        }
        sb.append("\n");

        // Production Summary
        List<Production> productions = productionDAO.getAllProductions();
        sb.append("--- PRODUCTION STATUS ---\n");
        if (productions.isEmpty()) {
            sb.append("- No production records found.\n");
        } else {
            // Take top 5 recent productions
            productions.stream().limit(5).forEach(p -> {
                sb.append("- ").append(p.getName()).append(": ").append(p.getQuantity()).append(" ").append(p.getUnit()).append(" (").append(p.getStatus()).append(")\n");
            });
        }

        sb.append("\nWhen responding, try to be helpful and proactive. If stock is low, suggest restocking. If sales are high, congratulate the user.");
        return sb.toString();
    }
}