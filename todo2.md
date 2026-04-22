Agentic UI Control — Kapsamlı Özellik ve Güvenlik Planı
VERİTABANI KATMANI
Yeni Tablolar

Agent işlem geçmişi tablosu — AI'ın gerçekleştirdiği her çok adımlı işlemi kayıt altına alan tablo. Hangi kullanıcının, hangi zaman, hangi adımları tamamladığı, hangisinin başarısız olduğu, toplam işlem süresi ve kullanılan token sayısı tutulur
Bekleyen onay tablosu — AI'ın kullanıcıdan onay beklediği işlemleri geçici olarak tutan tablo. Onay token'ı (UUID), son geçerlilik tarihi, onaylanan/reddedilen durumu, işlem planının JSON olarak tutulduğu alan ve hangi agent'ın oluşturduğu bilgisi
Agent işlem adımları tablosu — Çok adımlı bir işlemin her adımını ayrı satır olarak tutan tablo. Hangi üst işleme ait olduğu (foreign key), adım sırası, adım tipi (sepete ekle, kupon uygula, sipariş oluştur vb.), başarı/hata durumu, API yanıtının JSON özeti
Kullanıcı AI tercihleri tablosu — Kullanıcının AI'a verdiği izinler ve tercihler. Onaysız işlem yapılabilir mi, hangi işlem tipleri için otomatik onay var, varsayılan adres ve ödeme yöntemi kullanılsın mı gibi ayarlar

Mevcut Tablolara Eklenecekler

AI konuşmalar tablosuna: Hangi agent oturumunun bir işlem akışı içinde olduğunu gösteren alan, aktif bekleyen onay ID'si için alan
Sipariş tablosuna: Siparişin AI tarafından oluşturulup oluşturulmadığını gösteren bayrak, oluşturan agent tipini tutan alan
Sepet tablosuna: Son AI müdahalesi zamanı, hangi agent'ın en son güncellediği bilgisi
Audit log tablosuna: İşlemin normal kullanıcı mı yoksa AI agent mı tarafından yapıldığını gösteren alan, varsa üst işlem ID'si


BACKEND KATMANI (Spring Boot)
Yeni Endpoint Grupları
Kupon Sorgulama

Kullanıcının o anki sepetine uygulanabilecek tüm kuponları listeleyen endpoint — sepet tutarı, ürün kategorileri ve kullanıcı geçmişine göre filtreler
Belirli bir kupon kodunun o sepete uygulanıp uygulanamayacağını önceden kontrol eden (dry-run) endpoint — gerçekten uygulamaz, sadece sonucu döner

Hızlı Sipariş (Quick Checkout)

Tek API çağrısında şu adımları gerçekleştiren endpoint: kupon doğrulama, stok kontrolü, varsayılan adres seçimi, sipariş oluşturma. Tüm adımlar bir veritabanı transaction'ı içinde çalışır — herhangi bir adım başarısız olursa tümü geri alınır
İşlem öncesi ön doğrulama endpoint'i — gerçek işlem yapmadan "bu işlem başarılı olur mu?" sorusunu yanıtlar, AI'ın kullanıcıya doğru bilgi vermesini sağlar

Onay Yönetimi

AI'ın oluşturduğu işlem planını onay bekler durumda kaydeden endpoint
Kullanıcının bir işlemi onayladığında çağırdığı endpoint — onay token'ı ile işlemi tetikler
Kullanıcının bir işlemi reddettiğinde çağırdığı endpoint — planı iptal eder, varsa sepet değişikliklerini geri alır
Belirli bir onayın hâlâ geçerli olup olmadığını sorgulayan endpoint

AI Agent İç İletişim Endpoint'leri

Python AI servisinin Spring Boot'a güvenli şekilde bağlandığı, yalnızca iç ağdan erişilebilen endpoint grubu. Bu endpoint'ler dışarıdan erişilememeli, servisler arası özel bir kimlik doğrulama anahtarı kullanmalı
Kullanıcı varsayılan adresini dönen endpoint (agent için)
Kullanıcının kayıtlı ödeme yöntemlerini dönen endpoint (hassas veriler maskelenerek — kart numarasının son 4 hanesi, son kullanma tarihi)
Sepet özetini ve uygulanabilir kuponları tek seferde dönen birleşik endpoint

İşlem Durumu

Devam eden veya tamamlanan bir agent işleminin adım adım durumunu dönen endpoint — frontend ilerleme çubuğu için
Kullanıcının tüm agent işlem geçmişini listeleyen endpoint

Güvenlik Katmanı Eklemeleri

Agent işlemleri için ayrı bir yetkilendirme katmanı — normal kullanıcı isteklerinden ayrı log tutulur
Her agent işlemi için maksimum süre sınırı — belirlenen sürede tamamlanmayan işlemler otomatik iptal edilir
İşlem başına maksimum tutar sınırı — belirli tutarın üzerindeki siparişler AI tarafından otomatik oluşturulamaz, mutlaka kullanıcı onayı gerekir
Günlük AI işlem limiti — kullanıcı başına günde kaç AI-tetikli sipariş oluşturulabileceği sınırı
Onay token'larının tek kullanımlık olması ve kısa ömürlü olması (5-10 dakika)
Agent işlem kayıtlarının değiştirilemez (immutable) olması — audit amaçlı

Mevcut Endpoint'lere Eklenecekler

Sepete ekleme endpoint'ine: İsteğin bir agent tarafından mı yoksa kullanıcı tarafından mı geldiğini ayırt eden alan, agent işlemi ise hangi üst işleme ait olduğu
Sipariş oluşturma endpoint'ine: Agent kaynaklı işlemlerde onay token'ı doğrulaması zorunluluğu
Tüm veri değiştiren endpoint'lere: Agent işlemi bayrağı geldiğinde ek audit log kaydı


PYTHON AI SERVİSİ (LangGraph)
Yeni Agent'lar
Checkout Orchestration Agent

Kullanıcı "siparişimi tamamla" veya benzer bir şey dediğinde devreye girer
Önce mevcut sepeti kontrol eder, stok durumunu doğrular
Kullanılabilir kuponları sorgular ve en avantajlısını önerir
Varsayılan adresi ve ödeme yöntemini alır
Tüm bu bilgileri derleyerek kullanıcıya özet bir onay planı sunar
Onay gelirse sıralı API çağrılarını gerçekleştirir
Herhangi bir adımda hata olursa önceki adımları geri almaya çalışır ve kullanıcıya ne olduğunu açıklar

Navigation & Status Agent

"Siparişlerim nerede", "hesabıma git", "favorilerim" gibi navigasyon isteklerini işler
Sadece sayfaya yönlendirmekle kalmaz; sayfaya gidip ilgili veriye odaklanma talimatı da ekler
Belirli bir siparişin durumunu sorgulayıp chatbot üzerinden özetler

Multi-Step Executor

Birden fazla API çağrısını belirli bir sırayla yürüten genel amaçlı modül
Her adımı kayıt altına alır
Bir adım başarısız olduğunda "buraya kadar şunlar yapıldı, şu adımda hata oluştu" şeklinde detaylı geri bildirim üretir
Geri alma (rollback) listesi tutar — hangi adımlar geri alınabilir, hangileri alınamaz

Pre-validation Agent

Herhangi bir destructive işlem (sipariş oluşturma, sepet temizleme) yapmadan önce ön doğrulama API'sini çağırır
Stok yeterliliği, kupon geçerliliği, adres eksiksizliği gibi kontrolleri önceden yapar
Kullanıcıya "şunu yapacağım ama şu sorunu fark ettim" şeklinde proaktif uyarı verebilir

Mevcut Agent'lara Eklenecekler
Supervisor Agent'a

Yeni intent tipleri: CHECKOUT_FLOW, NAVIGATE_TO, ORDER_STATUS, MULTI_STEP
Bir işlemin tek adımlı mı yoksa çok adımlı mı olduğunu ayırt eden mantık
Çok adımlı işlemlerde önce ön doğrulama, sonra onay planı, sonra uygulama sıralamasını zorunlu kılan akış

Sepet Agent'ına

"En ucuz olanı ekle" işleminde onay adımı eklenmesi
Sepete ekleme sonrası otomatik kupon kontrolü ve önerisi
Sepetteki ürün sayısı veya tutarı belirli bir eşiği geçtiğinde kullanıcıyı bilgilendirme

Güvenlik Eklemeleri

Çok adımlı işlemlerde her API çağrısından önce JWT geçerliliği yeniden kontrol edilir
İşlem planı oluşturulurken kullanıcı kimliği bir kez doğrulanır ve plan içine gömülür — sonraki adımlar bu gömülü kimliği kullanır, dışarıdan kimlik enjeksiyonu mümkün olmaz
Onay planları imzalanır — kullanıcıya gösterilen plan ile backend'in çalıştırdığı plan aynı olduğu doğrulanır
Destructive işlemler için ayrı prompt injection filtresi — "siparişimi sil", "tüm verilerimi temizle" gibi geri dönüşü zor işlemler ekstra katmandan geçer
Agent'ların yalnızca kendi araç (tool) setlerini kullanabilmesi — bir agent başka bir agent'ın araçlarını doğrudan çağıramaz, her şey supervisor üzerinden geçer


FRONTEND KATMANI (Angular)
Yeni Bileşenler
Agent Onay Kartı

Chatbot içinde açılan özel bir UI bloğu
"Şu işlemi yapacağım" başlığı altında adım adım liste gösterir
Her adımın yanında ne anlama geldiği kısa açıklaması (ör. "Varsayılan adresiniz: X sokak")
Onay ve red butonları
Onay süresinin dolmasına kalan süreyi gösteren geri sayım
Kullanıcı "hayır" derse hangi adımların geri alınacağını gösterir

Agent İlerleme Göstergesi

İşlem başladıktan sonra chatbot içinde adım adım ilerlemeyi gösteren UI
Her adım tamamlandıkça checkmark, devam edenler spinner, bekleyenler gri nokta gösterir
"Sepete eklendi ✓ → Kupon uygulandı ✓ → Sipariş oluşturuluyor ⏳" gibi
Hata durumunda kırmızı çarpı ve açıklama

Agent İşlem Geçmişi Paneli

Kullanıcı profil sayfasında "AI ile yaptığım işlemler" sekmesi
Her işlem için: zaman, ne yapıldığı, başarılı/başarısız, oluşturulan sipariş varsa linki

Onay Bildirimi

AI bir işlem planı hazırladığında chatbot dışında da görünecek küçük bir bildirim banner'ı
"AI bir işlem hazırladı, onaylamak ister misiniz?" — tıklandığında chatbot'a yönlendirir
Kullanıcı chatbot'u kapattıysa bile kaybolmaz

Yeni Servisler
Agent Köprü Servisi

AI'dan gelen aksiyon tipine göre uygun Angular servisini çağıran merkezi servis
Router ile entegre — NAVIGATE aksiyonlarını işler
İşlem durumu observable'ı — ilerleme göstergesi bileşeni buna abone olur
Onay bekleyen işlem observable'ı — onay bildirimi bileşeni buna abone olur

Onay Yönetim Servisi

Bekleyen onayları tutar
Onay token'larını backend'e gönderir
Onay süresi dolmadan önce kullanıcıya hatırlatma tetikler
Red durumunda backend'e bildirir

Mevcut Yapıya Eklenecekler
Chatbot Bileşenine

Normal mesaj baloncuğu dışında "onay kartı" tipinde özel mesaj render desteği
İlerleme göstergesi tipinde özel mesaj render desteği
İşlem sırasında metin girişini devre dışı bırakma — işlem bitene kadar yeni mesaj gönderilemesin
"İşlemi iptal et" butonu — devam eden çok adımlı işlemi durdurma

Auth Servisine

Agent işlemleri sırasında session'ın dolup dolmadığını kontrol etme — uzun süren işlemlerde token yenilenebilmeli
İşlem ortasında session sona ererse işlemi askıya alma ve giriş sonrası kaldığı yerden devam etme

Hata Yönetimine

Agent işlemi sırasında oluşan hataları kullanıcı dostu dille açıklama
"Stok bitti", "kupon süresi doldu", "adres eksik" gibi durumları düz metin yerine actionable hata olarak gösterme — "Adresi güncellemek ister misiniz?" gibi


GÜVENLİK KATMANI (Tüm Servisleri Kesen)
Onay Mekanizması Güvenliği

Her destructive işlem (sipariş oluşturma, sepet temizleme, adres değiştirme) kullanıcı onayı olmadan gerçekleştirilmez — bu kural backend'de de zorunlu tutulur, frontend'i bypass etmek mümkün olmaz
Onay token'ları tek kullanımlık, kısa ömürlü ve imzalı olmalı
Onay verilen plan ile çalıştırılan plan aynı olmalı — backend onay anında planı hash'ler, çalıştırma anında yeniden hash'leyerek karşılaştırır
Onay token'ı kullanıldıktan sonra hemen geçersiz kılınmalı

İşlem Limitleri

Kullanıcı başına saatlik maksimum agent işlem sayısı
Tekli agent işleminde oluşturulabilecek maksimum sipariş tutarı
Günlük agent üzerinden oluşturulabilecek maksimum sipariş adedi
Aynı ürünü kısa sürede tekrar tekrar sepete eklemeye karşı koruma
Tüm limitler aşıldığında hem loglama hem de kullanıcıya bildirim

Prompt Injection — Agent İşlemleri İçin Ek Katman

Mevcut 3 katmanlı korumaya ek olarak çok adımlı akışlarda her adımdan önce tekrar kontrol
"Siparişimi oluştur ama adresi şu kişiye gönder" gibi manipülatif istekleri tespit etme — adres değişikliği her zaman kullanıcının kayıtlı adresleriyle sınırlı, serbest metin adres kabul edilmez
"Şu kuponu uygula" dediğinde kuponun gerçekten o kullanıcıya ait veya herkese açık olduğu doğrulanır

Veri İzolasyonu — Ek Kontroller

Agent bir işlem planı oluştururken yalnızca o kullanıcının verilerine erişir — başka kullanıcının adresi, kuponu veya siparişi plan içine giremez
Ödeme bilgileri hiçbir zaman AI servisine açık olarak iletilmez — yalnızca "kayıtlı kart son 4 hane: XXXX kullanılacak" şeklinde maskelenmiş özet
Agent tarafından oluşturulan siparişlerde kullanıcı kimliği iki kez doğrulanır: işlem planı oluşturulurken ve sipariş oluşturulurken

Audit & İzlenebilirlik

Agent tarafından yapılan her işlem ayrıca audit log'a düşer ve "AI kaynaklı" olarak işaretlenir
Bir işlemin hangi chat mesajından tetiklendiği, hangi agent kararlarının alındığı, hangi API çağrılarının yapıldığı iz bırakır
Başarısız veya iptal edilen agent işlemleri de eksiksiz loglanır
Belirli bir zaman diliminde anormal sayıda agent işlemi yapılırsa otomatik uyarı mekanizması

Güvenlik Test Senaryoları (Mevcut Section 11'e Eklenecekler)

Onay token'ını tekrar kullanmaya çalışma testi
Onay token'ı süresi dolduktan sonra işlemi tetikleme testi
Onay verilen planı değiştirerek farklı sipariş oluşturmaya çalışma testi
Agent aracılığıyla başka kullanıcının adresine sipariş göndermeye çalışma testi
Günlük işlem limitini aşmaya çalışma testi
İşlem sırasında session'ın kasıtlı olarak sona erdirilmesi testi
Onay ekranını atlayarak doğrudan quick-checkout endpoint'ine istek atmaya çalışma testi
Chat mesajına sahte bir onay token'ı yerleştirme testi


KULLANICI DENEYİMİ EKLENTİLERİ

Kullanıcı ilk kez AI-tetikli bir işlem yaparken kısa bir "AI neler yapabilir?" tanıtım ekranı
AI tercihler sayfası — kullanıcı hangi işlem tiplerinde otomatik onay vermek istediğini ayarlayabilir (ör. "50 TL altı alışverişlerde onay isteme")
Her agent işlemi sonrası kısa geri bildirim isteme — "AI size yardımcı olabildi mi?"
Hatalı veya beklenmedik agent davranışını bildirme butonu — doğrudan chat içinden
Agent işlemi sırasında kullanıcıya "Bu işlem şu kadar sürebilir" beklenti yönetimi mesajı


IMPLEMENTATION SIRASINA GÖRE ÖNERİLEN EKLEMELER

Önce backend onay mekanizmasını ve quick-checkout endpoint'ini kur — güvenlik temeli bu
Frontend onay kartı bileşenini ve agent köprü servisini ekle
Python tarafında checkout orchestration agent'ı yaz, önce tek adımlı akışlarla test et
Çok adımlı akışları ekle, rollback mekanizmasını test et
Limitler ve audit katmanını ekle
Kullanıcı tercihleri ve AI ayarlar sayfasını ekle
Güvenlik test senaryolarını çalıştır