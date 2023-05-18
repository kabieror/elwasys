# elwasys #

![Screenshot](https://raw.githubusercontent.com/kabieror/elwasys-raspi-client/master/docs/screenshot-startscreen-md.png)

![Screenshot](https://raw.githubusercontent.com/kabieror/elwasys-raspi-client/master/docs/screenshot-confirmation-md.png)

This client was developed to run on a RaspberryPi with the official 7" touch screen.
It uses the OpenSource [FHEM-Server](https://fhem.de/) to communicate with remote controlled sockets.
The sockets are switched on only, if a user has enough credit and are switched off again, when the washer is done.

## Features

- Use RFID Cards to identify users at the terminal
- Pre-Paid washing
- Detect end of program by measuring power consumption (=> vendor independent)
- Email- and push-notification when the laundry is ready
- Fixed price or time-based billing
- Fine-grained permissions
  - Special prices per user group
  - Deny access to a washing machine for a certain user group
