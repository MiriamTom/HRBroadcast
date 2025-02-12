# **📡 HR Broadcast App**  

## 📝 **Popis aplikácie**  
HR Broadcast je Android aplikácia na zobrazovanie a ukladanie informácií o zariadeniach, ktoré odosielajú údaje o srdcovom tepe. Dáta sa zobrazujú v aplikácii pomocou **RecyclerView** a zároveň sa ukladajú do **Firebase Firestore** pre ďalšie spracovanie.  

---

## 🚀 **Funkcionalita**  
✅ Zobrazenie zoznamu zariadení a ich srdcového tepu  
✅ Automatická synchronizácia dát s **Firebase Firestore**  
✅ Použitie **MVVM architektúry** na správu údajov  
✅ **LiveData** pre dynamickú aktualizáciu UI  
✅ Navigácia medzi fragmentmi pomocou **Jetpack Navigation**  

---

## 🔧 **Technológie**  
- **Kotlin / Java**  
- **Android Jetpack (ViewModel, LiveData, Navigation)**  
- **RecyclerView**  
- **Firebase Firestore**  
- **Material Design Components**  

---

## 📂 **Štruktúra projektu**  
```
/hr_broadcast
│── /app
│   ├── /src/main/java/com/example/hr_broadcast
│   │   ├── MainFragment.java        # Hlavný fragment s RecyclerView
│   │   ├── SharedViewModel.java     # ViewModel na správu dát
│   │   ├── Device.java              # Dátová trieda pre zariadenia
│   │   ├── DeviceAdapter.java       # Adapter pre RecyclerView
│   │   ├── FirestoreRepository.java # Trieda na komunikáciu s Firestore
│   ├── /res/layout
│   │   ├── fragment_main.xml        # UI pre MainFragment
│   │   ├── item_device.xml          # Layout pre jednotlivé zariadenia
│   ├── AndroidManifest.xml
│── /gradle
│── build.gradle
│── README.md
```

---

## ⚙ **Inštalácia a spustenie**  
1️⃣ **Naklonuj projekt**  
```bash
git clone https://github.com/tvoje-repo/hr-broadcast.git
cd hr-broadcast
```
2️⃣ **Otvoriť v Android Studio**  
3️⃣ **Pridať Firebase do projektu**  
   - Stiahni `google-services.json` z Firebase Console  
   - Ulož ho do `app/` priečinka  
4️⃣ **Spusti aplikáciu na emulátore alebo fyzickom zariadení**  

---

## 🔥 **Práca s Firestore**  
- **Firestore kolekcia:** `devices`  
- **Štruktúra dokumentu:**  
```json
{
  "name": "Test Device",
  "heartRate": 75
}
```
- **Firestore pravidlá:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /devices/{deviceId} {
      allow read, write: if true;
    }
  }
}
```

---

## 📌 **TODO / Budúce vylepšenia**  
🔹 Ukladať historické dáta o srdcovom tepe  

---

## 💡 **Autor**  
👤 **Miriam Tomášová**  

---
