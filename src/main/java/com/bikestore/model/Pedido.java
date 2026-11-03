package com.bikestore.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Pedido que viaja como JSON por el broker.
 */
public class Pedido {

    private String pedidoId;
    private String cliente;
    private String email;
    private List<Item> items = new ArrayList<>();
    private double total;
    private String paymentStatus = "PENDING";   // PENDING | PAID | FAILED
    private String timestamp;
    /** Solo para pruebas: si es true el cobro falla siempre (sirve para ver la DLQ). */
    private boolean forzarFallo;

    public Pedido() {
    }

    public static class Item {
        private String producto;
        private int cantidad;
        private double precio;

        public Item() {
        }

        public Item(String producto, int cantidad, double precio) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.precio = precio;
        }

        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }
        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
        public double getPrecio() { return precio; }
        public void setPrecio(double precio) { this.precio = precio; }
    }

    /** Suma cantidad * precio de los items. */
    public void calcularTotal() {
        double suma = 0;
        for (Item i : items) {
            suma += i.getCantidad() * i.getPrecio();
        }
        this.total = Math.round(suma * 100.0) / 100.0;
    }

    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public boolean isForzarFallo() { return forzarFallo; }
    public void setForzarFallo(boolean forzarFallo) { this.forzarFallo = forzarFallo; }
}
