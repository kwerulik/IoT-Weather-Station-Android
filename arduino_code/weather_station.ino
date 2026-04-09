#include <Wire.h>
#include <Adafruit_BMP085.h> // Zmień na Adafruit_BMP280.h jeśli używasz wersji 280
#include "DHT.h"

// --- KONFIGURACJA CZUJNIKÓW ---
#define DHTPIN 2
#define DHTTYPE DHT22
DHT dht(DHTPIN, DHTTYPE);
Adafruit_BMP085 bmp;

// --- DANE SIECI I THINGSPEAK ---
const String ssid = "NAZWA_TWOJEGO_WIFI";
const String password = "HASLO_DO_WIFI";
const String apiKey = "WQGFK92TDNNE34WA";
const String host = "api.thingspeak.com";

// --- PINY WYŚWIETLACZA 7-SEGMENTOWEGO ---
const int segA = 5;
const int segB = 6;
const int segC = 7;
const int segD = 8;
const int segE = 9;
const int segF = 10;
const int segG = 11;

// --- FUNKCJA STERUJĄCA WYŚWIETLACZEM ---
void pokazStan(char stan) {
  // Gaszenie wszystkich segmentów
  digitalWrite(segA, LOW); digitalWrite(segB, LOW); digitalWrite(segC, LOW);
  digitalWrite(segD, LOW); digitalWrite(segE, LOW); digitalWrite(segF, LOW);
  digitalWrite(segG, LOW);

  // Zapalanie odpowiednich wzorów
  if(stan == '0') {
    digitalWrite(segA, HIGH); digitalWrite(segB, HIGH); digitalWrite(segC, HIGH);
    digitalWrite(segD, HIGH); digitalWrite(segE, HIGH); digitalWrite(segF, HIGH);
  }
  else if(stan == '1') {
    digitalWrite(segB, HIGH); digitalWrite(segC, HIGH);
  }
  else if(stan == '2') {
    digitalWrite(segA, HIGH); digitalWrite(segB, HIGH); digitalWrite(segD, HIGH);
    digitalWrite(segE, HIGH); digitalWrite(segG, HIGH);
  }
  else if(stan == '3') {
    digitalWrite(segA, HIGH); digitalWrite(segB, HIGH); digitalWrite(segC, HIGH);
    digitalWrite(segD, HIGH); digitalWrite(segG, HIGH);
  }
  else if(stan == 'E') {
    digitalWrite(segA, HIGH); digitalWrite(segD, HIGH); digitalWrite(segE, HIGH);
    digitalWrite(segF, HIGH); digitalWrite(segG, HIGH);
  }
}

void setup() {
  // Inicjalizacja pinów wyświetlacza
  pinMode(segA, OUTPUT); pinMode(segB, OUTPUT); pinMode(segC, OUTPUT);
  pinMode(segD, OUTPUT); pinMode(segE, OUTPUT); pinMode(segF, OUTPUT);
  pinMode(segG, OUTPUT);

  // STAN 0: URUCHAMIANIE
  pokazStan('0');
  
  Serial.begin(115200);
  dht.begin();
  bmp.begin();
  
  delay(2000); // Rozgrzewanie czujników

  // STAN 1: ŁĄCZENIE Z WI-FI
  Serial.println("AT+CWMODE=1");
  delay(1000);
  Serial.println("AT+CWJAP=\"" + ssid + "\",\"" + password + "\"");
  
  // Animacja migającej '1' podczas łączenia z Wi-Fi (ok. 7 sekund)
  for(int i = 0; i < 14; i++) {
    pokazStan('1');
    delay(250);
    pokazStan('X'); // X oznacza zgaśnięcie
    delay(250);
  }
  
  // STAN 2: POŁĄCZONO / CZUWANIE
  pokazStan('2');
}

void loop() {
  // Odczyt danych z czujników
  float t = dht.readTemperature();
  float h = dht.readHumidity();
  int air = analogRead(A0);
  int press = bmp.readPressure() / 100; // Podzielone na 100, aby uzyskać hPa

  // Zabezpieczenie przed błędem czujnika DHT
  if (isnan(t) || isnan(h)) {
    pokazStan('E'); // Wyświetl ERROR
    delay(5000);
    pokazStan('2'); // Wróć do czuwania
    return;
  }

  // STAN 3: WYSYŁANIE DANYCH DO THINGSPEAK
  pokazStan('3');
  
  String url = "/update?api_key=" + apiKey + "&field1=" + String(t) + "&field2=" + String(h) + "&field3=" + String(air) + "&field4=" + String(press);
  String httpRequest = "GET " + url + " HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n\r\n";

  // Komunikacja z chmurą za pomocą komend AT
  Serial.println("AT+CIPSTART=\"TCP\",\"" + host + "\",80");
  delay(2000);

  Serial.print("AT+CIPSEND=");
  Serial.println(httpRequest.length());
  delay(1000);
  Serial.print(httpRequest);

  delay(2000); // Czas na zamknięcie transmisji

  // Powrót do STANU 2: CZUWANIE
  pokazStan('2');
  
  // ThingSpeak przyjmuje dane co 15 sekund
  delay(15000); 
}