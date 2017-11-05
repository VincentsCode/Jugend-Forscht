from __future__ import print_function
from __future__ import division

from threading import *
from bluetooth import *

import time

import socket

import brickpi3

# Variable Replacements
null = None
true = True
false = False

# Create Variables
angle = 0
distance = 0
BTMessage = ""
run = True
WlanConnected = False
BluetoothConnected = False

# Settings
speed = -30
ip = "127.0.0.1"
port = 1337

# Create Instance of BrickPi and Reset all Motors
BP = brickpi3.BrickPi3()
BP.set_motor_power(BP.PORT_A + BP.PORT_B + BP.PORT_C + BP.PORT_D, 0)
BP.offset_motor_encoder(BP.PORT_A, BP.get_motor_encoder(BP.PORT_A))
BP.offset_motor_encoder(BP.PORT_B, BP.get_motor_encoder(BP.PORT_B))
BP.offset_motor_encoder(BP.PORT_C, BP.get_motor_encoder(BP.PORT_C))
BP.offset_motor_encoder(BP.PORT_D, BP.get_motor_encoder(BP.PORT_D))


# Create a Wlan-Receiver
def initWlanServer():
    wlan_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    wlan_sock.bind((ip, port))
    wlan_sock.listen(1)
    return wlan_sock


# Create a Bluetooth-Receiver
def initBTServer():
    bt_sock = BluetoothSocket(RFCOMM)
    bt_sock.bind(("", PORT_ANY))
    bt_sock.listen(1)

    uuid = "00001101-0000-1000-8000-00805F9B34FB"

    advertise_service(bt_sock, "Bluetooth Server",
                      service_id=uuid,
                      service_classes=[uuid, SERIAL_PORT_CLASS],
                      profiles=[SERIAL_PORT_PROFILE])
    return bt_sock


# get Connection from Laptop
def getWlanClientConnection(server_sock):
    global WlanConnected

    print("Waiting for WlanConnection\n")
    client_sock, client_info = server_sock.accept()
    print("accepted WlanConnection from ", client_info)
    WlanConnected = True
    return client_sock


# get Connection from Smartphone
def getBTClientConnection(server_sock):
    global BluetoothConnected

    print("Waiting for BTConnection")
    client_sock, client_info = server_sock.accept()
    print("accepted BTConnection from ", client_info)
    BluetoothConnected = True
    return client_sock


# Thread which constantly returns the Values send from the Laptop
class WlanThread(Thread):
    def __init__(self, name):
        Thread.__init__(self)
        self.name = name

    def run(self):
        global distance, angle

        print("Wlan Thread gestartet \n")
        server = initWlanServer()

        while True:
            client = getWlanClientConnection(server)
            try:
                while True:
                    time.sleep(0.2)
                    data = client.recv(2048)
                    data = data.replace(" ", "")
                    if len(data) == 0:
                        break
                    if "#" in data:
                        i = data.index("#")
                        data2 = data[i:i + 20]
                        data2 = "{0}{1}{2}{3}{4}{5}{6}{7}{8}".format(data2[2], data2[4], data2[6], data2[8], data2[10],
                                                                     data2[12], data2[14], data2[16], data2[18])
                        data2 = data2.replace("y", "")
                        data2 = data2.replace("#", "")
                        breakpoint = data2.index(",")
                        try:
                            distance = int(data2[breakpoint + 1:len(data2)])
                            angle = int(data2[0:breakpoint])
                        except ValueError:
                            print(ValueError)

                        client.send("Data received!")
            except IOError:
                pass
                break


# Thread which returns Messages from the Smartphone
class BTThread(Thread):
    def __init__(self, name):
        Thread.__init__(self)
        self.name = name

    def run(self):
        global BTMessage

        print("Bluetooth Thread gestartet \n")
        server = initBTServer()

        while True:
            client = getBTClientConnection(server)
            try:
                while True:
                    data = client.recv(1024)
                    if len(data) == 0:
                        break
                    BTMessage = data
                    client.send("Data received")
            except IOError:
                pass
                break


# Creating Instances of both Threads and Starting them
wlanThread = WlanThread("WlanThread")
bluetoothThread = BTThread("BluetoothThread")
wlanThread.start()
bluetoothThread.start()

# Infinite Loop which reacts to the Data from the Threads by Moving the Robot
while True:

    # Collecting the States of the Motors
    statusA = BP.get_motor_status(BP.PORT_A)
    statusB = BP.get_motor_status(BP.PORT_B)
    statusC = BP.get_motor_status(BP.PORT_C)
    statusD = BP.get_motor_status(BP.PORT_D)
    powerA = statusA[1]
    powerB = statusB[1]
    powerC = statusC[1]
    powerD = statusD[1]
    positionB = statusB[2]
    positionC = statusC[2]

    # Check if Laptop and Smartphone are connected
    if WlanConnected and BluetoothConnected:

        # Check for new Bluetooth-Messages
        if len(str(BTMessage)) != 0:
            print("BTMessage:" + BTMessage)

            if "stop" in BTMessage:
                run = False
            elif "go" in BTMessage:
                run = True
            else:
                print(BTMessage)

            BTMessage = ""

        # Check if there are Angle- and Distance-Data to follow
        if len(str(distance)) != 0 and len(str(angle)) != 0:
            print("Angle: " + str(angle) + "     Distance: " + str(distance))

            # Check if the Robot isn't stopped by Bluetooth
            if run:

                # Check if the Person is far enough away
                if distance > 70:

                    # Power up Motor A and B
                    if powerA != speed or powerD != speed:
                        BP.set_motor_power(BP.PORT_A + BP.PORT_D, speed)

                    # Set the Robots Direction to Angle
                    if positionB != angle * 2 and positionC != angle * -2:
                        BP.set_motor_position(BP.PORT_B, angle * 2)
                        BP.set_motor_position(BP.PORT_C, angle * -2)

                else:
                    # Stopping the Robot, because the Person is to close
                    BP.set_motor_power(BP.PORT_A + BP.PORT_B + BP.PORT_C + BP.PORT_D, 0)
            else:
                # Stopping the Robot, because it was manually stopped
                BP.set_motor_power(BP.PORT_A + BP.PORT_B + BP.PORT_C + BP.PORT_D, 0)

    time.sleep(0.2)
