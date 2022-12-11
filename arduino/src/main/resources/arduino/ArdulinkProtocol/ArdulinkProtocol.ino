// include this to support the RGB chainable LED
#include <ChainableLED.h>

// include this to support the MP3 player
#include "KT403A_Player.h"
#include <SoftwareSerial.h>

SoftwareSerial SSerial(2, 3);
KT403A<SoftwareSerial> Mp3Player;
boolean mp3PlayerInit = false;

String inputString = "";         // a string to hold incoming data (this is general code you can reuse)
boolean stringComplete = false;  // whether the string is complete (this is general code you can reuse)

#define delayInMilli 4
#define readings 4
#define baudRate 9600
#define messageStartsWith "alp://"
#define digitalPinListeningNum 14 // Change 14 if you have a different number of pins.
#define analogPinListeningNum 6 // Change 6 if you have a different number of pins.
boolean digitalPinListening[digitalPinListeningNum]; // Array used to know which pins on the Arduino must be listening.
boolean analogPinListening[analogPinListeningNum]; // Array used to know which pins on the Arduino must be listening.
int digitalPinListenedValue[digitalPinListeningNum]; // Array used to know which value is read last time.
int analogPinListenedValue[analogPinListeningNum]; // Array used to know which value is read last time.

void setup() {
  // initialize serial: (this is general code you can reuse)
  Serial.begin(baudRate);
  while (!Serial); // Wait until Serial not connected (because difference between Leonardo and Micro with UNO and others)

  Serial.print("alp://rply/");
  Serial.print("ok?id=0");
  Serial.print('\n'); // End of Message
  Serial.flush();

  //set to false all listen variable
  int index = 0;
  for (index = 0; index < digitalPinListeningNum; index++) {
    digitalPinListening[index] = false;
    digitalPinListenedValue[index] = -1;
  }

  for (index = 0; index < analogPinListeningNum; index++) {
    analogPinListening[index] = false;
    analogPinListenedValue[index] = -1;
  }

  // Turn off everything (not on RXTX)
  for (index = 2; index < digitalPinListeningNum; index++) {
    pinMode(index, OUTPUT);
    digitalWrite(index, LOW);
  }
}

void loop() {
  // when a newline arrives:
  if (stringComplete) {
    if (inputString.startsWith(messageStartsWith)) {
      boolean msgRecognized = true;
      // Store reply parameters.
      String arguments = "";

      if (inputString.substring(6, 10) == "ppin") { // Power Pin Intensity (this is general code you can reuse)
        int separatorPosition = inputString.indexOf('/', 11 );
        String pin = inputString.substring(11, separatorPosition);
        String intens = inputString.substring(separatorPosition + 1);
        pinMode(pin.toInt(), OUTPUT);
        analogWrite(pin.toInt(), intens.toInt());

      } else if (inputString.substring(6, 10) == "ppsw") { // Power Pin Switch (this is general code you can reuse)
        int separatorPosition = inputString.indexOf('/', 11 );
        String pin = inputString.substring(11, separatorPosition);
        String power = inputString.substring(separatorPosition + 1);
        pinMode(pin.toInt(), OUTPUT);
        if (power.toInt() == 1) {
          digitalWrite(pin.toInt(), HIGH);
        } else if (power.toInt() == 0) {
          digitalWrite(pin.toInt(), LOW);
        }

      } else if (inputString.substring(6, 10) == "tone") { // tone request (this is general code you can reuse)
        int firstSlashPosition = inputString.indexOf('/', 11 );
        int secondSlashPosition = inputString.indexOf('/', firstSlashPosition + 1 );
        int pin = inputString.substring(11, firstSlashPosition).toInt();
        int frequency = inputString.substring(firstSlashPosition + 1, secondSlashPosition).toInt();
        int duration = inputString.substring(secondSlashPosition + 1).toInt();
        if (duration == -1) {
          tone(pin, frequency);
        } else {
          tone(pin, frequency, duration);
        }

      } else if (inputString.substring(6, 10) == "notn") { // no tone request (this is general code you can reuse)
        int firstSlashPosition = inputString.indexOf('/', 11 );
        int pin = inputString.substring(11, firstSlashPosition).toInt();
        noTone(pin);

      } else if (inputString.substring(6, 10) == "srld") { // Start Listen Digital Pin (this is general code you can reuse)
        String pin = inputString.substring(11);
        digitalPinListening[pin.toInt()] = true;
        digitalPinListenedValue[pin.toInt()] = -1; // Ensure a message back when start listen happens.
        pinMode(pin.toInt(), INPUT);

      } else if (inputString.substring(6, 10) == "spld") { // Stop Listen Digital Pin (this is general code you can reuse)
        String pin = inputString.substring(11);
        digitalPinListening[pin.toInt()] = false;
        digitalPinListenedValue[pin.toInt()] = -1; // Ensure a message back when start listen happens.

      } else if (inputString.substring(6, 10) == "srla") { // Start Listen Analog Pin (this is general code you can reuse)
        String pin = inputString.substring(11);
        analogPinListening[pin.toInt()] = true;
        analogPinListenedValue[pin.toInt()] = -1; // Ensure a message back when start listen happens.

      } else if (inputString.substring(6, 10) == "spla") { // Stop Listen Analog Pin (this is general code you can reuse)
        String pin = inputString.substring(11);
        analogPinListening[pin.toInt()] = false;
        analogPinListenedValue[pin.toInt()] = -1; // Ensure a message back when start listen happens.

      } else if (inputString.substring(6, 10) == "cust") {
        int separatorPosition = inputString.indexOf('/', 11 );
        int messageIdPosition = inputString.indexOf('?', 11 );

        String customCommand = inputString.substring(11, separatorPosition);
        String value = inputString.substring(separatorPosition + 1, messageIdPosition);

        if (customCommand == "pinMode") {
          // value = ${pinId}/${mode}
          int slashPosition = value.indexOf('/');
          int pinId = value.substring(0, slashPosition).toInt();
          String mode = value.substring(slashPosition + 1);

          // Update pin mode.
          pinMode(pinId, mode == "OUTPUT" ? OUTPUT : INPUT);

        } else if (customCommand == "chainableLED") {
          // value = ${pindId}/${intensity}
          int slashPosition = value.indexOf('/');
          int pinId = value.substring(0, slashPosition).toInt();
          double intensity = value.substring(slashPosition + 1).toDouble();

          // Store arguments for future reference.
          arguments.concat("&pinId=");
          arguments.concat(pinId);
          arguments.concat("&intensity=");
          arguments.concat(intensity);

          ChainableLED leds(pinId, pinId + 1, 1);
          leds.init();
          leds.setColorHSL(0, 0, 0, intensity);

        } else if (customCommand == "mp3") {
          // value = ${pindId}/${operation}
          int slashPosition = value.indexOf('/');
          int pinId = value.substring(0, slashPosition).toInt();

          String newValue = value.substring(slashPosition + 1);
          slashPosition = newValue.indexOf('/');
          String operation = newValue.substring(0, slashPosition);

          arguments.concat("&pinId=");
          arguments.concat(pinId);
          arguments.concat("&operation=");
          arguments.concat(operation);

          if (!mp3PlayerInit) {
            mp3PlayerInit = true;

            SSerial = SoftwareSerial (pinId, pinId + 1);
            SSerial.begin(9600);
            Mp3Player.init(SSerial);
          }

          if (operation == "pause") {
            Mp3Player.pause();

          } else if (operation == "resume") {
            Mp3Player.play();

          } else if (operation == "next") {
            Mp3Player.next();

          } else if (operation == "previous") {
            Mp3Player.previous();

          } else if (operation == "volume") {
            uint8_t volume = newValue.substring(slashPosition + 1).toInt();
            Mp3Player.volume(volume);

          } else if (operation == "play") {
            uint8_t songIndex = newValue.substring(slashPosition + 1).toInt();
            Mp3Player.playSongIndex(songIndex);

          } else if (operation == "stop") {
            Mp3Player.stop();
            mp3PlayerInit = false;
          }

        } else {
          msgRecognized = false;
        }

      } else {
        msgRecognized = false; // this sketch doesn't know other messages in this case command is ko (not ok)
      }

      // Prepare reply message if caller supply a message id (this is general code you can reuse)
      int idPosition = inputString.indexOf("?id=");
      if (idPosition != -1) {
        String id = inputString.substring(idPosition + 4);
        // This line is necessary because, by default, the ID ends with an \n, ending the stream.
        id.replace("\n", "");

        // Start printing the reply.
        Serial.print("alp://rply/");
        Serial.print(msgRecognized == true ? "ok" : "ko");
        Serial.print("?id=" + id + arguments + "\n");
        Serial.flush();
      }
    }

    // clear the string:
    inputString = "";
    stringComplete = false;
  }

  // Send listen messages
  int index = 0;
  for (index = 0; index < digitalPinListeningNum; index++) {
    if (digitalPinListening[index] == true) {
      int value = digitalRead(index);
      // if (value != digitalPinListenedValue[index]) {
      digitalPinListenedValue[index] = value;
      Serial.print("alp://dred/");
      Serial.print(index);
      Serial.print("/");
      Serial.print(value);
      Serial.print('\n'); // End of Message
      Serial.flush();
      // }
    }
  }

  for (index = 0; index < analogPinListeningNum; index++) {
    if (analogPinListening[index] == true) {
      int value = highPrecisionAnalogRead(index);
      // if (value != analogPinListenedValue[index]) {
      analogPinListenedValue[index] = value;
      Serial.print("alp://ared/");
      Serial.print(index);
      Serial.print("/");
      Serial.print(value);
      Serial.print('\n'); // End of Message
      Serial.flush();
      // }
    }
  }
}

// Read ${readings} times the value.
int highPrecisionAnalogRead(int pin) {
  double average = analogRead(pin);

  for (int i = 2; i <= readings; i++) {
    average = updateAverage(i, analogRead(pin), average);
  }

  int result = (int) average;
  delay(delayInMilli);
  return result;
}

double updateAverage(int i, int value, double oldAverage) {
  return ((i - 1.0) / i) * (oldAverage + value / (i - 1.0));
}

/*
  SerialEvent occurs whenever a new data comes in the
  hardware serial RX.  This routine is run between each
  time loop() runs, so using delay inside loop can delay
  response.  Multiple bytes of data may be available.
  This is general code you can reuse.
*/
void serialEvent() {
  while (Serial.available() && !stringComplete) {
    // get the new byte:
    char inChar = (char)Serial.read();
    // add it to the inputString:
    inputString += inChar;
    // if the incoming character is a newline, set a flag
    // so the main loop can do something about it:
    if (inChar == '\n') {
      stringComplete = true;
    }
  }
}
