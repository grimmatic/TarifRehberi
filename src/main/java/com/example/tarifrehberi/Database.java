package com.example.tarifrehberi;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {

//Tarif adini cekme


    // Veritabanı yolu.VT bu dosya üzerinden çalışır
    private static final String DB_URL = "jdbc:sqlite:./identifier.sqlite";

    // Veritabanı bağlantısını oluşturma
    public static Connection connect() {
        Connection conn = null;
        try {
            // SQLite bağlantısı oluşturuluyor
            conn = DriverManager.getConnection(DB_URL); // Bağlantıyı oluştur
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


            pstmt.executeUpdate(); //veritabanına ekleme işlemi
            System.out.println("Tarif başarıyla eklendi.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }






    public void createTable(Connection conn) {
        Statement statement;
        try {
            // Tarifler
            String createRecipesTable = "CREATE TABLE IF NOT EXISTS Tarifler (" +
                    "TarifID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "TarifAdi TEXT," +
                    "Kategori TEXT," +
                    "HazirlamaSuresi INTEGER," +
                    "Talimatlar TEXT" +
                    ");";

            // Malzemeler
            String createIngredientsTable = "CREATE TABLE IF NOT EXISTS Malzemeler (" + // Düzeltildi
                    "MalzemeID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "MalzemeAdi TEXT," +
                    "ToplamMiktar TEXT," +
                    "MalzemeBirim TEXT," +
                    "BirimFiyat DECIMAL(10, 2)" +
                    ");";

            // Tarif-Malzeme
            String createRecipeIngredientsTable = "CREATE TABLE IF NOT EXISTS TarifMalzemeleri (" +
                    "TarifID INTEGER REFERENCES Tarifler(TarifID) ON DELETE CASCADE ON UPDATE CASCADE," + // Düzeltildi
                    "MalzemeID INTEGER REFERENCES Malzemeler(MalzemeID) ON DELETE CASCADE ON UPDATE CASCADE," + // Düzeltildi
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
            e.printStackTrace();
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
            pstmt.setString(1, malzemeAdi); //querydeki "?" yerine malzemeAdi değerini yerleştir
            ResultSet rs = pstmt.executeQuery(); //sonuclar resultset içinde saklanır
            if (rs.next()) {//resultset içindeki sonraki kayda geç(malzemeadi db'de mevcutsa true)
                return rs.getInt("MalzemeID"); // Malzeme mevcutsa ID'sini döndür
            } else {
                // Malzeme yoksa ekle
                query = "INSERT INTO Malzemeler (MalzemeAdi) VALUES (?)";
                pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS); //yeni eklenen kaydın otomatik kaydını (malzemeID) almak için
                pstmt.setString(1, malzemeAdi);
                pstmt.executeUpdate();
                rs = pstmt.getGeneratedKeys(); //eklenen malzemenin idsini almaca
                if (rs.next()) {
                    return rs.getInt(1); // Yeni eklenen malzemenin ID'sini döndür
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return -1; //işlem başarısız oldu malzeme id alınamadı
    }

    // Tarif-Malzeme ilişkisi eklemece yaani bir tarifin hangi malzemeden ne kadar içerdiğini
    public void addRecipeIngredient(Connection conn, int tarifID, int malzemeID, float miktar) {
        try {
            String query = "INSERT INTO TarifMalzemeleri (TarifID, MalzemeID, MalzemeMiktar) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            //preperadstatement avantajları :
            // Performans: Sorgular önceden derlenmiş olduğundan performans açısından daha iyidir.
            //Güvenlik: SQL enjeksiyon saldırılarına karşı koruma sağlar, çünkü parametreler doğrudan sorguya gömülmez.
            //Amaç: Sorguyu dinamik hale getirir ve farklı tarif ve malzemeler için tekrar kullanılabilir olmasını sağlar.
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
                float gerekliMiktar = rs.getFloat("MalzemeMiktar"); // tarifte gereken malzeme miktarı
                float mevcutMiktar = Float.parseFloat(rs.getString("ToplamMiktar")); // depodaki mevcut miktar
                float birimFiyat = rs.getFloat("BirimFiyat");

                if (gerekliMiktar > mevcutMiktar) {
                    //Eksik malzeme kırmızı
                    recipeStatus.put(tarifID, false); //yeterli malzeme yoq
                    float eksikMiktar = gerekliMiktar - mevcutMiktar;
                    missingCosts.put(tarifID, missingCosts.getOrDefault(tarifID, 0f) + eksikMiktar * birimFiyat); // Eksik malzemenin maliyetini hesaplayarak tarifin toplam eksik maliyetine ekler.
                } else if (!recipeStatus.containsKey(tarifID)) { //değerlendirilmediyse true
                    //yeşil
                    recipeStatus.put(tarifID, true);
                }
            }

            // Tariflerin renkleri ve eksik maliyetlerle göstermece
            for (Map.Entry<Integer, Boolean> entry : recipeStatus.entrySet()) {
                int tarifID = entry.getKey();
                boolean yeterli = entry.getValue();
                String renk = yeterli ? "Yeşil" : "Kırmızı";
                float eksikMaliyet = missingCosts.getOrDefault(tarifID, 0f); //missingCosts'dan eksik maliyeti alır, yoksa 0 olarak döner.
                System.out.println("Tarif ID: " + tarifID + ", Durum: " + renk + (yeterli ? "" : ", Eksik Maliyet: " + eksikMaliyet));
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }


    // DatabaseHelper sınıfı içinde tarifin malzemelerini almak için bir metot
    public List<String> getIngredientsByRecipeId(Connection conn, int recipeId) {
        List<String> ingredients = new ArrayList<>();
        String sql = "SELECT MalzemeAdi FROM TarifMalzemeleri mt " +
                "JOIN Malzemeler m ON mt.MalzemeID = m.MalzemeID " +
                "WHERE mt.TarifID = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, recipeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ingredients.add(rs.getString("MalzemeAdi"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ingredients;
    }


    // DatabaseHelper sınıfı içinde tarifin malzemelerini güncellemek için bir metot
    public void updateRecipeIngredients(Connection conn, int recipeId, List<String> ingredients) {
        // İlk olarak, mevcut malzemeleri sil
        String deleteSql = "DELETE FROM TarifMalzemeleri WHERE TarifID = ?";

        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setInt(1, recipeId);
            deleteStmt.executeUpdate();

            // Yeni malzemeleri ekle
            String insertSql = "INSERT INTO TarifMalzemeleri (TarifID, MalzemeID) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);

            for (String ingredientName : ingredients) {
                int ingredientId = getOrCreateIngredient(conn, ingredientName);
                if (ingredientId != -1) {
                    insertStmt.setInt(1, recipeId);
                    insertStmt.setInt(2, ingredientId);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Tarifi adına veya adının parçasına göre arama
    public void searchRecipeByName(Connection conn, String tarifAdi) {
        try {
            String query = "SELECT * FROM Tarifler WHERE TarifAdi ILIKE ?"; //ILIKE SQL'de byk kçk harf duyarsız bi şekilde arama yapar
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, "%" + tarifAdi + "%"); //%cake% ifadesi "cake", "chocolate cake" veya "lemon cake"
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) { // her tarifin adını ekrana yazdırabilmek için, rs bitene kadar devamke
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
                    "JOIN TarifMalzemeleri tm ON t.TarifID = tm.TarifID " + //Tarifler ve TarifMalzemeleri tablolarını TarifID üzerinden birleştirir. Böylece her tarifin içerdiği malzemelere erişilir.
                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " + //TarifMalzemeleri ve Malzemeler tablolarını MalzemeID üzerinden birleştirir. Böylece malzemelerin adlarıyla işlem yapılabilir.
                    "WHERE m.MalzemeAdi = ANY (?) " + //Kullanıcının belirttiği malzemelerden herhangi birinin tarife dahil olup olmadığını kontrol eder.
                    "GROUP BY t.TarifID, t.TarifAdi " + //Tarif bazında gruplandırma yapar. Aynı tarifteki malzeme eşleşmelerini tek bir sonuç olarak toplar.
                    "ORDER BY MalzemeSayisi DESC"; //MalzemeSayisi değerine göre tarifleri azalan sırayla sıralar. Yani, en çok eşleşen malzemeye sahip tarifler ilk sırada gelir.
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setArray(1, conn.createArrayOf("VARCHAR", ingredients.toArray())); //ingredients listesini VARCHAR tipinde bir SQL dizisine dönüştürür.
            ResultSet rs = pstmt.executeQuery(); //SQL sorgusu çalıştırılır ve sonuçlar ResultSet nesnesine aktarılır.

            while (rs.next()) {
                System.out.println("Tarif: " + rs.getString("TarifAdi") + ", Eşleşen Malzeme Sayısı: " + rs.getInt("MalzemeSayisi")); //Sonuçlar, tarifin adını ve eşleşen malzeme sayısını ekrana yazdırır.
                //Kullanıcı "egg", "flour", "milk" gibi malzemeler sağladığında, bu metod çalıştırıldığında, örneğin şu türde bir çıktı olabilir:

                //Tarif: Pancake, Eşleşen Malzeme Sayısı: 3
                //Tarif: Cake, Eşleşen Malzeme Sayısı: 2
                //Tarif: Omelette, Eşleşen Malzeme Sayısı: 1 olabilir

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
            String query = "SELECT * FROM Tarifler WHERE Kategori = ?"; //Kategorisi belirtilen değere eşit olan tarifleri filtreler. ? sembolü, daha sonra PreparedStatement ile ayarlanacak olan bir parametreyi temsil eder.
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, kategori); //? yerine kategori parametresinin değeri yerleştirilir. Bu, sorgunun dinamik olarak kullanıcıdan alınan değeri kullanmasını sağlar.
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Tarif: " + rs.getString("TarifAdi") + ", Kategori: " + rs.getString("Kategori"));//Her tarifin adı ve kategorisi ekrana yazdırılır.
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

    public void deleteMalzeme(Connection conn,int malzemeID){
        try{
            String query ="DELETE FROM Malzemeler WHERE MalzemeID = ?";
            PreparedStatement pstmt =conn.prepareStatement(query);
            pstmt.setInt(1,malzemeID);
            pstmt.executeUpdate();
            System.out.println("Malzeme Silindi.");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getCategoriesFromRecipes(Connection conn) {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT Kategori FROM Tarifler"; // Kategorileri tekrar etmeden alıyoruz

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString("Kategori"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categories;
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
    }}

