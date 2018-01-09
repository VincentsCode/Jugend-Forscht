from __future__ import print_function
from __future__ import division

from threading import *

import time

import socket

null = None

angle = 0
distance = 0

BTMessage = ""

print("Willkommen!\n")

speed = -20
run = True

f = open('out.txt', 'w')

connected = False

data = null
data2 = null


def initWlanServer():
    wlan_sock = socket.socket(socket.AF_INET,
                              socket.SOCK_STREAM)
    wlan_sock.bind(("127.0.0.1", 1337))
    wlan_sock.listen(1)
    return wlan_sock


def getWlanClientConnection(server_sock):
    global connected

    print("Waiting for WlanConnection\n")
    client_sock, client_info = server_sock.accept()
    print("accepted WlanConnection from ", client_info)
    connected = True
    return client_sock


class WlanThread(Thread):
    def __init__(self, name):
        Thread.__init__(self)
        self.name = name

    def run(self):
        global angle, data, data2
        global distance
        print("Wlan Thread gestartet \n")
        server = initWlanServer()
        while True:
            client = getWlanClientConnection(server)
            f.write("Gestartet")
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
                            f.write("Data: " + data + "   Data2: " + data2 + "  " + "Angle: " + str(angle) + "  " +
                                    "Distance: " + str(distance))
                            print(ValueError)
                            f.close()

                        print("Data2: " + data2 + " -> " + "Angle: " + str(angle) + "  " + "Distance: " + str(distance))
                        f.write("Data: " + data + "   Data2: " + data2 + "  " + "Angle: " + str(angle) + "  " +
                                "Distance: " + str(distance))

                        data2 = ""
                        data = ""

                        client.send("Echo from Pi: [%s]\n" % data)
            except IOError:
                print(data + ", " + data2)
                f.close()
                pass
                break


wlanThread = WlanThread("WlanThread")
wlanThread.start()

old_dis = ""
old_ang = ""

while True:

    if connected:
        if distance != 0 and angle != 0 and len(str(distance)) != 0 and len(str(angle)) != 0:
            old_ang = angle
            old_dis = distance
            print("Angle: " + str(angle) + "     Distance: " + str(distance))

    time.sleep(0.2)
