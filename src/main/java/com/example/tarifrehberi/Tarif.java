package com.example.tarifrehberi;

public class Tarif {
    private String TarifAdi;
    private String Kategori;
    private int HazirlanmaSuresi;
    private String Talimatlar;

    public Tarif(String TarifAdi, String Kategori, int HazirlanmaSuresi, String Talimatlar) {
        this.TarifAdi = TarifAdi;
        this.Kategori = Kategori;
        this.HazirlanmaSuresi = HazirlanmaSuresi;
        this.Talimatlar = Talimatlar;
    }

    public String getTarifAdi() {
        return TarifAdi;
    }

    public String getKategori() {
        return Kategori;
    }

    public int getHazirlanmaSuresi() {
        return HazirlanmaSuresi;
    }

    public String getTalimatlar() {
        return Talimatlar;
    }
}