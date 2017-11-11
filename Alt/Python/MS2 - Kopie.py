import threading
from bluetooth import *

BTMessage = "";
WlanMessage = "";

print "Willkommen!\n"

def initServer():
 server_sock=BluetoothSocket( RFCOMM )
 server_sock.bind(("",PORT_ANY))
 server_sock.listen(1)

 uuid = "00001101-0000-1000-8000-00805F9B34FB"

 advertise_service(server_sock, "Echo Server",
     service_id = uuid,
     service_classes = [ uuid, SERIAL_PORT_CLASS ],
     profiles = [ SERIAL_PORT_PROFILE ]
 )
 return server_sock

def getClientConnection(server_sock):
 print "Waiting for connection" 
 client_sock, client_info = server_sock.accept()
 print "accepted connection from ", client_info
 return client_sock

def manageConnection(socket):
 try:
   while True:
     data = socket.recv(1024)
     if len(data) == 0: break
     print "received [%s]" % data
     socket.send("Echo from Pi: [%s]\n" % data)
 except IOError:
   pass

class BTThread (threading.Thread):
    def __init__(self, name):
        threading.Thread.__init__(self)
        self.name = name
    def run(self):
        print "Bluetooth Thread gestartet \n"
        server = initServer()
        while  True:
            client=getClientConnection(server)
            manageConnection(client)

class WlanThread (threading.Thread):
    def __init__(self, name):
        threading.Thread.__init__(self)
        self.name = name
    def run(self):
        print "Wlan Thread gestartet \n"


bluetoothThread = BTThread("BluetoothThread")
wlanThread = WlanThread("WlanThread")

bluetoothThread.start()
wlanThread.start()
