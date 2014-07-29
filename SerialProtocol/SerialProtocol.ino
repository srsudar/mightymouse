/*
  Serial Protocol, modified from the SerialCallResponse example
  http://www.arduino.cc/en/Tutorial/SerialCallResponse
*/

// Special bytes
#define  START_BYTE      255
#define  CLICK_BEGIN      67 // 'C'
#define  KEY_BEGIN        75 // 'K'
#define  MOUSE_BEGIN      77 // 'M'

// State machine states
#define  WAIT_FOR_START   0  // Expecting a START_BYTE next
#define  STARTED          1  // We got a START_BYTE, looking for a command type next
#define  CLICK_GET_0     10  // Expecting a click type (button number)
#define  KEY_GET_0       20  // Expecting an ASCII code to type
#define  MOUSE_GET_0     30  // Expecting an x-coordinate -127 to 127
#define  MOUSE_GET_1     31  // Expecting a y-coordinate -127 to 127
#define  MOUSE_GET_2     32  // Expecting a scroll amount -127 to 127

int currentState = WAIT_FOR_START;   // Current state machine state

int mouseX = 0;              // Hold the mouse move-by-x val until command is complete
int mouseY = 0;              // Hold the mouse move-by-y val until command is complete

void printError(int intVal);

void setup()
{
  // start serial ports at 9600 bps:
  Serial.begin(9600);
  Serial1.begin(9600);
  Keyboard.begin();
  Mouse.begin();
}

void loop()
{
  // if we get a valid byte, read analog ins:
  if (Serial1.available() > 0) {
    // get incoming byte and convert to int
    char inChar = Serial1.read();
    int intVal = (int) inChar;
    
    // Any time we receive START_BYTE, start over
    if (intVal == START_BYTE){
      currentState = STARTED;
      return;
    }
    
    switch (currentState){
      case WAIT_FOR_START:
        // If we're here, then we didn't get the START_BYTE, so print an error
        printError(0,intVal);
        break;
        
      case STARTED:
        // Set state according to this byte (should be one of the BEGINs)
        switch (intVal) {
          
          case CLICK_BEGIN:
            currentState = CLICK_GET_0;
            break;
            
          case KEY_BEGIN:
            currentState = KEY_GET_0;
            break;
            
          case MOUSE_BEGIN:
            currentState = MOUSE_GET_0;
            break;
            
          default:
            // If we didn't get a correct type byte, print an error
            printError(5,intVal);
            break;
        }        
        break;
        
      case CLICK_GET_0:
        // For now, we disregard the type of click information
        Mouse.click();
        currentState = WAIT_FOR_START;
        break;
        
      case KEY_GET_0:
        Keyboard.print(inChar);
        currentState = WAIT_FOR_START;
        break;
        
      case MOUSE_GET_0:
        mouseX = intVal; // Store the x value for later
        currentState = MOUSE_GET_1;
        break;
        
      case MOUSE_GET_1:
        mouseY = intVal; // Store the y value for later      
        currentState = MOUSE_GET_2;
        break;
        
      case MOUSE_GET_2:
        // Now we have all we need for the mouse command.
        // Execute the command and reset the global vars
        Mouse.move(mouseX, mouseY, intVal);
        mouseX = 0;
        mouseY = 0;
        currentState = WAIT_FOR_START;
        break;
        
      default:
         printError(10,intVal);
         break;
    }
  }
}

void printError(int location, int intVal){
  Serial1.print("E<");
  Serial1.print(currentState);
  Serial1.print(",");
  Serial1.print(location);
  Serial1.print(",");
  Serial1.print(intVal);
  Serial1.println(">");
   
  // Reset state to wait
  currentState = WAIT_FOR_START;
}

