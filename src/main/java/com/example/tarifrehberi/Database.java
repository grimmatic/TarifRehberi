package com.example.tarifrehberi;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

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
            String createRecipeIngredientsTable = "CREATE TABLE IF NOT EXISTS TarifMalzeme (" +
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

    public void addRecipe(Connection conn, String tarifAdi, String kategori, int hazirlamaSuresi, String talimatlar, Map<String, Map<String, Object>> malzemeler) {
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
            if (true) {

                int tarifID = rs.getInt(1);

                // Malzemeleri ekle
                for (Map.Entry<String, Map<String, Object>> entry : malzemeler.entrySet()) {
                    String malzemeAdi = entry.getKey();
                    Map<String, Object> malzemeDetay = entry.getValue();
                    Float miktar = (Float) malzemeDetay.get("miktar");
                    String birim = (String) malzemeDetay.get("birim");

                    // Malzeme var mı kontrol et yoksa ekle
                    int malzemeID = getOrCreateIngredient(conn, malzemeAdi, birim);

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
    public int getOrCreateIngredient(Connection conn, String malzemeAdi, String birim) {
        String checkQuery = "SELECT MalzemeID FROM Malzemeler WHERE MalzemeAdi = ?";
        String insertQuery = "INSERT INTO Malzemeler (MalzemeAdi, ToplamMiktar, MalzemeBirim, BirimFiyat) VALUES (?, 0, ?, 10.0)";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
            // Mevcut malzemeyi kontrol et
            checkStmt.setString(1, malzemeAdi);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    int malzemeId = rs.getInt("MalzemeID");
                    System.out.println("Mevcut malzeme bulundu. MalzemeID: " + malzemeId);
                    return malzemeId;
                }
            }

            // Malzeme yoksa, yeni ekle
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, malzemeAdi);
                insertStmt.setString(2, birim);
                int affectedRows = insertStmt.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Malzeme oluşturulamadı, hiçbir satır etkilenmedi.");
                }

                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newMalzemeId = generatedKeys.getInt(1);
                        System.out.println("Yeni malzeme eklendi. MalzemeID: " + newMalzemeId);
                        return newMalzemeId;
                    }
                }

                // getGeneratedKeys başarısız olursa, son eklenen ID'yi almak için
                try (Statement stmt = conn.createStatement();
                     ResultSet lastIdRs = stmt.executeQuery("SELECT last_insert_rowid() as last_id")) {
                    if (lastIdRs.next()) {
                        int lastId = lastIdRs.getInt("last_id");
                        System.out.println("Son eklenen malzeme ID'si manuel olarak alındı: " + lastId);
                        return lastId;
                    }
                }

                throw new SQLException("Malzeme oluşturuldu ancak ID alınamadı.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQL Hatası: " + e.getMessage());
        }
        return -1; // Hata durumunda
    }


    // Tarif-Malzeme ilişkisi eklemece yaani bir tarifin hangi malzemeden ne kadar içerdiğini
    public void addRecipeIngredient(Connection conn, int tarifID, int malzemeID, float miktar) {
        try {
            String query = "INSERT INTO TarifMalzeme (TarifID, MalzemeID, MalzemeMiktar) VALUES (?, ?, ?)";
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
                    "JOIN TarifMalzeme tm ON t.TarifID = tm.TarifID " +
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
        String sql = "SELECT MalzemeAdi FROM TarifMalzeme mt " +
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
    public void updateRecipeIngredients(Connection conn, int recipeId, List<Map<String, Object>> ingredients) {
        try {
            Map<String, Integer> existingIngredients = new HashMap<>();
            String selectSql = "SELECT m.MalzemeID, m.MalzemeAdi FROM TarifMalzeme tm JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID WHERE tm.TarifID = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, recipeId);
                ResultSet rs = selectStmt.executeQuery();
                while (rs.next()) {
                    existingIngredients.put(rs.getString("MalzemeAdi"), rs.getInt("MalzemeID"));
                }
            }


            String upsertSql = "INSERT INTO TarifMalzeme (TarifID, MalzemeID, MalzemeMiktar) VALUES (?, ?, ?) ON CONFLICT (TarifID, MalzemeID) DO UPDATE SET MalzemeMiktar = ?";
            try (PreparedStatement upsertStmt = conn.prepareStatement(upsertSql)) {
                for (Map<String, Object> ingredient : ingredients) {
                    String ingredientName = (String) ingredient.get("name");
                    double amount = (double) ingredient.get("amount");
                    String unit = (String) ingredient.get("unit");

                    int ingredientId = existingIngredients.getOrDefault(ingredientName, -1);
                    if (ingredientId == -1) {
                        ingredientId = getOrCreateIngredient(conn, ingredientName, unit);
                    }

                    upsertStmt.setInt(1, recipeId);
                    upsertStmt.setInt(2, ingredientId);
                    upsertStmt.setDouble(3, amount);
                    upsertStmt.setDouble(4, amount);
                    upsertStmt.addBatch();
                }
                upsertStmt.executeBatch();
            }


            Set<String> updatedIngredientNames = ingredients.stream().map(i -> (String)i.get("name")).collect(Collectors.toSet());
            String deleteSql = "DELETE FROM TarifMalzeme WHERE TarifID = ? AND MalzemeID = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                for (Map.Entry<String, Integer> entry : existingIngredients.entrySet()) {
                    if (!updatedIngredientNames.contains(entry.getKey())) {
                        deleteStmt.setInt(1, recipeId);
                        deleteStmt.setInt(2, entry.getValue());
                        deleteStmt.addBatch();
                    }
                }
                deleteStmt.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getIngredientDetailsForRecipe(Connection conn, int tarifID) {
        List<String> ingredientDetails = new ArrayList<>();
        String query = "SELECT m.MalzemeAdi, tm.MalzemeMiktar, m.MalzemeBirim " +
                "FROM TarifMalzeme tm " +
                "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                "WHERE tm.TarifID = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, tarifID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String malzemeAdi = rs.getString("MalzemeAdi");
                double miktar = rs.getDouble("MalzemeMiktar");
                String birim = rs.getString("MalzemeBirim");

                String detail = String.format("%s - %.2f %s", malzemeAdi, miktar, birim);
                ingredientDetails.add(detail);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ingredientDetails;
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
                    "JOIN TarifMalzeme tm ON t.TarifID = tm.TarifID " + //Tarifler ve TarifMalzeme tablolarını TarifID üzerinden birleştirir. Böylece her tarifin içerdiği malzemelere erişilir.
                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " + //TarifMalzeme ve Malzemeler tablolarını MalzemeID üzerinden birleştirir. Böylece malzemelerin adlarıyla işlem yapılabilir.
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
                    "JOIN TarifMalzeme tm ON t.TarifID = tm.TarifID " +
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
                    "JOIN TarifMalzeme tm ON t.TarifID = tm.TarifID " +
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
                    "JOIN TarifMalzeme tm ON t.TarifID = tm.TarifID " +
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
    }



    public void deleteIngredientFromRecipe(Connection conn, int tarifID, String malzemeAdi) {
        String query = "DELETE FROM TarifMalzeme WHERE TarifID = ? AND MalzemeID = (SELECT MalzemeID FROM Malzemeler WHERE MalzemeAdi = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, tarifID);
            pstmt.setString(2, malzemeAdi);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

//    q

    public void addIngredientToRecipe(Connection conn, int tarifID, int malzemeID, double miktar) {
        String query = "INSERT INTO TarifMalzeme (TarifID, MalzemeID, MalzemeMiktar) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, tarifID);
            pstmt.setInt(2, malzemeID);
            pstmt.setDouble(3, miktar);
            int affectedRows = pstmt.executeUpdate();

            System.out.println("TarifMalzeme ekleme sonucu: " + affectedRows + " satır etkilendi.");

            if (affectedRows == 0) {
                throw new SQLException("Malzeme tarife eklenemedi, hiçbir satır etkilenmedi.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("TarifMalzeme ekleme hatası: " + e.getMessage());
        }
    }

    // Belirli bir tarifin malzemelerini veritabanından alma
    public List<String> getIngredientsFromDatabase(Connection conn) {
        List<String> ingredients = new ArrayList<>();
        String sql = "SELECT MalzemeAdi FROM Malzemeler"; // Tüm malzemeleri seçiyoruz

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ingredients.add(rs.getString("MalzemeAdi")); // Malzeme adını listeye ekliyoruz
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ingredients; // Malzeme listesini döndür
    }

    public String getUnitForIngredient(Connection conn, String ingredient) {
        String unit = null;
        String query = "SELECT birim FROM Malzemeler WHERE malzeme_adi = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, ingredient);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                unit = rs.getString("birim");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return unit;
    }





    private void updateIngredientUnit(Connection conn, int malzemeID, String birim) {
        String query = "UPDATE Malzemeler SET MalzemeBirim = ? WHERE MalzemeID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, birim);
            pstmt.setInt(2, malzemeID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();}
    }




}