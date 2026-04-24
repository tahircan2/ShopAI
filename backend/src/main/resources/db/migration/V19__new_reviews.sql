-- ============================================================
-- ShopAI E-Commerce — reviews tablosu INSERT komutları
-- 50 ürün × 3 kullanıcı = 150 kayıt
-- user_id: 1, 2, 3 (UNIQUE KEY: user_id + product_id)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- AKILLI TELEFONLAR
-- ─────────────────────────────────────────────────────────────

-- 35: Samsung Galaxy S24 Ultra 256GB Titanium
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(35, 1, 5, 'Amiral gemisi deneyimi eksiksiz', 'S24 Ultra''yı iki haftadır kullanıyorum ve özellikle kamera sistemi beklentilerimin çok ötesinde. 200MP ana sensör gece çekimlerinde inanılmaz detay sunuyor. S Pen entegrasyonu da iş hayatımda büyük kolaylık sağlıyor. Titanium kasa hem hafif hem de son derece dayanıklı hissettiriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(35, 2, 5, 'Zoom performansı rakipsiz', 'Fotoğraf tutkunu biri olarak S24 Ultra''nın 10x optik zoom kapasitesini test ettim. Konserlerde ve doğa fotoğrafçılığında kullandığımda sonuçlar profesyonel kamera seviyesine yakın çıktı. Pil ömrü de yoğun kullanımda bile bir günü rahatlıkla kapsıyor. Kesinlikle tavsiye ederim.', TRUE, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(35, 3, 4, 'Güçlü ama büyük ve ağır', 'Cihazın performansı ve ekranı mükemmel, ancak 232 gram ağırlık uzun süreli kullanımda yorucu oluyor. Tek elle kullanmak neredeyse imkânsız. Kamera ve S Pen özellikleri gerçekten etkileyici; bu konularda şikâyetim yok. Boyutuna alışabilirseniz piyasadaki en iyi telefon.', TRUE, DATE_SUB(NOW(), INTERVAL 15 DAY));

-- 36: Apple iPhone 15 Pro 128GB Doğal Titanyum
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(36, 1, 5, 'Titanyum kasa fark yaratıyor', 'iPhone 15 Pro''ya geçişim mükemmel oldu. Titanyum kasa önceki modele kıyasla belirgin biçimde daha hafif ve elde daha iyi hissettiriyor. A17 Pro çipi sayesinde her uygulama anlık açılıyor, oyun performansı üst düzey. USB-C geçişi de aslında çok pratik.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(36, 2, 5, 'ProRAW fotoğraflar muhteşem', 'Profesyonel içerik üreticisi olarak iPhone 15 Pro''nun kamera sistemini yoğun kullanıyorum. ProRAW formatındaki fotoğraflar Lightroom''da düzenlendiğinde inanılmaz esneklik sunuyor. 4K 60fps ProRes video kaydı da harika. Action button özelleştirmesi ise iş akışımı hızlandırdı.', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(36, 3, 4, 'Harika telefon, depolama kısıtlayıcı', 'Telefon her açıdan mükemmel ama 128GB ProRAW ve ProRes video kullanıcısı için hızla doluyor. 256GB versiyonu seçmek daha mantıklıydı. Bunun dışında performans, ekran kalitesi ve yapı kalitesi açısından hiçbir şikâyetim yok. Apple ekosistemine derinlemesine dahilseniz bu telefon için biçilmiş kaftan.', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- 37: Xiaomi 14 Pro 512GB Siyah
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(37, 1, 5, 'Leica kamera ortaklığı gerçekten hissediliyor', 'Xiaomi 14 Pro''nun Leica optik sistemi sadece bir pazarlama iddiası değil, gerçekten fark yaratıyor. Renk bilimi ve keskinlik amiral sınıfı telefonlarla başa baş gidiyor. Snapdragon 8 Gen 3 performansı da kusursuz. 512GB depolama uzun süre yetecek kadar geniş.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(37, 2, 4, 'Mükemmel donanım, MIUI alışkanlık istiyor', 'Donanım açısından kusursuz bir telefon: hızlı şarj, güçlü işlemci, etkileyici ekran. Ancak MIUI arayüzü Samsung ve Apple''a alışkın biri için adapte olmayı gerektiriyor. Birkaç hafta sonra alışıyorsunuz ama başlangıçta biraz zaman alıyor. Fiyat/performans oranı çok başarılı.', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(37, 3, 5, '120W şarj inanılmaz', 'Telefonun en etkileyici özelliği kesinlikle 120W kablolu şarj hızı. Tamamen boşaltan telefonu 20 dakikada doluyor; bu özellik hayat kalitemi ciddi ölçüde artırdı. Ekran kalitesi ve kamera sistemi de üst düzey. Fiyatını hak eden güçlü bir amiral gemisi.', TRUE, DATE_SUB(NOW(), INTERVAL 18 DAY));

-- 38: Google Pixel 8 Pro 256GB Obsidyen
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(38, 1, 5, 'Yapay zeka özellikleri gerçek fark yaratıyor', 'Pixel 8 Pro''nun Magic Eraser ve Photo Unblur gibi AI özellikleri akıllı telefon fotoğrafçılığını bambaşka bir seviyeye taşıyor. Yüzey sıcaklığı ölçer ve yedi yıl yazılım güncellemesi taahhüdü de etkileyici. Saf Android deneyimi yaşamak isteyenler için ideal seçim.', TRUE, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(38, 2, 5, 'Gece fotoğrafçılığının şampiyonu', 'Computational photography konusunda Pixel serisinin liderliğini 8 Pro da sürdürüyor. Karanlık mekânlarda çekilen fotoğraflar diğer telefonların çok üzerinde. Tensor G3 çipinin canlı tercüme ve ses transkripsiyon özellikleri de günlük kullanımda son derece yararlı.', TRUE, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(38, 3, 4, 'Yazılım mükemmel, batarya ortalama', 'Yazılım deneyimi ve kamera kalitesi açısından piyasanın en iyisi. Ancak batarya ömrü yoğun kullanımda akşama kadar zor tutuyor; bu konuda S24 Ultra veya iPhone 15 Pro Max kadar başarılı değil. Kablosuz şarj hızı da rakiplerinin gerisinde. Yine de fotoğraf önceliklendirenler için güçlü bir seçenek.', TRUE, DATE_SUB(NOW(), INTERVAL 22 DAY));

-- 39: OnePlus 12 256GB Silky Black
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(39, 1, 5, 'Hız tutkunları için biçilmiş kaftan', 'OnePlus''ın "Never Settle" felsefesi 12 modelde zirveye ulaşmış. Snapdragon 8 Gen 3 ve 16GB RAM kombinasyonu her uygulamayı anında açıyor. OxygenOS de saf Android''e en yakın, en temiz arayüzlerden biri. Hasselblad kamera ortaklığı da renk üretimini ciddi ölçüde iyileştirmiş.', TRUE, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(39, 2, 5, '100W şarj ve güçlü pil birlikteliği', 'OnePlus 12''nin 100W SuperVOOC şarj hızı ve 5400mAh büyük piliyle kombinasyonu mükemmel bir denge sunuyor. Pil ömrü uzun, şarj süresi kısa; ikisini bir arada bu kadar iyi sunan az model var. Ekran kalitesi ve dokunuş hassasiyeti de üst düzey.', TRUE, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(39, 3, 4, 'Güçlü ama kamera rakiplerinin gerisinde', 'Performans ve şarj hızı açısından amiral sınıfının en iyilerinden biri. Ancak kamera sistemi aynı fiyat segmentindeki iPhone ve Samsung''un biraz gerisinde kalıyor; özellikle gece çekimlerinde fark belirgin. Fiyat/performans oranı ise son derece başarılı.', TRUE, DATE_SUB(NOW(), INTERVAL 25 DAY));

-- ─────────────────────────────────────────────────────────────
-- DİZÜSTÜ BİLGİSAYARLAR
-- ─────────────────────────────────────────────────────────────

-- 40: Apple MacBook Pro 14" M3 Pro 512GB Uzay Siyahı
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(40, 1, 5, 'Profesyonel iş yükü için mükemmel', 'Video editörü olarak M3 Pro çipinin performansını zorlu projelerle test ettim. 4K ProRes video render süreleri önceki Intel MacBook Pro''mun üçte biri kadar sürüyor. Tüm gün pil dayanıyor ve fan sesi neredeyse hiç çıkmıyor. Uzay Siyahı rengi de görsel olarak son derece etkileyici.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(40, 2, 5, 'Pil ömrü gerçekten inanılmaz', 'Yazılım geliştiricisi olarak MacBook Pro 14'' M3 Pro''yu yoğun kullanıyorum. Xcode derlemelerinde M3 Pro çipi rakipsiz hız sunuyor. Ancak en etkileyici özellik pil ömrü: 12 saat kesintisiz kodlama yapabiliyorum. Ekranın 120Hz ProMotion ve P3 geniş renk gamı da göz yormayan mükemmel bir deneyim sunuyor.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(40, 3, 5, 'Pahalı ama hak ediyor', 'Fiyat etiketini gördüğümde tereddüt ettim ama satın aldıktan sonra pişmanlık duymadım. Ses sistemi bu boyutta bir dizüstünde beklenmeyecek kadar zengin. Termal yönetim mükemmel; video render ederken bile klavye ılık kalıyor. macOS entegrasyonu iPhone ve iPad ile son derece akıcı çalışıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 19 DAY));

-- 41: Lenovo ThinkPad X1 Carbon Gen 12 Intel i7
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(41, 1, 5, 'Kurumsal kullanımın altın standardı', 'On yıldır ThinkPad kullanan biri olarak Gen 12''nin evrimini memnuniyetle takip ediyorum. Klavye hâlâ sektörün en iyisi; uzun yazı seanslarında bile el yorgunluğu hissettirmiyor. i7 işlemci iş yüklerini kolaylıkla kaldırıyor. 1.12kg ağırlığıyla bu güce sahip bir dizüstü bulmak çok zor.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(41, 2, 4, 'Dayanıklı ve hafif, ekran vasat', 'MIL-STD-810H sertifikası ciddiye alınması gereken bir dayanıklılık standardı; çantamda biraz sert davransam da hiçbir sorun yaşamadım. Ancak 2.8K IPS panel parlak güneş ışığında rakiplerine kıyasla yeterince parlak değil. İş odaklı kullanıcılar için ideal, medya tüketimi önceliklendirenler için başka seçeneklere bakılabilir.', TRUE, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(41, 3, 5, 'Pil ömrü ve bağlantı seçenekleri harika', 'ThinkPad''in Thunderbolt 4, USB-A ve HDMI gibi fiziksel portları koruması iş hayatımda büyük kolaylık sağlıyor. Adaptör taşımak zorunda kalmıyorum. Pil 15 saate kadar dayanıyor; uçak yolculuklarında şarj adaptörü çantada kalıyor. Güvenlik özellikleri (IR kamera + parmak izi) de eksiksiz.', TRUE, DATE_SUB(NOW(), INTERVAL 23 DAY));

-- 42: ASUS ROG Zephyrus G14 AMD Ryzen 9 RTX 4060
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(42, 1, 5, 'Taşınabilir oyun bilgisayarının en iyisi', 'ROG Zephyrus G14''ü üç aydır hem oyun hem de 3D modelleme için kullanıyorum. RTX 4060 AAA oyunları 1080p''de maksimum ayarlarda akıcı çalıştırıyor. Ryzen 9 işlemci render işlemlerinde de son derece güçlü. 14 inç formunda bu performansı sunan başka bir model yok.', TRUE, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(42, 2, 5, 'AniMe Matrix ekran muhteşem', 'Kapak üzerindeki AniMe Matrix LED dizisi belki gereksiz bir özellik gibi görünebilir ama toplantılarda ve LAN partilerde her zaman dikkat çekiyor. Performans konusunda ise hiçbir şikâyetim yok: oyunlarda sorunsuz 120+ fps, yaratıcı uygulamalarda da yeterli hız. Soğutma sistemi de ses çıkarmadan etkili iş yapıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(42, 3, 4, 'Güçlü ama ısınma yönetilebilir', 'Uzun oyun seanslarında alt yüzey oldukça ısınıyor; bir yükseltici stand kesinlikle tavsiye ederim. Bunun dışında performans ve ekran kalitesi beklentilerimin üzerinde. 165Hz QHD panel oyunlarda gerçekten fark yaratıyor. Fiyat/performans oranı oyun dizüstü segmentinde en iyi seçeneklerden.', TRUE, DATE_SUB(NOW(), INTERVAL 21 DAY));

-- 43: Dell XPS 15 9530 Intel i9 OLED Dokunmatik
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(43, 1, 5, 'OLED ekran içerik üreticileri için vazgeçilmez', 'Grafik tasarımcısı olarak renk doğruluğu benim için kritik. XPS 15''in OLED paneli %100 DCI-P3 renk gamıyla profesyonel ihtiyaçlarımı eksiksiz karşılıyor. Siyah seviyeleri ve kontrast oranı olağanüstü. i9 işlemci de Adobe uygulamalarını terlemeden kaldırıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(43, 2, 4, 'Performans mükemmel, pil ömrü kısa', 'OLED ekranın güzelliği ve i9 işlemcinin gücü yadsınamaz. Ancak bu güç kombinasyonu pil ömrünü zorluyor; yoğun kullanımda 5-6 saatte şarj gerekiyor. Adaptör de oldukça büyük ve ağır. Masaüstü kullanım ağırlıklıysa sorun değil; sık seyahat edenler dikkatli düşünmeli.', TRUE, DATE_SUB(NOW(), INTERVAL 17 DAY)),
(43, 3, 5, 'Dokunmatik ekran yaratıcı iş akışını hızlandırıyor', 'Dokunmatik OLED ekran başlangıçta lüks gibi görünse de alıştıktan sonra vazgeçilemez hale geliyor. Fotoğraf rötuşu ve video düzenleme sırasında direkt ekrana dokunabilmek iş hızımı ciddi artırdı. Yapım kalitesi de premium; alüminyum kasa düşmelere karşı güven veriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 28 DAY));

-- ─────────────────────────────────────────────────────────────
-- TABLETLER
-- ─────────────────────────────────────────────────────────────

-- 44: Apple iPad Pro 12.9" M2 256GB Wi-Fi
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(44, 1, 5, 'Dijital sanat için mükemmel tuval', 'İllüstratör olarak iPad Pro 12.9''u Apple Pencil 2 ile birlikte kullanıyorum. M2 çipi Procreate''de en karmaşık katmanları bile yavaşlamadan işliyor. Liquid Retina XDR ekranın parlaklığı ve renk doğruluğu harika. ProMotion teknolojisi çizim deneyimini gerçek kâğıt hissine yaklaştırıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(44, 2, 5, 'Video izleme deneyimi eşsiz', 'Bu büyüklükte ve kalitede bir ekranla film izlemek sinema deneyimine yakın. Dolby Vision ve HDR desteği içerikleri olağanüstü sunarken ses sistemi de boyutunun çok ötesinde zengin. M2 performansı da her uygulamayı tertemiz çalıştırıyor. iPadOS ile üretkenlik konusunda da giderek güçleniyor.', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(44, 3, 4, 'Harika tablet, aksesuarlar çok pahalı', 'iPad Pro başlı başına mükemmel ama tam potansiyele ulaşmak için Apple Pencil 2 ve Magic Keyboard gerekiyor. Bu aksesuarlar tabletten neredeyse yarı fiyat kadar ek maliyet çıkarıyor. Donanım ve yazılım kalitesi tartışmasız en iyi; sadece toplam maliyeti hesaba katarak karar verin.', TRUE, DATE_SUB(NOW(), INTERVAL 18 DAY));

-- 45: Samsung Galaxy Tab S9+ 256GB Grafite Wi-Fi
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(45, 1, 5, 'Android tablet segmentinin lideri', 'Galaxy Tab S9+''ı iki aydır hem iş hem de eğlence amaçlı kullanıyorum. Dynamic AMOLED ekranın renk canlılığı ve 120Hz yenileme hızı olağanüstü. S Pen dahil gelmesi de büyük artı: ek aksesuar maliyeti yok. DeX modu sayesinde harici monitöre bağladığımda masaüstü bilgisayar gibi çalışıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(45, 2, 4, 'İyi tablet, yazılım desteği kısa', 'Donanım açısından iPad Pro ile ciddi rekabet edebilecek kalitede. Ancak dört yıllık Android ve beş yıl güvenlik güncellemesi iPad''in ömründen kısa. Bunun dışında ekran, performans ve S Pen işlevselliği harika. Samsung ekosistemine dahilseniz mükemmel bir tamamlayıcı.', TRUE, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(45, 3, 5, 'Fotoğraf düzenleme için süper', 'Samsung''un renk kalibrasyon altyapısı ve AMOLED ekranının renk gamı fotoğraf düzenlemeyi keyifli kılıyor. Lightroom Mobile Tab S9+''da son derece akıcı çalışıyor. Snapdragon 8 Gen 2 de yoğun iş yüklerini sorunsuz kaldırıyor. Hafif gövdesiyle uzun süre elde tutmak da yorucu olmuyor.', TRUE, DATE_SUB(NOW(), INTERVAL 22 DAY));

-- 46: Lenovo Tab P12 Pro 256GB AMOLED
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(46, 1, 4, 'Fiyat/performans açısından değerli', 'Tab P12 Pro, iPad ve Galaxy Tab''ın çok üzerindeki fiyatlarına katlanmak istemeyenler için güçlü bir alternatif. 12.6 inç AMOLED ekranı renk üretimi ve siyah seviyeleri açısından etkileyici. Snapdragon 870 performansı günlük kullanım için yeterli, ancak yoğun 3D oyunlarda çerçeve düşüşleri oluyor.', TRUE, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(46, 2, 5, 'Not alma deneyimi mükemmel', 'Lenovo Precision Pen 3 ile birlikte not alma ve belge işareti için kullandığımda çok memnun oldum. Hassasiyet ve gecikme süresi rakip tabletlere kıyasla oldukça iyi. Hoparlör sistemi de film izlemek için yeterince güçlü ve net. Fiyat segmenti göz önüne alındığında kalite çok başarılı.', TRUE, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(46, 3, 4, 'AMOLED ekranı güzel ama yazılım güncellemeleri yavaş', 'Donanım beklentilerimi karşılıyor; özellikle ekran kalitesi bu fiyat segmentinde rakipsiz. Ancak Lenovo''nun yazılım güncelleme hızı Samsung ve Apple''dan belirgin biçimde yavaş. Uzun vadeli kullanım planlayanlar bunu göz önünde bulundurmalı. Kısa vadede ise son derece tatmin edici bir deneyim.', TRUE, DATE_SUB(NOW(), INTERVAL 24 DAY));

-- ─────────────────────────────────────────────────────────────
-- KULALIKLAR
-- ─────────────────────────────────────────────────────────────

-- 47: Sony WH-1000XM5 Kablosuz Gürültü Önleyici Kulaklık
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(47, 1, 5, 'Gürültü engelleme teknolojisinin zirvesi', 'WH-1000XM5''i kalabalık ofiste ve uçak yolculuklarında kullanıyorum. ANC sistemi çevre sesini neredeyse tamamen kesiyor; motor gürültüsü ve ofis konuşmaları adeta yok oluyor. Ses kalitesi de referans seviyesinde. 30 saatlik pil ömrüyle uzun yolculuklarda adaptör taşımak zorunda kalmıyorum.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(47, 2, 5, 'Multipoint bağlantı çok pratik', 'İki cihaza aynı anda bağlanabilme özelliği iş hayatımda büyük kolaylık sağlıyor. Bilgisayardan müzik dinlerken telefon araması geldiğinde otomatik geçiş yapıyor. Mikrofon kalitesi de sesli görüşmelerde son derece net. Başlık tasarımı uzun saatler boyunca rahat taşıma sağlıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(47, 3, 4, 'Mükemmel kulaklık, katlanabilirlik eksik', 'Ses ve gürültü engelleme açısından piyasanın en iyilerinden biri. Ancak XM4''ün aksine XM5 yatay katlanamıyor, bu da seyahat sırasında taşımayı biraz güçleştiriyor. Kılıf boyutu da bir miktar büyük. Bunları göz ardı edebilirseniz sesli deneyim olağanüstü.', TRUE, DATE_SUB(NOW(), INTERVAL 19 DAY));

-- 48: Apple AirPods Pro 2. Nesil USB-C
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(48, 1, 5, 'Apple ekosistemi için vazgeçilmez', 'iPhone ve MacBook kullanıcısı olarak AirPods Pro 2''nin cihazlar arası geçişi sihir gibi çalışıyor. Bir cihazdan diğerine anında bağlantı ve iCloud senkronizasyonu günlük hayatı çok kolaylaştırıyor. ANC kalitesi de bu boyutta bir kulaklık için inanılmaz başarılı. Uzamsal ses de film izlerken harika bir deneyim sunuyor.', TRUE, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(48, 2, 5, 'Şeffaf mod gerçekten doğal', 'Adaptive Transparency özelliği çevre sesini doğal şekilde iletirken zararlı ani gürültüleri filtreliyor. Bu özelliği şehir içinde bisiklet sürerken çok kullandım: trafik seslerini duyuyorsunuz ama keskin kornalar rahatsız etmiyor. Ses kalitesi ve ANC de beklentilerin çok üzerinde.', TRUE, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(48, 3, 4, 'Harika ama Android''de sınırlı', 'Android telefon kullanan bir arkadaşım için kıyasladığımda AirPods Pro''nun pek çok özelliğinin yalnızca Apple cihazlarda çalıştığı görülüyor. Apple ekosistemi kullanıcıları için mükemmel; diğerleri için benzer fiyatta alternatifler daha iyi seçim olabilir. Ses ve ANC kalitesi ise tartışmasız.', TRUE, DATE_SUB(NOW(), INTERVAL 21 DAY));

-- 49: Bose QuietComfort Ultra Headphones Siyah
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(49, 1, 5, 'En rahat kulaklık başlığı', 'Bose''un kulaklık konforu her zaman rakiplerinin önünde olmuş, QC Ultra bunu zirveye taşıyor. Sekiz saatlik uçuşlarda kullandığımda hiçbir baskı ya da yorgunluk hissetmedim. Ses imajlama özelliği müziği dinlerken sahne derinliği hissi yaratıyor. Gürültü engelleme de Sony''ye yakın, bazı frekanslarda daha başarılı.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(49, 2, 5, 'Ses imajlama özelliği devrimsel', 'CustomTune ses imajlama teknolojisi her dinleyicinin kulak anatomisine göre sesi kalibre ediyor. Müzik dinlerken ses kaynaklarının oda içinde konumlandığını hissediyorsunuz. Bu deneyim standart stereodan çok daha sürükleyici. Mikrofon kalitesi de sesli görüşmelerde oldukça net.', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(49, 3, 4, 'Pil ömrü rakiplerinin gerisinde', 'Ses kalitesi ve konfor açısından piyasadaki en iyi kulaklıklardan biri. Ancak 24 saatlik pil ömrü Sony WH-1000XM5''in 30 saatinin gerisinde kalıyor. Ses imajlama özelliği etkinleştirildiğinde bu süre daha da düşüyor. Uzun yolculuk yapanlar bu farkı göz önünde bulundurmalı.', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- ─────────────────────────────────────────────────────────────
-- TİŞÖRTLER
-- ─────────────────────────────────────────────────────────────

-- 50: Nike Dri-FIT ADV TechKnit Ultra Erkek Tişört
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(50, 1, 5, 'Yoğun antrenman için mükemmel', 'Nike''ın üst segmenti TechKnit kumaşı gerçekten fark yaratıyor. Yüksek yoğunluklu interval antrenmanlarında teri anında dışarı atıyor ve vücut ısısını dengede tutuyor. Vücuda oturan kesimi hareket özgürlüğünü kısıtlamıyor. Birçok yıkamadan sonra da şeklini ve performansını koruyor.', TRUE, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(50, 2, 5, 'Koşu antrenmanlarda vazgeçilmez', 'Uzun mesafe koşularında bu tişörtü test ettim. Nem yönetimi üstün; dokuz kilometre koştuktan sonra bile kumaş vücuda yapışmıyor. Hafifliği de dikkat çekici, giydiğinizi neredeyse hissetmiyorsunuz. Nike Dri-FIT ADV serisinin en başarılı ürünlerinden biri.', TRUE, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(50, 3, 4, 'Performans harika, fiyat biraz yüksek', 'Performans kumaşı olarak işlevini mükemmel yerine getiriyor. Ancak standart Dri-FIT tişörte kıyasla önemli fiyat farkı var; bunu haklı çıkaracak özellik farkı hissediliyor ama bütçeye göre değerlendirin. Antrenman yoğunluğu yüksek olanlar için kesinlikle değer.', TRUE, DATE_SUB(NOW(), INTERVAL 16 DAY));

-- 51: Adidas Originals Trefoil Logo Erkek Tişört Beyaz
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(51, 1, 5, 'Klasik Adidas şıklığı', 'Trefoil logosu ve temiz beyaz zemin zamansız bir kombinasyon. Kumaş kalitesi Adidas Originals''ın kalite standardına uygun: ağır değil ama ucuz da hissettirmiyor. Spor şortla da jean pantolonla da çok iyi gidiyor. Birkaç sezon kullanmayı planlamak için doğru bir seçim.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(51, 2, 4, 'Kaliteli ama beden büyük', 'Kumaş ve işçilik kalitesi beklentimi karşıladı. Ancak beden tablosuna göre medium aldım ve normal bedenimi biraz aştı. Daha küçük beden almak ya da beden tablosunu dikkatlice incelemek gerekiyor. Renk ve yıkama sonrası koyuluk kaybı yok; bu konuda memnunum.', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(51, 3, 5, 'Her gardıroba yakışır temel parça', 'Beyaz Adidas Trefoil tişört hem spor hem de gündelik kombinlerde kolayca kullanılabilen esnek bir parça. Kumaşın nefes alabilirliği yaz aylarında konfor sağlıyor. Logonun baskı kalitesi dayanıklı; birçok yıkamadan sonra solmamış. Kesinlikle tekrar alacağım.', TRUE, DATE_SUB(NOW(), INTERVAL 18 DAY));

-- 52: Levi's Vintage Graphic Tee 501 Logolu Tişört
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(52, 1, 5, 'Otantik vintage his', 'Levi''s''in vintage yıkamalı kumaşı tişörtü gerçekten yıllanmış ve değerli hissettiriyor. Baskı kalitesi soluk ama kasıtlı bir soluk, tutarlı ve şık görünüyor. Jean ile kombinlendiğinde ikonik Amerikan tarzını yakalıyor. Pamuk içeriği de cilde yumuşak ve nefes alabilir.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(52, 2, 4, 'Rahat ve şık, baskı solabiliyor', 'Tişört kumaş kalitesi ve kesim açısından memnunum. Ancak birkaç yıkama sonrasında logonun biraz daha solduyduğunu fark ettim; bu vintage estetiğini tamamlayan bir özellik mi yoksa sorun mu tartışılabilir. Genel konfor düzeyi ve görünümü çok başarılı.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(52, 3, 5, 'Gündelik kullanım için ideal', 'Hafta sonu kaçamaklarında ve gündelik kullanımda vazgeçemediğim bir parça haline geldi. Kesim vücudu olduğundan geniş göstermeden rahat hissettiriyor. 501 baskısı Levi''s hayranları için ayrı bir anlam taşıyor. Fiyat/kalite dengesi çok iyi.', TRUE, DATE_SUB(NOW(), INTERVAL 17 DAY));

-- 53: Tommy Hilfiger Slim Fit Flag Logo Polo Tişört
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(53, 1, 5, 'Klasik polo şıklığı eksiksiz', 'Tommy Hilfiger''ın polo tişörtleri kalite açısından her zaman güvenilir olmuştur; bu model de hayal kırıklığı yaratmadı. Slim fit kesim bedene tam oturtu, ne dar ne gevşek. Pique kumaş sıcak havalarda nefes alabilir ama serinleme konusundaki performansı da tatmin edici. Kırmızı-beyaz-mavi bayrak detayı şıklık katıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(53, 2, 5, 'İş-dışı smart casual için ideal', 'Casual iş ortamında ve hafta sonu yemeklerinde bu poloyu sıkça giyiyorum. Görünümü hafif resmi bir hava katıyor, ancak tam olarak "resmi" değil. Kumaş ütü gerektirmiyor, pratik. Birkaç yıkamadan sonra da canlılığını koruyor. Tommy Hilfiger kalitesini hak ediyor.', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(53, 3, 4, 'Güzel ama fiyat biraz yüksek', 'Kalite açısından bir şikâyetim yok: kumaş, dikişler ve baskı hepsi itinalı. Ancak bu fiyat noktasında daha yaratıcı tasarım seçenekleri beklenebilir. Klasik ve zamansız bir polo isteyen için doğru seçim; ama fiyat hassasiyeti olanlar için başka markalar da iyi alternatifte sunuyor.', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- 54: Under Armour HeatGear Compression Tişört Erkek
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(54, 1, 5, 'Ağırlık antrenmanı için birebir', 'HeatGear Compression tişört spor salonunda performansımı gerçekten destekliyor. Vücuda tam oturan kesim egzersiz sırasında hareket kısıtlaması yaratmıyor. Terletici ortamda nem yönetimi mükemmel; kumaş ıslak kalmak yerine nemi uzaklaştırıyor. Kas yorgunluğunu azalttığını da hissediyorum.', TRUE, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(54, 2, 5, 'Koşu ve HIIT antrenmanlar için vazgeçilmez', 'Yüksek yoğunluklu kardiyoda bu tişörtü test ettim. Soğutucu teknolojisi etkisini gösteriyor; diğer spor tişörtlerine kıyasla daha serin kalıyorum. Elastik kumaş tam hareket özgürlüğü sağlıyor. Yıkama sonrası şeklini ve sıkıştırma özelliğini koruması da dayanıklılık açısından güven veriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(54, 3, 4, 'İyi ama çok sıkı geliyor', 'Compression tişörtlerin sıkı olması doğal ama bu model standartların biraz üzerinde sıkı. Bir beden büyük almanızı öneririm. Performans özellikleri harika; sadece beden seçimine dikkat edilmesi gerekiyor. Uzun süre giyildikten sonra elastikiyetini koruması da olumlu.', TRUE, DATE_SUB(NOW(), INTERVAL 15 DAY));

-- ─────────────────────────────────────────────────────────────
-- SWEATSHIRT & HOODIE
-- ─────────────────────────────────────────────────────────────

-- 55: Nike Tech Fleece Erkek Sweatshirt Gri
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(55, 1, 5, 'Hafiflik ve sıcaklık dengesi mükemmel', 'Nike Tech Fleece''in en beğenilen özelliği standart sweatshirtlere kıyasla çok daha hafif olması ama eş sıcaklık sağlaması. Sonbahar ve ilkbahar geçiş döneminde birebir. İnce katman olarak ceketin altında da şık görünüyor. Gri renk her kombinle uyum sağlıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(55, 2, 5, 'Spordan şehre geçişi kolay', 'Sabah koşusundan kahvaltıya geçerken değiştirmeye üşendiğim tek üstüm bu oluyor artık. Tech Fleece''in temiz görünümü spor salonundan kafeye geçişte sormadan kabul görüyor. Dikişler ve baskı kalitesi de uzun vadeli dayanıklılık vaadi veriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(55, 3, 4, 'Harika ürün, bakım gerektiriyor', 'Performans ve görünüm açısından beğendim. Ancak kumaşın tüylenmemesi için önce yıkama talimatlarına dikkat etmek gerekiyor; ilk birkaç yıkamada iç tarafı çevirerek yıkamak şart. Bu bakımı gösterirseniz uzun süre iyi durumda kalıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 22 DAY));

-- 56: Champion Reverse Weave Hoodie Lacivert
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(56, 1, 5, 'Klasik American heritage kıyafeti', 'Champion Reverse Weave serisi kumaş dokuma yönüyle yıkama sonrası çekme sorununu çözüyor; bu teknik detay hayat kalitesini artırıyor. Ağır kumaş hem kaliteli hissettiriyor hem de soğuk günlerde yeterli sıcaklık sağlıyor. Lacivert renk ve küçük Champion logosu şık ve abartısız.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(56, 2, 5, 'Uzun ömürlü yatırım', 'Bu hoodie''yi iki yıl önce aldım ve hâlâ yeni gibi duruyor. Kumaş kalınlığı ve dikişler uzun ömürlülüğünü gösteriyor. Renk birçok yıkamadan sonra canlılığını korudu. Champion''ın premium koleksiyonlarına yakın kalite sunduğu kanıtlanmış bir ürün.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(56, 3, 4, 'Kaliteli ama ağır', 'Kumaşın ağırlığı soğuk havada sıcaklık konusunda avantaj sağlıyor ama sıcak havalarda fazla gelir. Kapüşon detayı ve kurtarılabilir kordon iyi yapılmış. Özellikle kış ayları için harika bir hoodie; dört mevsim kullanmak isteyenler daha hafif seçenekler de değerlendirebilir.', TRUE, DATE_SUB(NOW(), INTERVAL 19 DAY));

-- 57: Adidas Essentials Fleece 3-Stripes Erkek Sweatshirt
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(57, 1, 4, 'Temel ihtiyaç için yeterli', 'Günlük kullanım için rahat ve uygun fiyatlı bir sweatshirt. Üç çizgi detayı markanın özünü yansıtıyor. Kumaş kalitesi premium Adidas ürünlerine yetişemiyor ama fiyat segmentini haklı çıkarıyor. Kış aylarında ince katman olarak iyi çalışıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(57, 2, 5, 'Spor salonundan markete her yere gidiyor', 'Basit ve temiz tasarımıyla her ortamda rahat hissedebileceğim bir parça. Kumaş rahat ve hafif. Renk seçenekleri geniş; farklı renklerde birkaç tane daha almayı düşünüyorum. Fiyat/kalite dengesi Adidas logolu bir ürün için makul.', TRUE, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(57, 3, 4, 'Beden tutarsızlığına dikkat', 'İkinci kez Adidas sipariş ettiğimde aynı bedeni aldım ama bu model biraz daha dar kesimli. Beden tablosunu kontrol etmenizi öneririm. Bunun dışında kumaş kalitesi ve dikişler iyi; uzun vadeli dayanıklılık açısından umut verici görünüyor.', TRUE, DATE_SUB(NOW(), INTERVAL 23 DAY));

-- 58: Stüssy Stock Logo Crew Sweatshirt Krem
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(58, 1, 5, 'Streetwear ikonası hak ettiği yerde', 'Stüssy''nin kumaş kalitesi ve ağırlığı premium segmentte olduğunu hissettiriyor. Krem rengi ve el yazısı logo kombinasyonu zamansız bir streetwear klasiği. Aşırı marka logosu gösterişinden uzak, ince bir estetik duruş var. Çok fazla yıkamadan sonra da şeklini koruyor.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(58, 2, 5, 'Kumaş kalitesi olağanüstü', 'Stüssy''nin Fransız terry kumaşı hem iç hem de dış tarafta premium his veriyor. Görünümü sakin ama dikkat çeken bir cazibesi var. Krem renk pek çok renk palet ile uyum sağlıyor. Fiyatı yüksek ama kalitesiyle haklı çıkarıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(58, 3, 4, 'Kaliteli ama fiyatı biraz sarsmıyor değil', 'Kumaş ve işçilik kalitesi tartışmasız; dikişler sağlam ve ürün ciddi özenle üretilmiş. Ancak benzer kalitede daha uygun fiyatlı alternatifler de var. Stüssy estetiğine ve marka tarihine değer biçenler için kesinlikle doğru seçim; yalnızca kalite arayan için daha ekonomik yollar da mevcut.', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- 59: The North Face Tekno Logo Hoodie Siyah
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(59, 1, 5, 'Outdoor şehir arasında mükemmel köprü', 'TNF''in Tekno Logo serisi outdoor performansını şehir estetiğiyle buluşturuyor. Kumaş kalitesi hem soğuk havalarda sıcak tutuyor hem de şehir içinde şık görünüyor. Siyah renk her kombinle uyum sağlıyor. TNF logosu markanın güvenilirliğini ve kalitesini simgeliyor.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(59, 2, 4, 'Harika hoodie, kapüşon biraz dar', 'Kalite ve görünüm açısından beklentilerimi karşıladı. Tek sorunum kapüşonun başa tam oturmak için biraz dar kalması; saçlı olanlar için bu önemli olabilir. Kumaşın ağırlığı ve sıcaklık performansı ise tatmin edici. TNF garantisi de güven veriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(59, 3, 5, 'Dağ kampüs geçişleri için ideal', 'Dağ yürüyüşü sonrasında kampüs kafesine giderken bu hoodie benim köprüm oldu. Outdoor performansı sağlamlık ve sıcaklık açısından yeterli, şehir görünümü de şık ve temiz. TNF''in bu koleksiyonu gerçekten iki dünyanın en iyisini sunuyor.', TRUE, DATE_SUB(NOW(), INTERVAL 17 DAY));

-- ─────────────────────────────────────────────────────────────
-- PANTOLONLAR
-- ─────────────────────────────────────────────────────────────

-- 60: Levi's 501 Original Fit Erkek Kot Pantolon Koyu Indigo
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(60, 1, 5, 'Denim''in sonsuz klasiği', '501''leri on yıllardır giyiyorum ve bu kalite standardını korumak için Levi''s''e gerçekten saygı duyuyorum. Koyu indigo renk yıkamalarla güzelce açılıyor ve yaşlanıyor; bu kişiselleşme özelliği vintage estetiğini sevenler için büyük değer. Dikiş kalitesi ve kumaş sağlamlığı uzun yıllar dayanacağını gösteriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(60, 2, 5, 'Her kombinle çalışan evrensel parça', '501''in straight fit kesimi hem spor hem de yarı resmi kombinlere uyum sağlıyor. Çalışma ortamım smart casual kıyafet politikasına izin verdiğinde bu pantolonu sıkça giyiyorum. Koyu indigo renk beyaz gömlek ve deri ayakkabıyla harika görünüyor. Fiyat/kalite oranı çok iyi.', TRUE, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(60, 3, 4, 'Klasik ama bel genişliğine dikkat', '501''in kalıbı klasik ve güvenilir ama bel ölçüsü çok kesin. Beden sınırında olanlar bir üst beden almayı veya deneyip karar vermeyi düşünmeli. Bunun dışında kumaş kalitesi, dikişler ve renk performansı beklentileri karşılıyor. Levi''s güvencesi devam ediyor.', TRUE, DATE_SUB(NOW(), INTERVAL 16 DAY));

-- 61: Carhartt WIP Sid Pant Chino Haki
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(61, 1, 5, 'İşçi mirasını modern şehir estetiğine taşıyor', 'Carhartt WIP''in çalışma pantolonu mirasını contemporary yorumuyla buluşturması bu modelde çok başarılı. Slim fit kesim bacağı uzun gösterirken hareketliliği kısıtlamıyor. Haki renk son derece çok yönlü; hem spor hem de smart casual kombinlere giriyor. Kumaş kalitesi de markayı haklı çıkarıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(61, 2, 5, 'Dayanıklılık ve şıklık birlikte', 'Bu pantalonu üretim tesisi ziyaretlerinde ve ofis toplantılarında giyiyorum; iki ortamda da rahat ve yerinde hissediyorum. Kumaşın yırtılmaya ve aşınmaya direnci etkileyici; uzun vadeli kullanım için doğru yatırım. Cep sayısı ve yerleşimi de pratiklik açısından iyi düşünülmüş.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(61, 3, 4, 'Kaliteli ama ütü gerektiriyor', 'Chino kumaşın doğası gereği ütüsüz giyildiğinde kırışık görünüyor. Düzgün görünmesi için biraz bakım gerekiyor. Kumaş kalitesi ve kesim ise mükemmel; uzun süre iyi durumda kalacak gibi görünüyor. Düzenli ütü yapmayı sorun etmeyenler için harika bir seçim.', TRUE, DATE_SUB(NOW(), INTERVAL 19 DAY));

-- 62: Nike Sportswear Tech Fleece Jogger Erkek Eşofman Altı
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(62, 1, 5, 'Antrenman sonrası konforun zirvesi', 'Tech Fleece Jogger''ı antrenmandan eve dönerken giyiyorum ve eve geldiğimde değiştirmek istemiyorum. Hafif ama sıcak tutan kumaş ev ortamında da antrenman sonrasında da mükemmel konfor sağlıyor. Konik paça tasarımı da hem spor hem de gündelik görünümde şık duruyor.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(62, 2, 5, 'Seyahat konforunun vazgeçilmezi', 'Uzun uçuşlarda ve tren yolculuklarında bu pantolonu tercih ediyorum. Tech Fleece kumaşı kırışmıyor ve uzun süre oturmak için yeterli esneklik sağlıyor. Şık görünümü sayesinde varış noktasında direkt aktivitelere katılabiliyorum.', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(62, 3, 4, 'Şık ama özenli bakım gerektiriyor', 'Tech Fleece serisi güzel ama özellikle tüylenme konusunda dikkatli bakım istiyor. İç tarafı ters çevirerek yıkamak şart. Bu bakımı yaparsanız uzun süre iyi durumda kalıyor. Görünüm ve konfor açısından beğendim; sadece bakımına dikkat edin.', TRUE, DATE_SUB(NOW(), INTERVAL 17 DAY));

-- 63: Dickies 874 Original Work Pant Siyah
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(63, 1, 5, 'Çalışma pantolonunun efsanesi', 'Dickies 874''ü işlikte ve günlük kullanımda birlikte test ettim. Kumaşın aşınmaya direnci gerçekten etkileyici; kesici aletler ve sert yüzeylerle çalışırken hiç yırtılmadı. Yıkama sonrası şeklini ve rengini koruyor. Uygun fiyatıyla uzun ömürlülüğü düşününce çok iyi bir yatırım.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(63, 2, 5, 'Streetwear sahnesinin gizli kahramanı', 'Dickies 874 nasıl olduysa streetwear sahnesinin vazgeçilmezi haline geldi. Siyah renk temiz ve keskin görünüyor. Geniş bel ve rahat kesim uzun süre giyimi konforlu kılıyor. Kargo cebi işlevsellik katıyor. Fiyatı için sunduğu kalite olağanüstü.', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(63, 3, 4, 'Sağlam ama biraz kastır', 'Kumaş ilk başta sert geliyor; birkaç yıkama sonrasında yumuşuyor ve çok rahat hale geliyor. Bu "alışma sürecini" bilen için sorun değil, bilmeyenler hayal kırıklığı yaşayabilir. Tam alışınca bu pantalonu bırakamıyorsunuz.', TRUE, DATE_SUB(NOW(), INTERVAL 21 DAY));

-- 64: Columbia Silver Ridge Convertible Pant Erkek Outdoor
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(64, 1, 5, 'Trekking''in en pratik pantolonu', 'Silver Ridge Convertible''ı bir haftalık Kaçkarlar trekkingi boyunca kullandım. Bacakların çıkarılabilmesi akşam kampında kısa pantolon olarak kullanmayı sağlıyor; ekstra kıyafet taşıma gereksinimi azalıyor. Omni-Wick teknolojisi ter yönetimini de iyi karşılıyor. Hafif ve dayanıklı kombinasyonu mükemmel.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(64, 2, 5, 'UPF 50+ güneş koruması fark yaratıyor', 'Yüksek rakımda uzun güneş maruziyetinde bu pantolonun UPF 50+ koruması ciddi bir güvence sağlıyor. Bacaklarınız güneş yanığından korunuyor, üstelik nefes alabilen kumaş sıcak havada da rahat. Fermuarlı ceplerin sayısı ve tasarımı da saha koşullarında pratik.', TRUE, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(64, 3, 4, 'Fonksiyonel ama şehirde şık değil', 'Outdoor performansı tartışmasız başarılı; ancak şehir kullanımına yönelik görsel estetiği zayıf kalıyor. Trekking ve kamp için mükemmel, günlük şehir kullanımı için başka seçenekler daha şık görünecektir. İki dünyanın ortasında kullanmak isteyenler alternatif modellere de bakabilir.', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- ─────────────────────────────────────────────────────────────
-- SPOR AYAKKABILAR
-- ─────────────────────────────────────────────────────────────

-- 65: Nike Air Max 270 Erkek Spor Ayakkabı Siyah/Beyaz
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(65, 1, 5, 'Günlük kullanımın en rahat tercihi', 'Air Max 270''in 270 derece Air birimi ayak tabanını adeta yastık üzerinde yürütüyor. Uzun şehir yürüyüşlerinde bile akşama kadar ayak ağrısı yaşamıyorum. Siyah/beyaz renk kombinasyonu hem spor hem de gündelik kombinlere uyum sağlıyor. Nike''ın en başarılı yaşam tarzı modellerinden biri.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(65, 2, 5, 'Ayak yorgunluğunu ortadan kaldırıyor', 'Perakende mağazasında uzun saatler ayakta çalışıyorum. Air Max 270''e geçtiğimden bu yana ayak ve bel ağrılarım belirgin azaldı. Air birimi gerçekten fark yaratıyor. Dış taban aşınma direnci de yoğun kullanımda iyi performans sergiliyor.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(65, 3, 4, 'Rahat ama hafiften büyük kalıyor', 'Konfor açısından Nike''ın en başarılı modellerinden biri. Ancak kalıp normalden biraz büyük; yarım numara küçük almanızı öneririm. Bu noktaya dikkat ederseniz mükemmel bir günlük ayakkabı. Renk ve tasarım da uzun süre moda dışı kalmayacak.', TRUE, DATE_SUB(NOW(), INTERVAL 18 DAY));

-- 66: Adidas Ultraboost 22 Erkek Koşu Ayakkabısı Beyaz
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(66, 1, 5, 'Koşu ayakkabısında paradigma değişimi', 'Ultraboost 22''yi yarı maraton antrenman sürecimde kullandım. BOOST köpük teknolojisi her adımda enerji geri dönüşü sağlıyor; mesafe arttıkça bu fark daha belirgin hale geliyor. Primeknit üst materyal ayağa ikinci bir deri gibi sarılıyor. Uzun mesafeler için kesinlikle tavsiye ederim.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(66, 2, 5, 'Streetwear estetiği ve koşu performansı', 'Ultraboost''un yalnızca iyi bir koşu ayakkabısı değil, aynı zamanda şık bir günlük ayakkabı olduğunu çevrem onaylıyor. Beyaz renk temiz ve premium görünüyor. Tabii ki koşu performansı da harika; BOOST teknolojisi yorgunluğu geciktiriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(66, 3, 4, 'Mükemmel ama beyaz renk bakım istiyor', 'Koşu performansı tartışmasız; ama beyaz renk kısa sürede kirleniyor ve temizlik gerektiriyor. Düzenli bakım yapmaya hazırsanız uzun süre yeni görünümlü kalıyor. Performans ayakkabısı arayanlar için sektörün en iyilerinden biri.', TRUE, DATE_SUB(NOW(), INTERVAL 21 DAY));

-- 67: New Balance 990v6 Made in USA Gri
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(67, 1, 5, 'Made in USA kalitesi hissediliyor', '990v6''yı eline aldığınızda ABD yapımı olduğu hemen belli oluyor: malzeme kalitesi, dikişler ve genel işçilik Asya üretimi ayakkabılardan farklı bir seviyede. Deri ve mesh kombinasyonu hem dayanıklı hem nefes alabilir. Koşu ve günlük kullanım için dengeli bir performans sunuyor.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(67, 2, 5, 'Zamansız sneaker tasarımı', '990 serisi 40 yılı aşkın süredir üretiliyor ve bu süreklilik kalite güvencesinin en iyi kanıtı. v6 sürümü teknolojiyi güncellerken ikonik estetiği korumuş. Gri renk ve suede deri kombinasyonu her döneme uyum sağlıyor. Koleksiyon bilinçlileri için değerli bir parça.', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(67, 3, 4, 'Kaliteli ama fiyat yüksek', 'Made in USA ek maliyeti yadsınamaz: bu fiyata Asya üretimi çok daha uygun fiyatlı alternatifler mevcut. Ancak kalite farkını fiziksel olarak hissediyorsunuz; uzun vadeli kullanımda bu yatırımın karşılığını aldığınızı düşünüyorum. Kalite önceliklendirenler için harika seçim.', TRUE, DATE_SUB(NOW(), INTERVAL 17 DAY));

-- 68: ASICS Gel-Kayano 30 Koşu Ayakkabısı Erkek
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(68, 1, 5, 'Uzun mesafe koşucularının güvenilir ortağı', 'Gel-Kayano serisini beş yıldır kullanıyorum ve 30. versiyon serinin en gelişmiş modeli. FF BLAST+ köpük ve ek GEL teknolojisinin kombinasyonu 30+ km koşularda bile topuk ve diz stresini minimize ediyor. Overpronation sorunu olan koşucular için özellikle tavsiye ederim.', TRUE, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(68, 2, 5, 'Dizliğim olmadan koşabiliyorum', 'Diz problemi nedeniyle koşuyu bırakmak üzereydim. Ortopedistim Kayano''yu önerdi; üç aydır kullanıyorum ve sorun belirgin azaldı. Destek sistemi gerçekten işe yarıyor. Uzun vadeli yatırım düşünenler için sağlık açısından da değer.', TRUE, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(68, 3, 4, 'Destekleyici ama biraz ağır', 'Gel-Kayano''nun destek ve amortizasyon sistemi harika. Ancak rekabetçi yarışmalarda hız odaklıysanız bu ağırlık hissediliyor. Uzun mesafe antrenman ve yarı maratonlar için ideal; hız odaklı 5K koşular için daha hafif modeller tercih edilebilir.', TRUE, DATE_SUB(NOW(), INTERVAL 22 DAY));

-- 69: Salomon Speedcross 6 GTX Erkek Trail Koşu
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(69, 1, 5, 'Çamurlu patikaların hakimi', 'Speedcross 6 GTX''i Anadolu''nun sarp patikalarında test ettim. Agresif dış taban tutunması kayalık ve çamurlu zeminlerde güven veriyor; hiç kaymadım. Gore-Tex membran yağmurlu hava ve ıslak çim geçişlerinde ayağı kuru tutuyor. Trail koşucuları için vazgeçilmez bir model.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(69, 2, 5, 'GTX su geçirmezliği gerçekten çalışıyor', 'Sabah çiğ ve ıslak çim üzerinde iki saatlik trail koşusundan sonra çorabım kuru çıktı. Gore-Tex teknolojisi vaadini yerine getiriyor. Quicklace sistemi de eldivensiz hızlı bağlama sağlıyor. Sert zemin koşucuları için Salomon''un en iyi modellerinden.', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(69, 3, 4, 'Trail için harika, asfalt için sert', 'Patikaları bu ayakkabı için biçilmiş kaftan. Ancak asfalt veya düz zeminlerde Speedcross''un agresif tabanı konforu azaltıyor. Saf trail koşucuları için beş yıldız; karma zemin kullanacaklar başka modeller de değerlendirsin.', TRUE, DATE_SUB(NOW(), INTERVAL 19 DAY));

-- ─────────────────────────────────────────────────────────────
-- GÜNLÜK SNEAKERLAR
-- ─────────────────────────────────────────────────────────────

-- 70: Adidas Stan Smith Vegan Beyaz/Yeşil
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(70, 1, 5, 'Sürdürülebilir moda başarısı', 'Vegan Stan Smith hem çevresel sorumluluğu hem de klasik estetiği bir arada sunuyor. Microfiber üst malzeme gerçek deri kadar şık görünüyor ve bakımı daha kolay. Klasik Stan Smith formunu sevenler için yıldörümü geçmeyen bir tercih. Orijinal Stan Smith ile karşılaştırılınca neredeyse aynı kalite hissi veriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(70, 2, 5, 'Her kombinle çalışan ikonik sneaker', 'Stan Smith modeli on yıllardır başarısını koruyor ve vegan versiyonu bu mirası sürdürüyor. Jean, chino, hatta hafif elbise kombinlerinde bile şık duruyor. Beyaz/yeşil renk kombinasyonu temiz ve sade. Günlük kullanım için harika.', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(70, 3, 4, 'Güzel ama vegan deri solunabilir değil', 'Görünüm ve dayanıklılık açısından çok iyi. Ancak vegan microfiber deri gerçek deri gibi nefes alamıyor; uzun yürüyüşlerde bu fark hissedilebiliyor. Kısa süreli kullanım için mükemmel, uzun yürüyüşler için çorap seçimine dikkat etmek gerekiyor.', TRUE, DATE_SUB(NOW(), INTERVAL 16 DAY));

-- 71: Converse Chuck Taylor All Star High Top Siyah
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(71, 1, 5, 'Asla modası geçmeyen bir klasik', 'Chuck Taylor''ı ilk kez 15 yaşında giymiştim, şimdi de aynı heyecanla giyiyorum. Canvas kumaşı ve vulkanize dış taban yıllar içinde değişmemiş; bu tutarlılık takdire değer. Siyah high top modeli özellikle rock ve alternatif modaya harika uyum sağlıyor. Efsane devam ediyor.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(71, 2, 5, 'Sanat ve müzik dünyasının simgesi', 'Converse Chuck Taylor sanat galerilerinde, konserlerde ve müzik stüdyolarında standart giysinin parçası. Bunun nedenini giydiğinizde anlıyorsunuz: özgün, savruk ama şık bir ifade taşıyor. Yıpranmış görünümü bile estetik bir değer kazanıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(71, 3, 3, 'İkonik ama amortizasyon yok', 'Tasarım ve stil açısından tartışılmaz bir klasik. Ancak günde sekiz saatten fazla yürüyenlere uyarı: Chuck Taylor''da ciddi amortizasyon yok ve uzun günlerde topuk ağrısı başlıyor. Kısa süreli kullanım veya ara ara giyim için harika; birincil günlük ayakkabı olarak tavsiye etmem.', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- 72: New Balance 550 Beyaz/Krem Retro Sneaker
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(72, 1, 5, 'Retro basketbol estetiğinin modern yorumu', 'NB 550 son dönemde patlayan popülerliğini tam hak eden bir model. Beyaz/krem renk kombinasyonu ve geniş dil tasarımı 90''ların basketbol ayakkabı nostaljisini yaşatıyor. Deri üst malzeme premium his veriyor. Hem erkek hem kadın kombinlerine uyum sağlayan cinsiyet nötr bir estetik.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(72, 2, 5, 'Günlük kullanımın yeni favorisi', 'Birçok sneaker''ı denedikten sonra 550''ye yerleştim. Görsel dengesi ve yüksekliği günlük kombinlerde mükemmel tamamlayıcı. Taban konforu yeterli, üst malzeme bakımı kolay. Fiyat/kalite dengesi de New Balance''ın standart üstü kalitesini yansıtıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(72, 3, 4, 'Popülerlik stok sorununa yol açıyor', 'Ayakkabı mükemmel ama son dönemde popülerleşmesiyle birlikte stok bulmak güçleşti ve fiyatlar arttı. Reseller piyasasına düşmeden orijinal fiyatına bulabilirseniz kesinlikle alın. Kalite ve görünüm harika; sadece erişilebilirlik sorunu var.', TRUE, DATE_SUB(NOW(), INTERVAL 18 DAY));

-- 73: Vans Old Skool Checkerboard Siyah/Beyaz
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(73, 1, 5, 'Skate kültürünün ikonik deseni', 'Checkerboard desen Vans''ın adeta ikinci ismi haline gelmiş; Old Skool silueti ile buluşması ikonik bir kombinasyon. Siyah/beyaz dama desen sakin görünümlü kombinlere güçlü bir karakter katıyor. Vulkanize taban kaykay için tasarlanmış ama günlük kullanımda da sağlam duruyor.', TRUE, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(73, 2, 5, 'Her tarzla uyum sağlayan crossover', 'Punk''tan hiphop''a, streetwear''dan vintage modaya kadar pek çok stille uyum sağlayan nadir modellerden biri. Checkerboard deseni iddia sahibi bir görünüm sunuyor ama abartıya kaçmıyor. Canvas kumaşı nefes alabilir ve yaz aylarında konforlu.', TRUE, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(73, 3, 4, 'Şık ama taban desteği az', 'Görsel açıdan harika ve çok yönlü bir sneaker. Ancak taban desteği ve amortizasyon minimal; uzun yürüyüşlerde bunu hissediyorsunuz. Birkaç saatlik şehir gezisi için ideal, tam gün yürüyüş için ek tabanlık düşünülebilir.', TRUE, DATE_SUB(NOW(), INTERVAL 15 DAY));

-- 74: Puma Suede Classic XXI Erkek Sneaker Lacivert
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(74, 1, 5, '50 yılı aşkın başarının sırrı', 'Puma Suede Classic 1968''den bu yana üretiliyor ve bu uzun ömrü haklı çıkaran bir kalite var. Lacivert suede üst mükemmel renk canlılığını koruyor. Düz profil ve temiz çizgiler zamansız bir elegans sunuyor. Koleksiyon amaçlı da günlük kullanım amaçlı da doğru bir seçim.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(74, 2, 4, 'Güzel ama suede bakım gerektiriyor', 'Ayakkabının görünümü ve kalitesi harika. Ancak suede malzeme yağmur ve leke konusunda hassas; suede koruyucu spreyi düzenli kullanmak şart. Bu bakımı yaparsanız uzun yıllar yeni gibi kalıyor. Kolayca bakımsız bırakmak isteyenler deri versiyonlarına bakabilir.', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(74, 3, 5, 'Retro şıklık günümüzde de geçerli', 'Puma Suede''in klasik silueti bugün de moda, geçmişte de modaydı, gelecekte de moda olacak. Lacivert renk hem spor hem de casual kombinlere uyuyor. İnce profili günlük sürüş ayakkabısı olarak kullandığımda da son derece rahat. Fiyat/kalite dengesi çok başarılı.', TRUE, DATE_SUB(NOW(), INTERVAL 19 DAY));

-- ─────────────────────────────────────────────────────────────
-- EV VE YAŞAM
-- ─────────────────────────────────────────────────────────────

-- 75: Philips Hue Starter Kit E27 Akıllı LED Ampul 3lü Set
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(75, 1, 5, 'Akıllı ev aydınlatmasının standardı', 'Philips Hue sistemine üç yıl önce geçtim ve eve döndüğümde ışıkların beklediğimi görmek hâlâ keyifli. Kurulum son derece basit; Bridge ve ampulleri bağladıktan sonra uygulama her şeyi otomatik hallediyor. 16 milyon renk seçeneği ve ışık sahneleri ortamı tamamen değiştiriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(75, 2, 5, 'Google Home ve Alexa entegrasyonu kusursuz', 'Sesle ışıkları kontrol edebilmek alışkanlık haline gelince geri dönüş zor oluyor. Google Home ile "Tamam Google, salon ışıklarını yüzde kırka indir" demek yeterli. Enerji tüketimi geleneksel ampullere kıyasla da belirgin şekilde düşük.', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(75, 3, 4, 'Harika sistem, Bridge zorunluluğu ekstra maliyet', 'Başlangıç kiti üç ampul içeriyor ama Bluetooth yerine tüm özelliklere erişmek için Bridge cihazı da gerekiyor; bu ek bir maliyet. Tüm sistemi kurduğunuzda sonuç muhteşem ama toplam yatırımı hesaplayarak karar verin. Uzun vadede değer sağladığından eminim.', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- 76: Dyson V15 Detect Absolute Kablosuz Süpürge
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(76, 1, 5, 'Ev temizliğinde devrim', 'Dyson V15''in lazer tozalgılama teknolojisi göze görünmeyen ince tozları aydınlatıyor ve süpürmenin ne kadar etkin olduğunu kanıtlıyor. Piezo sensörü ot sayarak gerçek zamanlı istatistik gösteriyor. Kablo olmadan tüm evi rahatça süpürebilmek lüks değil zorunluluk haline geldi.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(76, 2, 5, 'Ev hayvanı sahipleri için şart', 'İki kedim var ve saçlanma sorunum ciddi boyutlardaydı. V15 Detect''in hayvan tüyü özelleşmiş başlığı ve güçlü emişi bu sorunu tamamen çözdü. Filtrasyon sistemi HEPA sertifikalı; alerjisi olan aile bireyim de memnun. Yatırıma değer.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(76, 3, 4, 'Mükemmel performans, yüksek fiyat', 'Performans açısından eleştirecek hiçbir şey yok: emme gücü, lazer algılama ve filtrasyon harika. Ancak fiyatı standart bir kablosuz süpürgenin iki katı. Bu fiyatı haklı çıkaracak özellikleri kullananlar için kesinlikle değer; bütçe sınırı olanlar için daha uygun Dyson modelleri de var.', TRUE, DATE_SUB(NOW(), INTERVAL 19 DAY));

-- 77: Le Creuset Signature Döküm Demir Tencere 26cm Kırmızı
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(77, 1, 5, 'Mutfak yatırımının altın standardı', 'Le Creuset tenceremi beş yıldır kullanıyorum ve her şeyin aynı mükemmellikte olduğunu söylemek borcum. Isı dağılımı ve sabit tutma kapasitesi diğer tencerelerimi geride bırakıyor. Güveç ve düşük ateşte pişen yemeklerde fark gözle görülür. Nesilden nesile geçecek bir mutfak parçası.', TRUE, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(77, 2, 5, 'Rengi on yıl sonra da canlı', 'Le Creuset''nin emaye kaplama hem estetik hem de işlevsel açıdan mükemmel. On yıllık anneme ait bir tencereyi gördüm; rengi hâlâ kıpkırmızı ve çatlak yok. Bu dayanıklılık yüksek fiyatı haklı çıkarıyor. Ömürlük bir yatırım.', TRUE, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(77, 3, 4, 'Mükemmel pişirme, ağır taşıma', 'Pişirme performansı için beş yıldız hak ediyor. Ancak 26cm model dolu olduğunda oldukça ağır; fırından çıkarırken dikkatli olmak gerekiyor, özellikle yaşlı veya bilek sorunu olan kullanıcılar için zorlayıcı olabilir. Kalite ve pişirme sonuçları ise tartışmasız mükemmel.', TRUE, DATE_SUB(NOW(), INTERVAL 22 DAY));

-- 78: Nespresso Vertuo Next Premium Kahve Makinesi
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(78, 1, 5, 'Ev kahveciliğinde yeni standart', 'Vertuo Next sistemine geçmeden önce kahve çok basit bir içecekti benim için. Şimdi espresso, americano ve mug boyutu arasında seçim yapıyor ve her birinde tutarlı, kafede içtiğim kalitede kahve hazırlayabiliyorum. Centrifugal teknoloji kremanın kalitesini gerçekten artırıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(78, 2, 5, 'Sabah rutinini iyileştirdi', 'Kahve kapsülünü yerleştirip butona basmak ve 30 saniye sonra mükemmel espresso hazır. Bu kolaylık sabah rutinini tamamen değiştirdi; artık kafede sıra beklemeden evde aynı kalitede kahve içiyorum. Tasarımı da son derece şık, mutfak tezgâhı güzelleşti.', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(78, 3, 4, 'Makine harika, kapsül maliyeti yüksek', 'Makinenin performansı ve kolaylığı mükemmel. Ancak Nespresso kapsüllerinin kümülatif maliyeti uzun vadede kahve bütçesini artırıyor. Aylık kahve tüketiminizi hesaplayarak karara varın. Kolaylık ve kalite için bütçe ayırabiliyorsanız harika bir yatırım.', TRUE, DATE_SUB(NOW(), INTERVAL 17 DAY));

-- 79: IKEA KALLAX Kitaplık/Raf Ünitesi 4x4 Beyaz
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(79, 1, 5, 'Depolama çözümünün klasiği', 'KALLAX serisi IKEA''nın en başarılı depolama sistemlerinden biri; 4x4 model çalışma odama mükemmel uyum sağladı. Kitap, dekorasyon ve kutu aksesuar kombinasyonuyla hem fonksiyonel hem estetik bir duvar çözümü sunuyor. Montajı iki kişiyle rahatlıkla yapılıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(79, 2, 5, 'Kişiselleştirme seçenekleri sonsuz', 'KALLAX''ın asıl değeri IKEA\'ın sunduğu kapı, çekmece ve sepet aksesuarlarıyla sonsuz kişiselleştirilebilir olması. Bazı gözleri açık raf, bazılarını kapalı yapıp karma bir depolama sistemi oluşturdum. Beyaz renk küçük odayı daha geniş gösteriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(79, 3, 4, 'İyi ürün ama kargo hasarına dikkat', 'Ürün planladığım gibi çalışıyor; ancak büyük kutu kargo sürecinde hasar görebiliyor. Teslim alırken dikkatlice kontrol etmenizi öneririm. Ayrıca levhaların ağırlığı nedeniyle monte sırasında ek bir yardım eli şart. Bunları aşarsanız çok pratik ve şık bir depolama çözümü.', TRUE, DATE_SUB(NOW(), INTERVAL 23 DAY));

-- ─────────────────────────────────────────────────────────────
-- OUTDOOR & SPOR EKİPMANI
-- ─────────────────────────────────────────────────────────────

-- 80: Garmin Forerunner 965 Koşu GPS Akıllı Saat
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(80, 1, 5, 'Koşucu için nihai veri merkezi', 'Forerunner 965''i maraton antrenman sürecimde kullandım. VO2 Max tahmini, Training Load ve Race Predictor özellikleri antrenmanlarımı bilimsel bir zemine oturttu. AMOLED ekran güneş ışığında bile netliğini koruyor. Koşu dinamikleri analizi koşu formumu geliştirmeme yardımcı oldu.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(80, 2, 5, 'Pil ömrü rekabetsiz', 'GPS modunda 31 saatlik pil ömrü ultramaraton koşucuları için hayat kurtarıcı. Çok sporlu triatlon modunda da pil rakiplerinin çok önünde. Garmin Connect uygulaması ve Strava entegrasyonu koşu verilerini analiz etmeyi kolaylaştırıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(80, 3, 4, 'Harika saat, karmaşık arayüz', 'Fonksiyonel zenginliği açısından piyasanın en iyilerinden biri. Ancak bu zenginlik beraberinde dik bir öğrenme eğrisi getiriyor; tüm özelliklere hâkim olmak birkaç hafta alıyor. Sabırlı ve meraklı kullanıcılar için mükemmel; sadece adım ve mesafe isteyenler aşırı bulabilir.', TRUE, DATE_SUB(NOW(), INTERVAL 19 DAY));

-- 81: Decathlon Quechua MH500 Trekking Sırt Çantası 40L
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(81, 1, 5, 'Hafta sonu trekkingi için mükemmel', 'MH500''ü Kaz Dağları''nda iki gecelik bir trekkingde kullandım. 40L hacim, uyku tulumu, mont, yiyecek ve kamp malzemelerimi rahatlıkla aldı. Sırt paneli hava sirkülasyonu sağlayan tasarımıyla uzun taşımada sırt ısınmasını minimize ediyor. Fiyatı için kalitesi mükemmel.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(81, 2, 5, 'Organizasyon sistemi çok düşünülmüş', 'Çantanın cep ve bölme düzeni saha koşullarında malzemelere hızlı erişimi sağlıyor. Yan ceplerden su matarası kolayca alınabiliyor; bu küçük detay uzun yürüyüşlerde önemli. Yağmur siperi de dahil gelmesi büyük bir artı. Decathlon yine fiyat/kalite dengesini doğru kurmuş.', TRUE, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(81, 3, 4, 'İyi çanta, bel kemeri biraz dar', 'Genel olarak memnunum ama bel kemeri büyük beden kullanıcılar için biraz dar kalabiliyor. Bel ölçünüz 90cm üzerindeyse önce denemenizi öneririm. Omuz askısı ve sırt pedi ise son derece konforlu. Orta seviye trekkingciler için harika değer sunan bir ürün.', TRUE, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- 82: Black Diamond Spot 400 Kafa Feneri
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(82, 1, 5, 'Kamp ve hikayenin vazgeçilmezi', 'Spot 400''ü birden fazla kış kampında kullandım. 400 lümen gece dağ yolunu kolaylıkla aydınlatıyor. IPX8 su geçirmezliği yağmurlu havalarda hiç sorun yaşatmadı. PowerTap teknolojisi tam parlaklıktan gece moduna tek dokunuşla geçişi çok pratik kılıyor. Black Diamond kalitesi güven veriyor.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(82, 2, 5, 'Kırmızı ışık gece görüşünü koruyor', 'Kamp bölgesinde geceleri kırmızı ışık modunu kullanarak hem kendi gece görüşümü hem de çadır arkadaşlarımın uyumunu koruduk. Bu düşünceli tasarım detayı deneyimli üreticinin imzasını taşıyor. Pil ömrü de düşük güç modunda birkaç geceyi karşılıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(82, 3, 4, 'İyi fener, arka ışık hissettiriyor', 'Performans ve dayanıklılık açısından beğendim. Tek şikâyetim yoğun kullanımda başlık bandının arkasının biraz ısınması. Bu uzun süre kullanımda fark edilen küçük bir detay. Genel kullanım konforunu bozmayacak düzeyde ama gelecek versiyonda iyileştirilebilir.', TRUE, DATE_SUB(NOW(), INTERVAL 17 DAY));

-- 83: Buff Lightweight Merino Wool Balaclava Yün Boyunluk
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(83, 1, 5, 'Soğuk hava sporlarının gizli silahı', 'Merino yünü boyunluğu kayak ve snowboard sezonumda dönüştürücü bir ürün oldu. Doğal termal yalıtımı ve nemi vücuttan uzaklaştırma özelliği sentetik alternatiflerin çok üzerinde. -15°C''de boya kadar kullandığımda boyun ve yüz bölgesi yeterince sıcak kaldı. Kaşımıyor, bu büyük artı.', TRUE, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(83, 2, 5, 'Çok yönlü kullanım şaşırttı', 'Boyunluk, balaclava, bant, şapka gibi farklı biçimlerde kullanılabiliyor. Trekking, koşu ve günlük kullanımda farklı konfigürasyonları denedim. Merino yününün koku önleyici özelliği birden fazla gün kullanımda önemli bir avantaj. Hafifliği ve kompaktlığı da seyahatte yer kaplamıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(83, 3, 4, 'Mükemmel malzeme, dikkatli yıkama gerekiyor', 'Merino yünü performansı gerçekten üstün. Ancak makinede hassas yıkama programı şart; aksi halde küçülebiliyor. Bu bakım gereksinimi alıştıktan sonra sorun olmaktan çıkıyor. Outdoor aktiviteler için uzun vadeli yatırım olarak kesinlikle değer.', TRUE, DATE_SUB(NOW(), INTERVAL 21 DAY));

-- 84: Osprey Daylite Plus 20L Günlük Sırt Çantası
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at) VALUES
(84, 1, 5, 'Günlük kullanım ve hafif trekking için ideal', 'Daylite Plus''ı hem şehir günlük kullanımında hem de kısa day-hike''larda kullanıyorum. 20L hacim, 13" laptop, su matarası ve gün boyu ihtiyacımı karşılayan malzemeleri rahatlıkla alıyor. Osprey''nin AirScape omuz askısı ağırlığı omuzlara eşit dağıtıyor. Yıllar içinde yıpranma yok.', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(84, 2, 5, 'Seyahat kabini çantası olarak mükemmel', 'Kısa iş seyahatlerinde kabin bagajı olarak kullanıyorum. Laptop bölmesi, organizasyon cepleri ve dışarıdan erişilebilir su matarası cebi birleşimi seyahati kolaylaştırıyor. Sırt paneli uzun bekleme sürelerinde bile konforlu. Osprey All Mighty Guarantee kalite güvencesi ek değer katıyor.', TRUE, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(84, 3, 5, 'Tasarım zekice, ağırlık minimal', 'Osprey çantaların tasarım zekâsı bu modelde de kendini gösteriyor: her cep ve bölme nerede olduğunda en işlevsel olacağı düşünülerek konumlandırılmış. 500g ağırlık bu kapasitede sektörün en hafiflerinden. Günlük + outdoor karma kullanım için biçilmiş kaftan.', TRUE, DATE_SUB(NOW(), INTERVAL 18 DAY));

-- ============================================================
-- Toplam: 150 INSERT satırı (50 ürün × 3 kullanıcı)
-- ============================================================
