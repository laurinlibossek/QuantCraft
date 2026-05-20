package com.quantcraft.screen;

public record TradeMessage(long timestamp, String text, MessageType type) {
    public enum MessageType {
        TRADE,      // Buy/sell confirmations - green
        ERROR,      // Failed trades - red
        INFO,       // General info - gray
        SUCCESS     // Successful actions - gold
    }
}
