package com.example.tarifrehberi;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {

    // Veritabanı yolu
    private static final String DB_URL = "jdbc:sqlite:./identifier.sqlite";

    // Veritabanı bağlantısını oluşturma
    public static Connection connect() {
        Connection conn = null;
        try {
            // SQLite bağlantısı oluşturuluyor
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Veritabanına bağlanıldı.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }



    // Veri ekleme
    public void TarifEkle(String TarifAdi, String Kategori, int HazirlanmaSuresi,String Talimatlar) {
        String sql = "INSERT INTO Tarifler(TarifAdi, Kategori, HazirlanmaSuresi,Talimatlar) VALUES(?, ?, ?,?)";

        try (Connection conn = connect();
             var pstmt = conn.prepareStatement(sql)) {
            // Verileri SQL sorgusuna ekliyoruz
            pstmt.setString(1, TarifAdi);
            pstmt.setString(2, Kategori);
            pstmt.setInt(3, HazirlanmaSuresi);
            pstmt.setString(4, Talimatlar);

            pstmt.executeUpdate();
            System.out.println("Tarif başarıyla eklendi.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


}