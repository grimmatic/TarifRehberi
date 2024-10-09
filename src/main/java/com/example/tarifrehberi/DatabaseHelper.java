package com.example.tarifrehberi;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {

//Tarif adini cekme


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


    public ObservableList<Tarif> getTarifler() {
        ObservableList<Tarif> tarifler = FXCollections.observableArrayList();
        String sql = "SELECT TarifAdi FROM tarifler ";
        // tarif id'ye göre sırala ORDER BY TarifID ASC
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {

                Tarif tarif = new Tarif(rs.getString("TarifAdi"));
                tarifler.add(tarif);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return tarifler;
    }



    public void createTable(Connection conn){
        Statement statement;
        try {
            // Tarifler
            String createRecipesTable = "CREATE TABLE Tarifler (" +
                    "TarifID SERIAL PRIMARY KEY," +
                    "TarifAdi VARCHAR(255)," +
                    "Kategori VARCHAR(100)," +
                    "HazirlamaSuresi INT," +
                    "Talimatlar TEXT" +
                    ");";

            // Malzemeler
            String createIngredientsTable = "CREATE TABLE Malzemeler (" +
                    "MalzemeID SERIAL PRIMARY KEY," +
                    "MalzemeAdi VARCHAR(255)," +
                    "ToplamMiktar VARCHAR(100)," +
                    "MalzemeBirim VARCHAR(50)," +
                    "BirimFiyat DECIMAL(10, 2)" +
                    ");";

            // Tarif-Malzeme
            String createRecipeIngredientsTable = "CREATE TABLE TarifMalzemeleri (" +
                    "TarifID INT REFERENCES Tarifler(TarifID)," +
                    "MalzemeID INT REFERENCES Malzemeler(MalzemeID)," +
                    "MalzemeMiktar FLOAT," +
                    "PRIMARY KEY (TarifID, MalzemeID)" +
                    ");";

            // SQL
            statement = conn.createStatement();
            statement.executeUpdate(createRecipesTable);
            statement.executeUpdate(createIngredientsTable);
            statement.executeUpdate(createRecipeIngredientsTable);

            System.out.println("Tables Created");

        } catch (Exception e) {
            System.out.println(e);
        }

    }


    public void addRecipe(Connection conn, String tarifAdi, String kategori, int hazirlamaSuresi, String talimatlar, Map<String, Float> malzemeler) {
        try {
            // Tarifi ekle
            String query = "INSERT INTO Tarifler (TarifAdi, Kategori, HazirlamaSuresi, Talimatlar) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, tarifAdi);
            pstmt.setString(2, kategori);
            pstmt.setInt(3, hazirlamaSuresi);
            pstmt.setString(4, talimatlar);
            pstmt.executeUpdate();

            // Tarifin ID'sini al
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int tarifID = rs.getInt(1);

                // Malzemeleri ekle
                for (Map.Entry<String, Float> entry : malzemeler.entrySet()) {
                    String malzemeAdi = entry.getKey();
                    Float miktar = entry.getValue();

                    // Malzeme var mı kontrol et yoksa ekle
                    int malzemeID = getOrCreateIngredient(conn, malzemeAdi);

                    // Tarif Malzeme ilişkisini ekle
                    addRecipeIngredient(conn, tarifID, malzemeID, miktar);
                }
                System.out.println("Recipe added successfully.");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Malzeme ekleme veya mevcut malzemenin ID'sini almaca
    public int getOrCreateIngredient(Connection conn, String malzemeAdi) {
        try {
            // Malzeme var mı kontrol et
            String query = "SELECT MalzemeID FROM Malzemeler WHERE MalzemeAdi = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, malzemeAdi);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("MalzemeID"); // Malzeme mevcutsa ID'sini döndür
            } else {
                // Malzeme yoksa ekle
                query = "INSERT INTO Malzemeler (MalzemeAdi) VALUES (?)";
                pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1, malzemeAdi);
                pstmt.executeUpdate();
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1); // Yeni eklenen malzemenin ID'sini döndür
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return -1;
    }

    // Tarif-Malzeme ilişkisi eklemece
    public void addRecipeIngredient(Connection conn, int tarifID, int malzemeID, float miktar) {
        try {
            String query = "INSERT INTO TarifMalzemeleri (TarifID, MalzemeID, MalzemeMiktar) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, tarifID);
            pstmt.setInt(2, malzemeID);
            pstmt.setFloat(3, miktar);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void suggestRecipes(Connection conn) {
        try {
            // Tüm tarifleri ve malzemelerini al
            String query = "SELECT t.TarifID, t.TarifAdi, tm.MalzemeID, tm.MalzemeMiktar, m.ToplamMiktar, m.BirimFiyat " +
                    "FROM Tarifler t " +
                    "JOIN TarifMalzemeleri tm ON t.TarifID = tm.TarifID " +
                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID";
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            // Tariflerin durumu
            Map<Integer, Boolean> recipeStatus = new HashMap<>();
            Map<Integer, Float> missingCosts = new HashMap<>();

            while (rs.next()) {
                int tarifID = rs.getInt("TarifID");
                float gerekliMiktar = rs.getFloat("MalzemeMiktar");
                float mevcutMiktar = Float.parseFloat(rs.getString("ToplamMiktar"));
                float birimFiyat = rs.getFloat("BirimFiyat");

                if (gerekliMiktar > mevcutMiktar) {
                    //Eksik malzeme kırmızı
                    recipeStatus.put(tarifID, false);
                    float eksikMiktar = gerekliMiktar - mevcutMiktar;
                    missingCosts.put(tarifID, missingCosts.getOrDefault(tarifID, 0f) + eksikMiktar * birimFiyat);
                } else if (!recipeStatus.containsKey(tarifID)) {
                    //yeşil
                    recipeStatus.put(tarifID, true);
                }
            }

            // Tariflerin renkleri ve eksik maliyetlerle göstermece
            for (Map.Entry<Integer, Boolean> entry : recipeStatus.entrySet()) {
                int tarifID = entry.getKey();
                boolean yeterli = entry.getValue();
                String renk = yeterli ? "Yeşil" : "Kırmızı";
                float eksikMaliyet = missingCosts.getOrDefault(tarifID, 0f);
                System.out.println("Tarif ID: " + tarifID + ", Durum: " + renk + (yeterli ? "" : ", Eksik Maliyet: " + eksikMaliyet));
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Tarif adına göre arama
    public void searchRecipeByName(Connection conn, String tarifAdi) {
        try {
            String query = "SELECT * FROM Tarifler WHERE TarifAdi ILIKE ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, "%" + tarifAdi + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Tarif: " + rs.getString("TarifAdi"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Malzemeye göre arama ve eşleşme yüzdesi hesaplama
    public void searchRecipeByIngredients(Connection conn, List<String> ingredients) {
        try {
            String query = "SELECT t.TarifID, t.TarifAdi, COUNT(tm.MalzemeID) AS MalzemeSayisi " +
                    "FROM Tarifler t " +
                    "JOIN TarifMalzemeleri tm ON t.TarifID = tm.TarifID " +
                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                    "WHERE m.MalzemeAdi = ANY (?) " +
                    "GROUP BY t.TarifID, t.TarifAdi " +
                    "ORDER BY MalzemeSayisi DESC";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setArray(1, conn.createArrayOf("VARCHAR", ingredients.toArray()));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Tarif: " + rs.getString("TarifAdi") + ", Eşleşen Malzeme Sayısı: " + rs.getInt("MalzemeSayisi"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    //filtreleme ve sıralama

    // Hazırlama süresine göre tarifleri sıralama
    public void sortRecipesByPrepTime(Connection conn, boolean ascending) {
        try {
            String query = "SELECT * FROM Tarifler ORDER BY HazirlamaSuresi " + (ascending ? "ASC" : "DESC"); //Artıo mu azalıo mu
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Tarif: " + rs.getString("TarifAdi") + ", Hazırlama Süresi: " + rs.getInt("HazirlamaSuresi"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Tarifleri maliyete göre sıralama
    public void sortRecipesByCost(Connection conn, boolean ascending) {
        try {
            String query = "SELECT t.TarifID, t.TarifAdi, SUM(tm.MalzemeMiktar * m.BirimFiyat) AS ToplamMaliyet " +
                    "FROM Tarifler t " +
                    "JOIN TarifMalzemeleri tm ON t.TarifID = tm.TarifID " +
                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                    "GROUP BY t.TarifID, t.TarifAdi " +
                    "ORDER BY ToplamMaliyet " + (ascending ? "ASC" : "DESC");
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Tarif: " + rs.getString("TarifAdi") + ", Maliyet: " + rs.getFloat("ToplamMaliyet"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Tarifleri malzeme sayısına göre sıralama
    public void sortRecipesByIngredientCount(Connection conn, boolean ascending) {
        try {
            String query = "SELECT t.TarifID, t.TarifAdi, COUNT(tm.MalzemeID) AS MalzemeSayisi " +
                    "FROM Tarifler t " +
                    "JOIN TarifMalzemeleri tm ON t.TarifID = tm.TarifID " +
                    "GROUP BY t.TarifID, t.TarifAdi " +
                    "ORDER BY MalzemeSayisi " + (ascending ? "ASC" : "DESC");
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Tarif: " + rs.getString("TarifAdi") + ", Malzeme Sayısı: " + rs.getInt("MalzemeSayisi"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    //  Ttarifleri filtreleme
    public void filterRecipesByCategory(Connection conn, String kategori) {
        try {
            String query = "SELECT * FROM Tarifler WHERE Kategori = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, kategori);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Tarif: " + rs.getString("TarifAdi") + ", Kategori: " + rs.getString("Kategori"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Maliyet aralığına göre tarifleri filtreleme
    public void filterRecipesByCostRange(Connection conn, float minCost, float maxCost) {
        try {
            String query = "SELECT t.TarifID, t.TarifAdi, SUM(tm.MalzemeMiktar * m.BirimFiyat) AS ToplamMaliyet " +
                    "FROM Tarifler t " +
                    "JOIN TarifMalzemeleri tm ON t.TarifID = tm.TarifID " +
                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                    "GROUP BY t.TarifID, t.TarifAdi " +
                    "HAVING SUM(tm.MalzemeMiktar * m.BirimFiyat) BETWEEN ? AND ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setFloat(1, minCost);
            pstmt.setFloat(2, maxCost);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Tarif: " + rs.getString("TarifAdi") + ", Maliyet: " + rs.getFloat("ToplamMaliyet"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    public void updateRecipe(Connection conn, int tarifID, String yeniAdi, String yeniKategori, int yeniSure, String yeniTalimatlar) {
        try {
            String query = "UPDATE Tarifler SET TarifAdi = ?, Kategori = ?, HazirlamaSuresi = ?, Talimatlar = ? WHERE TarifID = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, yeniAdi);
            pstmt.setString(2, yeniKategori);
            pstmt.setInt(3, yeniSure);
            pstmt.setString(4, yeniTalimatlar);
            pstmt.setInt(5, tarifID);
            pstmt.executeUpdate();
            System.out.println("Tarif güncellendi.");
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    public void deleteRecipe(Connection conn, int tarifID) {
        try {
            String query = "DELETE FROM Tarifler WHERE TarifID = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, tarifID);
            pstmt.executeUpdate();
            System.out.println("Tarif silindi.");
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    // duplicate kontrolü
    public boolean isDuplicateRecipe(Connection conn, String tarifAdi) {
        try {
            String query = "SELECT COUNT(*) FROM Tarifler WHERE TarifAdi = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, tarifAdi);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Tarif zaten varsa true döndür
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }



}