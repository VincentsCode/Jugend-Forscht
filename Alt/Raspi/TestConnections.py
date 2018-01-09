from __future__ import print_function
from __future__ import division

from threading import *
from bluetooth import *

import threading

import socket

print("Willkommen!\n")

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
    wlan_sock.bind(("127.0.0.1", 1337))
    wlan_sock.listen(1)
    return wlan_sock

def getWlanClientConnection(server_sock):
    print("Waiting for WlanConnection")
    client_sock, client_info = server_sock.accept()
    print("accepted WlanConnection from ", client_info)
    return client_sock


class BTThread(Thread):

    BTMessage = ""

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
                    # print("BT: received [%s]" % data)
                    lockMe.acquire()
                    BTMessage = data
                    lockMe.release()
                    client.send("Echo from Pi: [%s]\n" % BTMessage)
            except IOError:
                pass


class WlanThread(Thread):

    WlanMessage = ""

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
                    # print("Wlan: received [%s]" % data)
                    lockMe.acquire()
                    WlanMessage = data
                    lockMe.release()
                    client.send("Echo from Pi: [%s]\n" % WlanMessage)
            except IOError:
                pass

lockMe = threading.Lock()

bluetoothThread = BTThread("BluetoothThread")
wlanThread = WlanThread("WlanThread")

bluetoothThread.start()
wlanThread.start()

while True:

    if BTThread.BTMessage != "":
        print("BTMessage: " + BTThread.BTMessage)
        BTThread.BTMessage = ""

    if WlanThread.WlanMessage != "":
        print("WlanMessage: " + WlanThread.WlanMessage)
        WlanThread.WlanMessage = ""