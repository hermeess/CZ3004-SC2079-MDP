import socket
import json
import time


class Server:
    """
    Used as the server for ALGO.
    """
    def __init__(self, host, port):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.host = host 
        self.port = port

    def start(self):
        print(f"Creating server at {self.host}:{self.port}")
        self.socket.bind((self.host, self.port))
        print(f"Socket binded to port {self.port}")
        self.socket.listen()
        print("Listening for connection...")
        self.clientSocket, self.address = self.socket.accept()
        print('Got connection from', self.address)

    def send(self, d):
        data = d.encode()
        self.clientSocket.send(data)
    
    def receive(self):
        msg = self.clientSocket.recv(1024)
        data = msg.decode()
        return(data)
        
    def close(self):
        print("Closing server socket.")
        self.socket.close()

if __name__ == '__main__':
    server = Server("192.168.13.26", 3004)
    server.start()

    a = {"x":1,"y":8,"direction":-90,"obs_id":1}
    b = {"x":10,"y":8,"direction":180,"obs_id":2}
    c = {"x":1,"y":18,"direction":-90,"obs_id":3}
    d = {"x":10,"y":7,"direction":0,"obs_id":4}
    e = {"x":6,"y":12,"direction":90,"obs_id":5}
    f = {"x":13,"y":2,"direction":0,"obs_id":6}
    h = {"x":15,"y":16,"direction":180,"obs_id":7}
    i = {"x":19,"y":9,"direction":180,"obs_id":8}
    g = {"x":-2,"y":-2,"direction":"None"}

    server.send(json.dumps(a))
    time.sleep(1)
    server.send(json.dumps(b))
    time.sleep(1)
    server.send(json.dumps(c))
    time.sleep(1)
    server.send(json.dumps(d))
    time.sleep(1)
    server.send(json.dumps(e))
    time.sleep(1)
    server.send(json.dumps(f))
    time.sleep(1)
    server.send(json.dumps(h))
    time.sleep(1)
    server.send(json.dumps(i))
    time.sleep(1)
    server.send(json.dumps(g))
    time.sleep(1)
    server.send("PC;START")

   



