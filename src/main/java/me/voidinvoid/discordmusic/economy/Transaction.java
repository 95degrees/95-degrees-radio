package me.voidinvoid.discordmusic.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Transaction {

    private String id;
    private TransactionType type;
    private int amount;
    private long timestamp;
    private Map<String, Object> params = new HashMap<>();

    public Transaction(TransactionType type, int amount) {

        this(UUID.randomUUID().toString().substring(0, 8), type, amount, System.currentTimeMillis());
    }

    public Transaction(String id, TransactionType type, int amount, long timestamp) {

        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.id = id;
    }

    public Transaction addParameter(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public String getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
