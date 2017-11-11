from __future__ import print_function
from __future__ import division

from threading import *
from bluetooth import *

import socket

import brickpi3

print("Willkommen!\n")

BTMessage = ""
WlanMessage = ""

run = False

power = 20

BrickPi = brickpi3.BrickPi3()

BrickPi.set_motor_power(BrickPi.PORT_A + BrickPi.PORT_B + BrickPi.PORT_C + BrickPi.PORT_D, 0)


def initBTServer():
    server_sock = BluetoothSocket(RFCOMM)
    server_sock.bind(("", PORT_ANY))
    server_sock.listen(1)

    uuid = "00001101-0000-1000-8000-00805F9B34FB"

    advertise_service(server_sock, "Echo Server",
                      service_id=uuid,
                      service_classes=[uuid, SERIAL_PORT_CLASS],
                      profiles=[SERIAL_PORT_PROFILE]
                      )
    return server_sock


def getBTClientConnection(server_sock):
    print("Waiting for BTConnection")
    client_sock, client_info = server_sock.accept()
    print("accepted BTConnection from ", client_info)
    return client_sock


def initWlanServer():
    wlan_sock = socket.socket(socket.AF_INET,
                              socket.SOCK_STREAM)
    wlan_sock.bind((socket.gethostname(), 1337))
    wlan_sock.listen(1)
    return wlan_sock

def getWlanClientConnection(server_sock):
    print("Waiting for WlanConnection")
    client_sock, client_info = server_sock.accept()
    print("accepted WlanConnection from ", client_info)
    return client_sock


class BTThread(Thread):
    def __init__(self, name):
        Thread.__init__(self)
        self.name = name

    def run(self):
        print("Bluetooth Thread gestartet \n")
        server = initBTServer()
        while True:
            client = getBTClientConnection(server)
            try:
                while True:
                    data = client.recv(1024)
                    if len(data) == 0:
                        break
                    print("BT: received [%s]" % data)
                    BTMessage = data
                    client.send("Echo from Pi: [%s]\n" % BTMessage)
            except IOError:
                pass


class WlanThread(Thread):
    def __init__(self, name):
        Thread.__init__(self)
        self.name = name

    def run(self):
        print("Wlan Thread gestartet \n")
        server = initWlanServer()
        while True:
            client = getWlanClientConnection(server)
            try:
                while True:
                    data = client.recv(1024)
                    if len(data) == 0:
                        break
                    print("Wlan: received [%s]" % data)
                    WlanMessage = data
                    client.send("Echo from Pi: [%s]\n" % WlanMessage)
            except IOError:
                pass

bluetoothThread = BTThread("BluetoothThread")
wlanThread = WlanThread("WlanThread")

bluetoothThread.start()
wlanThread.start()

while True:
    statusA = BrickPi.get_motor_status(BrickPi.PORT_A)
    statusB = BrickPi.get_motor_status(BrickPi.PORT_B)
    statusC = BrickPi.get_motor_status(BrickPi.PORT_C)
    statusD = BrickPi.get_motor_status(BrickPi.PORT_D)

    powerA = statusA[1]
    powerB = statusB[1]
    powerC = statusC[1]
    powerD = statusD[1]

    positionB = statusB[2]
    positionC = statusC[2]

    positionWheelB = positionB / 2
    positionWheelC = positionC / 2

    if BTMessage != "":
        print("BTMessage: " + BTMessage)
        if BTMessage == "stop":
            run = False
            BrickPi.set_motor_power(BrickPi.PORT_A, 0)
            BrickPi.set_motor_power(BrickPi.PORT_D, 0)
        elif BTMessage == "weiter":
            run = True
            BrickPi.set_motor_power(BrickPi.PORT_A, power)
            BrickPi.set_motor_power(BrickPi.PORT_D, power)
        else:
            print("Error: BTMessage = " + BTMessage)
        BTMessage = ""

    if WlanMessage != "":
        print("WlanMessage: " + WlanMessage)

"""
    if WlanMessage != "" and run:
        if int(WlanMessage) > 0:
            print("Rechts")
            if powerA == 0 or powerB == 0:
                BrickPi.set_motor_power(BrickPi.PORT_A, power)
                BrickPi.set_motor_power(BrickPi.PORT_D, power)
            # getValue of Motor B & C -> Calc -> set_motor_power
        elif int(WlanMessage) < 0:
            print("Links")
            if powerA == 0 or powerB == 0:
                BrickPi.set_motor_power(BrickPi.PORT_A, power)
                BrickPi.set_motor_power(BrickPi.PORT_D, power)
            # getValue of Motor B & C -> Calc -> set_motor_power
        elif int(WlanMessage) == 0:
            print("Gerade")
            if powerA == 0 or powerB == 0:
                BrickPi.set_motor_power(BrickPi.PORT_A, power)
                BrickPi.set_motor_power(BrickPi.PORT_D, power)
            # getValue of Motor B & C -> Reverse -> set_motor_power
        else:
            print("Error: WlanMessage = " + WlanMessage)
            BrickPi.set_motor_power(BrickPi.PORT_A + BrickPi.PORT_B + BrickPi.PORT_C + BrickPi.PORT_D, 0)
"""