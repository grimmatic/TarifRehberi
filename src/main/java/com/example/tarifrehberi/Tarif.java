package com.example.tarifrehberi;

public class Tarif {
    private String tarifAdi;

    public Tarif(String tarifAdi) {
        this.tarifAdi = tarifAdi;
    }

    public String getTarifAdi() {
        return tarifAdi;
    }
    @Override
    public String toString() {
        return tarifAdi;
    }
}