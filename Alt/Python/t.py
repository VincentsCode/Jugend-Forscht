from __future__ import print_function
from __future__ import division

from threading import *
import threading

print("Willkommen!\n")

class BTThread(threading.Thread):

    BTMessages = []

    def __init__(self, name):
        Thread.__init__(self)
        self.name = name

    def run(self):
        print("Bluetooth Thread gestartet \n")
        while True:
			lockMe.acquire()
			BTMessage[len(BTMessage)] = "HALLO"
			lockMe.release()

			
lockMe = threading.Lock()

bluetoothThread = BTThread("BluetoothThread")

bluetoothThread.start()

while True:

    if BTThread.BTMessage[len(BTThread.BTMessage) -1 ] != "":
		print("BTMessage: " + BTThread.BTMessage[len(BTThread.BTMessage) -1 ]
