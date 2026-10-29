# ShopAI — Docker & Yerel Geliştirme Rehberi

Bu dosya, projenin hem terminalden (manuel) hem de Docker üzerinden sorunsuzca nasıl çalıştırılacağını anlatır. Yapılandırma "Hibrit" olarak ayarlanmıştır; yani dosyaları değiştirmeden her iki modda da çalışabilirsiniz.

---

## 💻 Mod 1: Terminalden Çalıştırma (Geliştirme Modu)

Geliştirme aşamasında (Hot-reload, hızlı debug vb.) bu modu kullanın.

### 1. Servisleri Başlatın (Docker lazımdır)
Veritabanı ve Typesense gibi servisler için Docker'ı sadece "altyapı" olarak kullanacağız:
```powershell
# Sadece DB, Redis ve Typesense'i başlatın
docker-compose up -d db redis typesense
```

### 2. AI Service Başlatın
```powershell
cd ai-service
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload
```

### 3. Backend Başlatın
```powershell
cd backend
mvn spring-boot:run
```

### 4. Frontend Başlatın
```powershell
cd frontend
npm install
ng serve
```
> **Erişim:** [http://localhost:4200](http://localhost:4200)

---

## 🐳 Mod 2: Docker ile Çalıştırma (Üretim/Test Modu)

Tüm sistemi tek komutla, izole bir şekilde çalıştırmak istediğinizde:

### 1. Terminaldeki çalışan servisleri durdurun.
### 2. Docker Stack'i başlatın:
```powershell
docker-compose up -d --build
```
> **Erişim:** [http://localhost](http://localhost) (Portsuzdur, çünkü Nginx üzerinden çalışır).

---

## ⚙️ Hibrit Yapı Nasıl Çalışıyor?

Sistemi esnek kılmak için **Spring Boot Environment Overriding** özelliğini kullandık:

- **application.yml**: İçindeki adresler `localhost` olarak ayarlıdır. Terminalden başlattığında bu değerler okunur.
- **docker-compose.yml**: İçindeki `environment` bölümünde bu adresler Docker ağındaki isimlerle (`db`, `ai-service` vb.) ezilir.

**Örnek:**
```yaml
# application.yml (Terminal bunu görür)
url: ${DB_HOST:localhost} 

# docker-compose.yml (Docker bunu üzerine yazar)
environment:
  - DB_HOST=db
```

## ⚠️ Dikkat Edilmesi Gerekenler
1. **Frontend Portu:** Docker'da `http://localhost`, Terminalde `http://localhost:4200` kullanılır. Backend içindeki `OriginHeaderFilter` her iki modu da çevre değişkenleri sayesinde tanır.
2. **Veritabanı Portu:** Docker `3307` portunu dışarı açar. Terminalden bağlanırken `localhost:3307` yerine yerel MySQL kullanıyorsanız `3306`'ya dikkat edin.

---
*Hazırlayan: Antigravity Code Assistant*
