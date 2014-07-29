# mighty mouse

> Turn your android device phone into a mouse and keyboard.

## Overview

Mighty Mouse is an Android app and arduino code that together allow android
devices to act as a USB keyboard and mouse. The arduino becomes a USB dongle.
It also lets you transfer entire files by typing them, which could be useful
for system admins.

This code arose as a project for the CSE 550 Systems course at the University
of Washington and is the work of Sam Sudar and Jaylen VanOrden.

The code leaned heavily on the Bluetooth example projects in the Android Open
Source Project.

### Caveats!

Note that, as this was course code, this comes with some big ol' caveats! We
hacked it together to meet deadlines and worked on it for fun. This means there
are no tests, and neither of us would say this is the best code we have
written. It worked and was a pretty neat idea, though.

### Current status

First note the above caveats! Overall the repo is in a state of
disrepair. The Android code lives in the `MightyMouse/` directory. The code for
the Arduino Leonardo component lives in `SerialProtocol`. We were also
prototyping LUFA-style code that would work on more primitive devices, but this
never made it out of the prototyping stages. The code lives in `LUFA-120730/`.
