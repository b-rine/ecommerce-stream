package com.ecommerce.kafkaecommerce.dto;

public class CreateOrderResponse {
    private boolean success;
    private String orderId;
    private String message;
    private String error;
    
    // Constructors
    public CreateOrderResponse() {}
    
    public CreateOrderResponse(boolean success, String orderId, String message) {
        this.success = success;
        this.orderId = orderId;
        this.message = message;
    }
    
    public CreateOrderResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
