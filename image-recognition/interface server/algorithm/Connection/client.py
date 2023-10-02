from re import M
import socket
import json


class Client:
    """
    Used as the client for RPI.
    """
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.socket = socket.socket()
    
    def connect(self):
        print("=================================Connection=================================")
        print(f"Attempting connection to ALGO at {self.host}:{self.port}")
        self.socket.connect((self.host, self.port))
        print("Connected to ALGO!")

    def send(self, d):
        data = d.encode()
        self.socket.send(data)

    def receive(self):
        msg = self.socket.recv(1024)
        data = msg.decode()
        return(data)

    def is_json(self, msg):
        try:
            data = json.loads(msg)
            d = data["obstacle1"]
            return data
        except Exception:
            print("exception occured")
            return False

    def close(self):
        print("Closing client socket.")
        self.socket.close()


if __name__ == '__main__':
    client = Client("192.168.13.1", 3004)
    client.connect()
    datas = client.receive()
    if client.is_json(datas):  # Receive obstacle data
        if len(datas) > 0:
            client.send_json_aknowledgement()
            print(datas)
    else:  # Receive image rec or stm data
        client.send({"NOTJSON": 1})

    datas2 = client.receive()
    if client.is_json(datas2):  # Receive obstacle data
        if len(datas2) > 0:
            client.send_json_aknowledgement()
            print(datas2)
    else:  # Receive image rec or stm data
        client.send({"NOTJSON": 1})