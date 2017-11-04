from __future__ import print_function
from __future__ import division

from threading import *
from bluetooth import *

import time

import socket

import brickpi3

try:

    angle = 0
    distance = 0

    BTMessage = ""

    print("Willkommen!\n")

    speed = 30 * -1
    run = True

    BP = brickpi3.BrickPi3()
    BP.set_motor_power(BP.PORT_A + BP.PORT_B + BP.PORT_C + BP.PORT_D, 0)

    BP.offset_motor_encoder(BP.PORT_A, BP.get_motor_encoder(BP.PORT_A))
    BP.offset_motor_encoder(BP.PORT_B, BP.get_motor_encoder(BP.PORT_B))
    BP.offset_motor_encoder(BP.PORT_C, BP.get_motor_encoder(BP.PORT_C))
    BP.offset_motor_encoder(BP.PORT_D, BP.get_motor_encoder(BP.PORT_D))

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
        wlan_sock.bind(("192.168.2.108", 1337))
        wlan_sock.listen(1)
        return wlan_sock


    def getWlanClientConnection(server_sock):
        print("Waiting for WlanConnection\n")
        client_sock, client_info = server_sock.accept()
        print("accepted WlanConnection from ", client_info)
        return client_sock


    class BTThread(Thread):
        global BTMessage

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
                        BTMessage = data
                        client.send("Echo from Pi: [%s]\n" % BTMessage)
                except IOError:
                    pass
                    break


    class WlanThread(Thread):
        def __init__(self, name):
            Thread.__init__(self)
            self.name = name

        def run(self):
            global angle
            global distance
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
                            data2 = data2[2] + data2[4] + data2[6] + data2[8] + data2[10] + data2[12] + data2[14] \
                                    + data2[16] + data2[18]
                            data2 = data2.replace("y", "")
                            data2 = data2.replace("#", "")

                            breakpoint = data2.index(",")

                            distance = int(data2[breakpoint + 1:len(data2)])
                            angle = int(data2[0:breakpoint])

                            print("Data2: " + data2 + ", " + "Angle: " + str(angle) + ", " + "Distance: " + str(distance))

                            data2 = ""
                            data = ""

                            client.send("Echo from Pi: [%s]\n" % data)
                except IOError:
                    print(data + ", " + data2)
                    pass
                    break


    wlanThread = WlanThread("WlanThread")
    bluetoothThread = BTThread("BluetoothThread")
    wlanThread.start()
    bluetoothThread.start()

    old_dis = ""
    old_ang = ""

    while True:
        print("1. Running")

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

        print("2. Running")

        if distance != 0 and angle != 0 and len(str(distance)) != 0 and len(str(angle)) != 0:
            old_ang = angle
            old_dis = distance
            print("Angle: " + str(angle) + "     Distance: " + str(distance))
        else:
            print(str(distance) + "    " + str(angle))

        print("3. Running")

        if run and distance > 70:
            print("GO")
            if powerA != speed or powerD != speed:
                BP.set_motor_power(BP.PORT_A + BP.PORT_D, speed)

            if positionB != angle * 2 and positionC != angle * 2:
                print("Turning to " + str(angle * 2))
                BP.set_motor_position(BP.PORT_B, angle * 2)
                BP.set_motor_position(BP.PORT_C, angle * -2)
        else:
            print("STOP")
            if powerA != speed or powerB != 0 or powerC != 0 or powerD != 0:
                BP.set_motor_power(BP.PORT_A + BP.PORT_B + BP.PORT_C + BP.PORT_D, 0)

        print("4. Ende")
        time.sleep(0.2)
except KeyboardInterrupt:
    BP.set_motor_position(BP.PORT_B, 0)
    BP.set_motor_position(BP.PORT_C, 0)
    BP.reset_all()
    exit(0)
