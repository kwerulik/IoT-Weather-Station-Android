## 🔌 Schemat Połączeń (Pinout)

| Komponent | Pin Czujnika / Wyświetlacza | Pin Arduino | Uwagi Zasilania |
| :--- | :--- | :--- | :--- |
| **DHT22** (Temperatura/Wilg.) | DATA (OUT) | **D2** | VCC ➡️ 5V, GND ➡️ GND |
| **MQ-135** (Jakość Powietrza) | A0 (Analog Out) | **A0** | VCC ➡️ 5V, GND ➡️ GND |
| **BMP280** (Ciśnienie) | SDA | **SDA** (lub A4) | ⚠️ VCC ➡️ **3.3V**, GND ➡️ GND |
| | SCL | **SCL** (lub A5) | |
| **Wyświetlacz 5161AS** | COM (Wspólna Katoda) | **GND** | *Koniecznie przez rezystor 220-330Ω!* |
| | Segment A (Góra, pin 4) | **D5** | |
| | Segment B (Góra, pin 5) | **D6** | |
| | Segment C (Dół, pin 4) | **D7** | |
| | Segment D (Dół, pin 2) | **D8** | |
| | Segment E (Dół, pin 1) | **D9** | |
| | Segment F (Góra, pin 2) | **D10** | |
| | Segment G (Góra, pin 1) | **D11** | |